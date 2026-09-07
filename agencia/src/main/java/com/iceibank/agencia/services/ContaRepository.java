package com.iceibank.agencia.services;

import com.iceibank.agencia.model.Conta;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ContaRepository {

    private final Map<Integer, Conta> contas = new ConcurrentHashMap<>();

    public Conta buscar(int id) {
        return contas.get(id);
    }

    public boolean existe(int id) {
        return contas.containsKey(id);
    }

    public void salvar(Conta conta) {
        contas.put(conta.getId(), conta);
    }

    public int quantidade() {
        return contas.size();
    }
}