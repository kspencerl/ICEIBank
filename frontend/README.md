# ICEIBank Frontend

Cliente web do ICEIBank, implementado com HTML, CSS e JavaScript puro, sem framework ou etapa de build.

## Requisitos

- Python 3 para servir os arquivos localmente
- As agências do backend configuradas e disponíveis nas portas `4000`, `4001` e `4002`
- Credenciais configuradas em `agencia/.env`

## Executar

A partir da raiz do repositório, inicie as agências em terminais separados:

```bash
cd agencia
./run-agencia.sh 0
./run-agencia.sh 1
./run-agencia.sh 2
```

Em outro terminal, sirva os arquivos do frontend:

```bash
cd frontend
python3 -m http.server 5173
```

Acesse no navegador:

```text
http://localhost:5173
```

O frontend deve ser servido por HTTP. Não abra `index.html` diretamente com `file://`, pois o navegador pode bloquear as chamadas para a API.

## Funcionalidades

A interface permite:

- autenticar no endpoint `/auth/login`;
- escolher a agência de entrada entre as portas `4000`, `4001` e `4002`;
- consultar o saldo de uma conta;
- depositar e sacar;
- transferir entre contas da mesma agência ou de agências diferentes.

A distinção entre transferência local e entre agências é feita pelo backend. O frontend apenas exibe a mensagem retornada pela API.

## Autenticação

Após o login, o JWT retornado pela API é armazenado em `sessionStorage` com a chave `iceibank.token`. As requisições protegidas enviam automaticamente:

```http
Authorization: Bearer <token>
```

A sessão é removida ao usar o botão `Sair` ou ao fechar a aba do navegador. Se o token estiver ausente ou expirado, a interface informa que é necessário fazer login novamente.

## Agência de entrada

A agência selecionada define a URL usada pelo frontend para as chamadas:

| Agência | URL |
| --- | --- |
| 0 | `http://localhost:4000` |
| 1 | `http://localhost:4001` |
| 2 | `http://localhost:4002` |

A conta consultada ou movimentada precisa pertencer à agência escolhida, conforme a regra `id da conta % 3` do backend.

## Tratamento de erros

As respostas de erro são exibidas na própria interface:

- `400` para dados inválidos ou saldo insuficiente;
- `401` para sessão ausente, token inválido ou token expirado;
- `404` para conta inexistente na agência selecionada;
- `502` quando a agência de destino não responde.

## Estrutura

```text
frontend/
├── index.html   # estrutura da interface
├── styles.css   # estilos e layout
└── app.js       # estado, autenticação e chamadas à API
```
