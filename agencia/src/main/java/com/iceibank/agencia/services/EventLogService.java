package com.iceibank.agencia.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceibank.agencia.model.EventoLog;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class EventLogService {

    @Value("${agencia.id:0}")
    private int currentAgenciaId;

    private final ObjectMapper objectMapper;
    private BufferedWriter writer;

    @PostConstruct
    public void init() {
        try {
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            Path filePath = logsDir.resolve("eventos-agencia-" + currentAgenciaId + ".log");
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar o diretório e arquivo de log", e);
        }
    }

    public synchronized void registrarEvento(int timestampLamport, String tipo, Object detalhes) {
        EventoLog evento = EventoLog.criar(currentAgenciaId, timestampLamport, tipo, detalhes);
        try {
            String json = objectMapper.writeValueAsString(evento);
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Falha ao gravar evento no log da agência " + currentAgenciaId + ": " + e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar o BufferedWriter: " + e.getMessage());
        }
    }
}