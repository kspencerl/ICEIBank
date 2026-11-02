package com.iceibank.agencia.controllers;

import com.iceibank.agencia.services.BullyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bully")
@RequiredArgsConstructor
public class BullyController {
    public record EleicaoRequest(Integer origemId, int[] vetorEnvio) {}
    public record CoordenadorRequest(Integer liderId, int[] vetorEnvio) {}
    public record LiderResponse(int idAgencia, Integer liderAtual) {}
    private final BullyService bully;

    @GetMapping("/lider")
    public LiderResponse lider() {
        return new LiderResponse(bully.obterIdAgencia(), bully.obterLiderAtual());
    }

    @PostMapping("/eleicao")
    public ResponseEntity<?> eleicao(@RequestBody EleicaoRequest req) {
        if (req.origemId() == null) return ResponseEntity.badRequest().body(Map.of("erro", "origemId obrigatório."));
        try {
            bully.receberEleicao(req.origemId(), req.vetorEnvio());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException erro) {
            return ResponseEntity.badRequest().body(Map.of("erro", erro.getMessage()));
        }
    }

    @PostMapping("/coordenador")
    public ResponseEntity<?> coordenador(@RequestBody CoordenadorRequest req) {
        if (req.liderId() == null) return ResponseEntity.badRequest().body(Map.of("erro", "liderId obrigatório."));
        try {
            bully.receberCoordenador(req.liderId(), req.vetorEnvio());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException erro) {
            return ResponseEntity.badRequest().body(Map.of("erro", erro.getMessage()));
        }
    }
}
