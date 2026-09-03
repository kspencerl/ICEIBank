# ICEIBank - Sprint 1 (Agências)

Este projeto implementa uma simulação de sistema bancário distribuído com Relógio de Lamport, construído em Java 25 com Spring Boot.

## Requisitos
* Java 25+
* Linux/Mac (Bash) ou Windows (PowerShell)

## Como executar as Agências

O sistema utiliza particionamento de contas. É necessário subir três instâncias (Agências 0, 1 e 2) simultaneamente, cada uma em um terminal separado.

### 🐧 Linux / macOS (Bash)
Abra três abas no terminal e execute:

**Terminal 1 (Agência 0):**
```bash
AGENCIA_ID=0 SERVER_PORT=4000 ./gradlew bootRun
```

**Terminal 2 (Agência 1):**
```bash
AGENCIA_ID=1 SERVER_PORT=4001 ./gradlew bootRun
```

**Terminal 3 (Agência 2):**
```bash
AGENCIA_ID=2 SERVER_PORT=4002 ./gradlew bootRun
```

### 🪟 Windows (PowerShell)
Se estiver usando o PowerShell no Windows, a sintaxe das variáveis é diferente:

**Terminal 1:** `$env:AGENCIA_ID=0; $env:SERVER_PORT=4000; ./gradlew bootRun`

**Terminal 2:** `$env:AGENCIA_ID=1; $env:SERVER_PORT=4001; ./gradlew bootRun`

**Terminal 3:** `$env:AGENCIA_ID=2; $env:SERVER_PORT=4002; ./gradlew bootRun`


## Como Testar a API

Em um quarto terminal (ou utilizando o Postman/Insomnia), envie requisições para a porta `4000`.

**1. Criar a conta 0 (pertence à Agência 0):**
```bash
curl -X POST http://localhost:4000/contas \
     -H "Content-Type: application/json" \
     -d '{"id":0, "nomeAluno":"Ana", "saldoInicial":100}'
```

**2. Consultar o saldo:**
```bash
curl -X GET http://localhost:4000/contas/0
```

**3. Depositar dinheiro:**
```bash
curl -X POST http://localhost:4000/contas/0/depositar \
     -H "Content-Type: application/json" \
     -d '{"valor":25}'
```

**4. Sacar dinheiro:**
```bash
curl -X POST http://localhost:4000/contas/0/sacar \
     -H "Content-Type: application/json" \
     -d '{"valor":15}'
```

*Após executar as operações, observe que um arquivo `logs/eventos-agencia-0.log` será criado na raiz do projeto contendo os registros JSON com a evolução do Relógio de Lamport.*