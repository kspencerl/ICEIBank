package com.iceibank.agencia.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/** Eleição Bully com heartbeat e failover automático. */
@Service
public class BullyService {
    private final Duration timeout;
    private final long intervaloHeartbeat;
    private final int idAgencia;
    private final String[] urls;
    private final RelogioVetorial relogio;
    private final EventLogService registro;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
    private boolean eleicaoEmAndamento;
    private boolean eleicaoSolicitadaDuranteExecucao;
    private volatile Integer liderAtual;

    @Autowired
    public BullyService(@Value("${agencia.id}") int idAgencia,
            @Value("${bully.urls}") String urls,
            @Value("${bully.timeout-ms}") long timeoutMs,
            @Value("${bully.heartbeat-ms}") long intervaloHeartbeat,
            RelogioVetorial relogio, EventLogService registro, ObjectMapper mapper) {
        this(idAgencia, urls, timeoutMs, intervaloHeartbeat, relogio, registro, mapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build());
    }

    BullyService(int idAgencia, String urls, long timeoutMs, long intervaloHeartbeat, RelogioVetorial relogio,
            EventLogService registro, ObjectMapper mapper, HttpClient http) {
        this.urls = Arrays.stream(urls.split(",")).map(String::trim).toArray(String[]::new);
        if (this.urls.length != relogio.valorAtual().length || idAgencia < 0 || idAgencia >= this.urls.length) {
            throw new IllegalArgumentException("BULLY_URLS deve listar uma URL por agência, em ordem de ID.");
        }
        if (Arrays.stream(this.urls).map(URI::create).anyMatch(uri ->
                uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())))) {
            throw new IllegalArgumentException("URL de agência inválida na configuração bully.urls.");
        }
        if (timeoutMs <= 0 || intervaloHeartbeat <= 0) {
            throw new IllegalArgumentException("Timeout e intervalo do heartbeat devem ser positivos.");
        }
        this.timeout = Duration.ofMillis(timeoutMs);
        this.intervaloHeartbeat = intervaloHeartbeat;
        this.idAgencia = idAgencia;
        this.relogio = relogio;
        this.registro = registro;
        this.mapper = mapper;
        this.http = http;
    }

    // O servidor já está aceitando conexões quando esta eleição é iniciada.
    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        iniciarEleicao();
        heartbeat.scheduleWithFixedDelay(this::verificarLider,
                intervaloHeartbeat, intervaloHeartbeat, TimeUnit.MILLISECONDS);
    }

    public void verificarLider() {
        Integer lider = liderAtual;
        if (lider == null) {
            iniciarEleicao();
            return;
        }
        if (lider == idAgencia || liderResponde(lider)) return;

        synchronized (this) {
            if (!Objects.equals(liderAtual, lider)) return;
            registro.registrarEvento(relogio.eventoLocal(), "LIDER_NAO_RESPONDEU",
                    Map.of("liderAtual", lider, "agencia", idAgencia));
            liderAtual = null;
            iniciarEleicao();
        }
    }

    private boolean liderResponde(int lider) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(urls[lider] + "/bully/lider"))
                .timeout(timeout).GET().build();
        return respondeu(req);
    }

    public synchronized CompletableFuture<Void> iniciarEleicao() {
        if (eleicaoEmAndamento) {
            eleicaoSolicitadaDuranteExecucao = true;
            return CompletableFuture.completedFuture(null);
        }
        eleicaoEmAndamento = true;
        return CompletableFuture.runAsync(this::executarEleicao, executor);
    }

    private void executarEleicao() {
        try {
            registro.registrarEvento(relogio.eventoLocal(), "ELEICAO_INICIADA", Map.of("idAgencia", idAgencia));
            boolean alguemRespondeu = IntStream.range(idAgencia + 1, urls.length)
                    .mapToObj(this::enviarEleicao)
                    .reduce(false, Boolean::logicalOr);
            if (!alguemRespondeu) tornarMeLider();
        } finally {
            finalizarEleicao();
        }
    }

    private boolean enviarEleicao(int destino) {
        return enviar(destino, "/bully/eleicao",
                Map.of("origemId", idAgencia, "vetorEnvio", relogio.aoEnviar()));
    }

    private synchronized void finalizarEleicao() {
        eleicaoEmAndamento = false;
        if (eleicaoSolicitadaDuranteExecucao && !executor.isShutdown()) {
            eleicaoSolicitadaDuranteExecucao = false;
            iniciarEleicao();
        }
    }

    private void tornarMeLider() {
        liderAtual = idAgencia;
        registro.registrarEvento(relogio.eventoLocal(), "TORNOU_LIDER", Map.of("idAgencia", idAgencia));
        IntStream.range(0, urls.length)
                .filter(id -> id != idAgencia)
                .forEach(id -> enviar(id, "/bully/coordenador",
                        Map.of("liderId", idAgencia, "vetorEnvio", relogio.aoEnviar())));
    }

    public void receberEleicao(int origemId, int[] vetorRecebido) {
        validarId(origemId);
        validarVetor(vetorRecebido);
        if (origemId >= idAgencia) throw new IllegalArgumentException("Eleição deve vir de um ID menor.");
        registro.registrarEvento(relogio.aoReceber(vetorRecebido), "ELEICAO_RECEBIDA",
                Map.of("de", origemId, "minhaId", idAgencia));
        iniciarEleicao();
    }

    public synchronized void receberCoordenador(int liderId, int[] vetorRecebido) {
        validarId(liderId);
        validarVetor(vetorRecebido);
        registro.registrarEvento(relogio.aoReceber(vetorRecebido), "LIDER_ANUNCIADO",
                Map.of("liderId", liderId, "agencia", idAgencia));
        if (liderId < idAgencia) {
            // Um anúncio atrasado de ID menor não pode desbancar uma agência superior ativa.
            iniciarEleicao();
            return;
        }
        liderAtual = liderId;
    }

    private boolean enviar(int destino, String rota, Object corpo) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(urls[destino] + rota))
                    .timeout(timeout).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo))).build();
            return respondeu(req);
        } catch (IOException erro) {
            return false;
        }
    }

    private boolean respondeu(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException erro) {
            return false; // Sem resposta dentro do timeout: segue para a próxima agência.
        }
    }

    private void validarId(int id) {
        if (id < 0 || id >= urls.length) throw new IllegalArgumentException("ID de agência inválido.");
    }

    private void validarVetor(int[] vetor) {
        if (vetor == null || vetor.length != urls.length || Arrays.stream(vetor).anyMatch(v -> v < 0)) {
            throw new IllegalArgumentException("Vetor deve conter um contador não negativo por agência.");
        }
    }

    public Integer obterLiderAtual() { return liderAtual; }
    public int obterIdAgencia() { return idAgencia; }
    @PreDestroy public void parar() {
        heartbeat.shutdownNow();
        executor.shutdownNow();
    }
}
