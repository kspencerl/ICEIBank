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
RABBITMQ_URL=amqps://usuario:senha@host.cloudamqp.com/vhost
```

`RABBITMQ_URL` é a URL AMQP da sua instância CloudAMQP (painel da instância → campo **AMQP URL**). Sem ela a agência não inicia.

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

As rotas bancárias abaixo exigem JWT. `/auth/login` e as três rotas Bully descritas na Parte A são públicas.

### Status da agência

O endpoint adicional de status permite consultar a identidade da agência, o valor atual do relógio vetorial e a quantidade de contas mantidas localmente:

```text
GET /status
```

Exemplo de resposta:

```json
{
  "agencia": 0,
  "timestampVetorial": [3, 0, 1],
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
```

Exemplo de transferência:

```json
{
  "idOrigem": 300,
  "idDestino": 301,
  "valor": 30
}
```

O backend identifica automaticamente se a transferência é local ou entre agências. Entre agências, a origem debita e **publica uma mensagem no RabbitMQ**; a agência de destino consome e aplica o crédito de forma assíncrona. Por isso, `200` significa "mensagem publicada", não "crédito já aplicado". Se o broker estiver inacessível, o débito é desfeito e a API responde `503`.

Valores de depósito, saque, transferência e crédito remoto devem ser positivos e finitos. O saldo inicial pode ser omitido ou deve ser zero ou maior.

O JWT autentica o usuário da API. A associação entre um usuário e uma conta bancária não faz parte deste sprint, portanto a autorização individual por conta ainda não é aplicada.

## Mensageria (RabbitMQ)

As agências se comunicam por publish/subscribe (código em `src/main/java/.../mensageria`):

```text
exchange iceibank.eventos (topic)
  ├── agencia.0.creditar -> fila-agencia-0
  ├── agencia.1.creditar -> fila-agencia-1
  └── agencia.2.creditar -> fila-agencia-2
```

Exchange, filas e mensagens são duráveis: uma transferência para uma agência fora do ar fica retida na fila até ela voltar.

**Dead-letter queue (funcionalidade adicional):** se o crédito não puder ser aplicado (ex.: a agência reiniciou e a conta em memória não existe mais), a mensagem é rejeitada e vai para `fila-agencia-{id}.dlq` em vez de ser descartada. Ela pode ser inspecionada no RabbitMQ Manager.

> Se as filas já existirem no broker sem a configuração de dead-letter, o RabbitMQ recusa a declaração (`PRECONDITION_FAILED`). Apague as filas `fila-agencia-*` antigas no RabbitMQ Manager e suba as agências de novo.

## Relógio vetorial e logs

Cada agência mantém um relógio vetorial (`[a0, a1, a2]`) e grava eventos em JSON Lines:

```text
logs/eventos-agencia-0.log
logs/eventos-agencia-1.log
logs/eventos-agencia-2.log
```

Para gerar a linha do tempo causal:

```bash
node mesclar-logs.js
```

O script ordena os eventos por hora de parede e compara os vetores para listar os pares de eventos de agências diferentes que são **concorrentes** e os que são **causalmente relacionados**. Eventos antigos do Sprint 1 (sem vetor) são ignorados.

## Testes

Execute a suíte do projeto com:

```bash
./gradlew test
```

Os testes unitários cobrem as validações financeiras e as três regras do relógio vetorial em `src/test/java`.

## Sprint 3 — Parte A: eleição inicial

Cada agência inicia uma eleição quando o servidor está pronto. O maior ID ativo vence. O heartbeat é descrito na Parte B abaixo.

As URLs estão em `src/main/resources/application.yaml`, na propriedade `bully.urls`, em ordem de ID 0, 1 e 2. O padrão usa portas 4000, 4001 e 4002. Para outras portas, configure `BULLY_URLS` no `.env` e informe a porta ao launcher.

Execute um comando em cada terminal, na pasta `agencia`:

```bash
./run-agencia.sh 0
./run-agencia.sh 1
./run-agencia.sh 2
```

Depois de as três iniciarem, consulte no Postman (GET, No Auth) ou no terminal:

```bash
curl http://localhost:4000/bully/lider
curl http://localhost:4001/bully/lider
curl http://localhost:4002/bully/lider
```

Cada resposta informa `idAgencia` e `liderAtual`. As três devem indicar líder 2.

As rotas `POST /bully/eleicao`, `POST /bully/coordenador` e `GET /bully/lider` são públicas conforme o roteiro. Operações bancárias continuam exigindo JWT. Os eventos da eleição são gravados nos logs existentes e transportam o relógio vetorial.

Testes: `./gradlew test`. Evidências e resultados da execução: [evidencias/sprint3/README.md](../evidencias/sprint3/README.md). Respostas conceituais: [RESPOSTAS.md](../RESPOSTAS.md).

## Sprint 3 — Parte B: heartbeat e failover

Após a eleição inicial, cada agência consulta a líder em `GET /bully/lider` a cada 3 segundos. Se ela não responder com HTTP 200 dentro do timeout, registra `LIDER_NAO_RESPONDEU`, esquece a líder e inicia outra eleição. A própria líder não consulta a si mesma. Ao voltar, uma agência inicia sua eleição normalmente e pode reassumir a liderança se tiver o maior ID.

Configuração em `src/main/resources/application.yaml`: `bully.heartbeat-ms: 3000` e `bully.timeout-ms: 1000`. O intervalo é contado após cada verificação terminar; a detecção inclui a espera do timeout e a eleição leva tempo adicional.

Para testar, suba as três agências com os comandos anteriores e confirme líder 2. Encerre a agência 2 com `Ctrl+C`, aguarde a reeleição e consulte `/bully/lider` nas portas 4000 e 4001: ambas devem indicar líder 1. Suba a agência 2 novamente e confirme que as três voltam à líder 2.

Capture a queda, os logs da reeleição e as duas respostas com data visível (`date -Is`) em `evidencias/sprint3/failover.png`. Consulte também o [roteiro da evidência](../evidencias/sprint3/README.md).
