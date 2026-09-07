# ICEIBank Agencia

Backend distribuído do ICEIBank, implementado em Java 25 com Spring Boot. Cada processo representa uma agência e mantém em memória as contas sob sua responsabilidade.

## Requisitos

- Java 25+
- Gradle Wrapper incluído no projeto
- Bash, Linux/macOS ou WSL/Git Bash no Windows

## Configuração

As configurações sensíveis ficam em `agencia/.env`, que não é versionado. Crie o arquivo a partir do modelo:

```bash
cd agencia
cp .env.example .env
```

Preencha os valores necessários:

```dotenv
JWT_SECRET=seu-segredo-base64
JWT_EXPIRATION_SECONDS=900
AUTH_USERNAME=admin
AUTH_PASSWORD=admin123
```

Use um segredo forte e diferente em ambientes reais. Todas as agências devem usar o mesmo `JWT_SECRET` para validar os tokens umas das outras.

## Executar as agências

Inicie cada agência em um terminal separado, a partir desta pasta:

```bash
./run-agencia.sh 0
./run-agencia.sh 1
./run-agencia.sh 2
```

As portas padrão são:

| Agência | Porta |
| --- | ---: |
| 0 | 4000 |
| 1 | 4001 |
| 2 | 4002 |

O launcher carrega o `.env` automaticamente. Também é possível informar uma porta diferente:

```bash
./run-agencia.sh 0 4010
```

Para encerrar uma instância, pressione `Ctrl+C` no terminal correspondente.

## Particionamento

Uma conta pertence a uma única agência, definida por `id da conta % 3`:

```text
id % 3 == 0 -> Agência 0
id % 3 == 1 -> Agência 1
id % 3 == 2 -> Agência 2
```

As contas são armazenadas em memória. Ao reiniciar uma agência, suas contas são perdidas e precisam ser criadas novamente.

## Autenticação

O login é público e retorna um JWT:

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "usuario": "admin",
  "senha": "admin123"
}
```

As credenciais reais são as definidas no `.env`. Envie o token nas demais rotas:

```http
Authorization: Bearer <token>
```

O token expira conforme `JWT_EXPIRATION_SECONDS`, que vale 900 segundos por padrão. Rotas protegidas retornam `401` quando o token está ausente, inválido ou expirado.

## API

Todas as rotas abaixo, exceto `/auth/login`, exigem JWT.

### Status da agência

O endpoint adicional de status permite consultar a identidade da agência, o valor atual do relógio de Lamport e a quantidade de contas mantidas localmente:

```text
GET /status
```

Exemplo de resposta:

```json
{
  "agencia": 0,
  "timestampLamport": 3,
  "contasLocais": 2
}
```

Essa rota também exige JWT.

### Contas

```text
POST /contas
GET  /contas/{id}
POST /contas/{id}/depositar
POST /contas/{id}/sacar
```

Exemplo de criação:

```bash
curl -X POST http://localhost:4000/contas \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":300,"nomeAluno":"Alice","saldoInicial":100}'
```

### Transferências

```text
POST /transferencias
POST /contas/{id}/creditar-remoto
```

Exemplo de transferência:

```json
{
  "idOrigem": 300,
  "idDestino": 301,
  "valor": 30
}
```

O backend identifica automaticamente se a transferência é local ou entre agências. A chamada interna para `creditar-remoto` também usa um JWT técnico.

Valores de depósito, saque, transferência e crédito remoto devem ser positivos e finitos. O saldo inicial pode ser omitido ou deve ser zero ou maior.

O JWT autentica o usuário da API. A associação entre um usuário e uma conta bancária não faz parte deste sprint, portanto a autorização individual por conta ainda não é aplicada.

## Relógio e logs

Cada agência mantém seu próprio relógio de Lamport e grava eventos em JSON Lines:

```text
logs/eventos-agencia-0.log
logs/eventos-agencia-1.log
logs/eventos-agencia-2.log
```

Para gerar uma linha do tempo unificada:

```bash
node mesclar-logs.js
```

O script ordena os eventos por `timestampLamport` e destaca timestamps empatados entre agências diferentes.

## Testes

Execute a suíte do projeto com:

```bash
./gradlew test
```

Os testes unitários cobrem as validações financeiras em `src/test/java`.
