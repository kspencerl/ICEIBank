6.4 Perguntas - Parte B

Por que o relógio de Lamport usa max(contador_local, timestampRecebido) + 1 ao receber uma mensagem, em vez de simplesmente adotar o timestamp recebido diretamente?

- R: Para garantir que o relógio lógico nunca retroceda. Se adotasse um timestamp recebido menor que o local, o tempo da agência "voltaria no passado". O + 1 serve para registrar o próprio ato de recebimento como um novo evento seguinte.

Se a Agência 0 está no evento de contador 10 e recebe uma mensagem com timestamp 3 (de uma agência mais “atrasada”), qual o novo valor do contador da Agência 0? O que isso implica sobre agências que processam muitos eventos rapidamente versus agências mais lentas?

- R: O novo valor será 11 (max(10, 3) + 1 = 11). Isso implica que o relógio mede o volume de eventos, não o tempo real. Agências muito ativas terão contadores altos, e receber mensagens de agências lentas não atrasa as rápidas. Porém se a agência lenta receber mensagem da rápida, seu relógio vai dar um salto para acompanhar o fluxo de eventos que já ocorreram.

8.3 Perguntas - Parte D

1. No trecho `agenciaDestino === idAgencia`, por que a transferência local não precisa da lógica de `aoEnviar()`/`aoReceber()` do relógio de Lamport, enquanto a transferência entre agências precisa?

- R: Na transferência local, a origem e o destino estão no mesmo processo e compartilham o mesmo repositório de contas e o mesmo relógio de Lamport. Não existe uma mensagem sendo enviada para outro processo, então basta registrar o débito e o crédito como eventos locais com `eventoLocal()`. Na transferência entre agências, existe comunicação entre processos por REST: a agência de origem usa `aoEnviar()` para incrementar o relógio e anexar o timestamp à mensagem, e a agência de destino usa `aoReceber()` para atualizar seu relógio com base no timestamp recebido.

2. Reproduza a falha conhecida (tarefa 5) e observe o saldo da conta de origem depois do erro. Ele foi revertido? O que isso significa em termos de consistência do sistema bancário?

- R: Não. Quando a agência de destino está indisponível, a agência de origem já realizou o débito antes de tentar o crédito remoto. Se a chamada REST falhar, o sistema registra `TRANSFERENCIA_FALHOU` e retorna `502`, mas não desfaz o débito. Isso gera uma inconsistência temporária: o saldo da origem diminui, mas o saldo do destino não aumenta, fazendo o dinheiro desaparecer até que exista uma compensação ou correção manual.

3. Pensando à frente para o Sprint 4: cite, em alto nível, duas formas possíveis de corrigir esse problema (não precisa implementar agora, só descrever a ideia).

- R: Uma possibilidade é implementar o protocolo de confirmação em duas fases (2PC): a origem e o destino primeiro reservam/preparam a operação e somente depois confirmam o débito e o crédito quando ambas as partes estiverem prontas. Outra possibilidade é usar uma Saga: cada etapa é executada com eventos persistentes e, se uma etapa falhar, uma ação compensatória estorna o débito na origem ou desfaz o crédito no destino. Em ambos os casos, é necessário tratar falhas, reenvios e idempotência para evitar duplicação ou perda de transferências.

10.3 Perguntas - Parte E

1. Para esse par de eventos empatados: eles são realmente causalmente relacionados (um influenciou o outro) ou são concorrentes (aconteceram de forma independente)? Compare também com o campo `horaParede` de cada um - a ordem por hora de parede bate com a ordem por Lamport?

- R: No resultado observado, o evento `TRANSFERENCIA_CREDITO_REMOTO` da Agência 1 e o evento `CRIAR_CONTA` da Agência 0 tiveram `timestampLamport` igual a 4. Eles são concorrentes entre si. O crédito remoto foi causado pelo débito anterior da transferência, mas a criação da conta 303 não influenciou nem foi influenciada por esse crédito. A `horaParede` mostrou o crédito remoto às 23:12:15 e a criação da conta às 23:12:27. Como os timestamps empataram, o script usou o horário registrado pela máquina apenas para organizar a exibição. Esse horário não estabelece causalidade.

2. O relógio de Lamport garante que, se A aconteceu antes de B causalmente, `timestamp(A) < timestamp(B)`. Ele não garante a volta. O que isso significa na prática quando você vê dois eventos com timestamps diferentes na linha do tempo, mas sem saber se um realmente influenciou o outro?

- R: Timestamps diferentes não provam, sozinhos, que um evento influenciou o outro. A relação de ordem de Lamport é suficiente para preservar uma ordem causal quando ela existe, mas eventos independentes também podem receber valores diferentes por causa da quantidade de eventos locais processados em cada agência. Portanto, ao ver dois timestamps diferentes, só é possível afirmar a ordem causal se houver evidência de comunicação ou dependência entre os eventos.

3. Baseado no que você observou no passo 3 da tarefa: o relógio de Lamport, sozinho, seria suficiente para um sistema que precisa distinguir com certeza “A e B são concorrentes” de “A aconteceu antes de B”? Por que isso motiva o relógio vetorial do Sprint 2?

- R: Não. O relógio de Lamport sozinho não permite distinguir com certeza todos os eventos concorrentes dos eventos causalmente relacionados. Ele registra apenas um contador inteiro por agência e não mantém a informação sobre cada processo. No experimento, eventos de agências diferentes empataram em Lamport e foram identificados como concorrentes pela ausência de comunicação entre eles, não pelo timestamp sozinho. O relógio vetorial é motivado por essa limitação porque mantém um contador por agência. Assim, é possível comparar os vetores e identificar quando um evento aconteceu antes de outro ou quando os eventos são realmente concorrentes.

11.1 Decisões de autenticação

- R: O login usa um usuário e uma senha configurados por variáveis de ambiente. Essa escolha mantém o exemplo simples e evita armazenar credenciais no código ou no repositório. O endpoint `POST /auth/login` valida as credenciais e retorna um JWT assinado com HMAC. O token possui expiração de 15 minutos por padrão, configurável por `JWT_EXPIRATION_SECONDS`.

- R: Todas as rotas de contas e transferências exigem `Authorization: Bearer <token>`. A chamada entre agências também envia um JWT no mesmo cabeçalho para o endpoint `creditar-remoto`. Assim, a comunicação interna recebe a mesma validação de assinatura e expiração, sem deixar uma rota protegida aberta para chamadas sem autenticação. O segredo, o usuário e a senha são carregados do arquivo `.env`.

11.3 Perguntas - Parte F

1. Qual a diferença entre autenticação e autorização? Sua implementação verifica só uma das duas, ou as duas? Por exemplo, um usuário autenticado consegue sacar de uma conta que não é dele, na sua implementação atual?

- R: Autenticação é confirmar a identidade de quem está fazendo a requisição. Neste projeto ela acontece quando o servidor valida as credenciais no login e depois verifica a assinatura e a expiração do JWT. Autorização é verificar se essa identidade tem permissão para executar uma ação específica. A implementação atual faz autenticação, mas ainda não faz autorização por conta. Portanto, qualquer usuário com um JWT válido consegue acessar, depositar ou sacar de qualquer conta existente, desde que conheça o identificador. Em um sistema real, seria necessário associar usuários a contas e verificar essa permissão em cada operação.

2. Por que o servidor não precisa consultar um banco de dados para validar a assinatura de um JWT a cada requisição? O que isso implica sobre escalabilidade, comparado a guardar sessões em memória no servidor?

- R: O servidor consegue validar o JWT usando a própria chave secreta, o algoritmo de assinatura e as informações de expiração presentes no token. Como a validação é local, não é necessário consultar um banco de dados ou uma sessão armazenada para confirmar a identidade. Isso facilita a escalabilidade horizontal, pois várias instâncias podem validar o mesmo token de forma independente quando compartilham a chave. Em comparação, sessões em memória exigem que o usuário volte ao mesmo servidor ou que exista uma sessão compartilhada entre as instâncias. O JWT reduz essa dependência, mas exige cuidados com expiração, revogação e proteção da chave.

3. O que aconteceria com a segurança do sistema se a chave secreta usada para assinar o JWT vazasse?

- R: Uma pessoa que obtivesse a chave poderia criar tokens com assinaturas válidas e se passar por usuários ou agências. Ela poderia acessar as rotas protegidas até que a chave fosse substituída ou os tokens expirassem. A resposta seria revogar a chave comprometida, gerar uma nova chave forte, atualizar o segredo em todas as agências e reiniciá-las. Também seria necessário invalidar os tokens antigos e investigar o uso indevido. Por isso a chave fica no `.env`, fora do Git, e em produção deve ser armazenada em um gerenciador de segredos com controle de acesso e rotação.

12.3 Perguntas - Parte G

1. Como o frontend “lembra” de reenviar o token em cada requisição depois do login? Descreva, em alto nível, o mecanismo que você implementou.

- R: Depois do login, o frontend recebe o JWT e o armazena no `sessionStorage` do navegador com a chave `iceibank.token`. A função responsável pelas chamadas à API lê o valor atual dessa chave antes de cada requisição. Quando existe um token, ela adiciona automaticamente o cabeçalho `Authorization: Bearer <token>`. O token é removido quando a pessoa usuária encerra a sessão ou fecha a aba do navegador.

2. Se o token expirar enquanto alguém está usando o frontend no meio de uma operação, o que acontece na sua implementação? A interface avisa a pessoa usuária, ou ela só vê um erro genérico?

- R: A API rejeita a requisição com HTTP 401 quando o token expira. O frontend identifica esse status e exibe a mensagem “Sessao ausente ou expirada. Faca login novamente.” no painel da interface. Portanto, a pessoa usuária recebe um aviso específico em vez de apenas um erro genérico. Ela precisa fazer login novamente para obter um novo token.

3. Esta unidade da disciplina trata de arquitetura MVC. No seu frontend, onde fica o “M” (Model), o “V” (View) e o “C” (Controller)? Eles existem de forma clara na sua implementação, ou o código ficou mais misturado do que o padrão sugere?

- R: O frontend não implementa MVC formalmente. A View está no `index.html` e no `styles.css`, que definem a estrutura e a apresentação da interface. O estado mantido em `app.js`, como o token e a agência selecionada, funciona como um Model simples. As funções de requisição e os listeners dos formulários funcionam como Controllers, pois recebem ações da interface, chamam a API e atualizam a tela. Como o frontend é pequeno e foi feito sem framework, essas responsabilidades ficam reunidas no `app.js` em vez de separadas em módulos MVC distintos. A separação MVC é mais explícita no backend, com controllers, services e models.

2.1 Funcionalidade adicional - status da agência

- R: A funcionalidade adicional escolhida foi o endpoint autenticado `GET /status`. Ele informa a identidade da agência, o valor atual do relógio de Lamport e a quantidade de contas mantidas localmente. Escolhi esse recurso porque acrescenta uma capacidade observável de monitoramento sem misturar responsabilidades de negócio aos controllers de contas e transferências. Ele também ajuda a verificar qual processo está respondendo e qual é o estado local de cada agência.

# Sprint 2

6.4 Perguntas - Parte B

1. Com 3 agências, o vetor tem 3 posições. Se o sistema crescesse para 10 agências, o que aconteceria com o tamanho de cada vetor anexado a cada mensagem? Isso é um problema? Por quê (ou por que não)?

- R: O vetor passaria a ter 10 posições, porque o relógio vetorial guarda um contador por processo. O tamanho cresce de forma linear com o número de agências, tanto em cada mensagem quanto em cada evento do log. Com 10 agências isso não é um problema: são 10 inteiros. Em sistemas com centenas ou milhares de processos, ou com processos entrando e saindo dinamicamente, o custo de rede e armazenamento cresce e passa a exigir otimizações, como enviar só as posições alteradas ou usar variações como version vectors com poda.

2. Dado V1 = [3, 1, 0] e V2 = [3, 2, 0]: qual evento aconteceu primeiro, ou eles são concorrentes?

- R: V1 aconteceu antes de V2. Comparando posição a posição: 3 <= 3, 1 <= 2 e 0 <= 0, e os vetores são diferentes (a posição 1 é menor em V1). Como V1[i] <= V2[i] para todo i, V1 → V2.

3. Dado V1 = [3, 1, 0] e V2 = [1, 3, 0]: qual evento aconteceu primeiro, ou eles são concorrentes?

- R: São concorrentes. Na posição 0, V1 é maior (3 > 1); na posição 1, V2 é maior (3 > 1). Nem V1 <= V2 nem V2 <= V1, então nenhum dos dois eventos influenciou o outro.

7.5 Perguntas - Parte C

1. No passo 4 da tarefa, o que aconteceu exatamente quando a Agência 1 voltou? Se a mensagem "sumiu" (não foi aplicada), isso foi porque a mensageria falhou, ou por outro motivo?

- R: A mensagem foi entregue. Com a Agência 1 fora do ar, a transferência respondeu 200 e a mensagem ficou retida na `fila-agencia-1` (1 mensagem, 0 consumidores). Quando a agência voltou, o consumidor recebeu a mensagem imediatamente, mas a conta 301 não existia mais, pois as contas ficam em memória e foram perdidas no reinício. O log registrou `CREDITO_REMOTO_FALHOU` com motivo "conta nao encontrada" e vetor `[5,1,0]`. A mensageria funcionou: o problema foi a falta de persistência do estado da agência. Com a funcionalidade adicional, a mensagem não foi descartada e ficou na `fila-agencia-1.dlq`.

2. Compare esse comportamento com o do Sprint 1 (chamada REST direta): o que melhorou com a mensageria, e o que continua sendo um problema em aberto?

- R: No Sprint 1, se a agência de destino estivesse fora do ar, a chamada REST falhava na hora e o débito ficava aplicado sem crédito. Agora a mensagem é durável e é entregue quando o destino volta, então a indisponibilidade temporária não perde a transferência. O problema em aberto é que "a mensagem não se perde" não significa "o sistema está correto": a origem já debitou e o destino pode não conseguir aplicar o crédito (conta inexistente), deixando o dinheiro fora das duas contas. Também não há confirmação de volta para a origem, estorno automático nem garantia de processamento único (idempotência). Isso exige persistência das contas e um protocolo de transação distribuída (2PC ou Saga, Sprint 4).

3. O consumidor de mensagens processa créditos sem passar por nenhuma verificação de token JWT. Isso é um problema de segurança? Por que sim, ou por que não?

- R: Sim. O consumidor confia em qualquer mensagem que chegue na fila. Hoje, qualquer pessoa com a `RABBITMQ_URL` (que contém usuário e senha) pode publicar na exchange `iceibank.eventos` com a routing key `agencia.1.creditar` e criar dinheiro em qualquer conta, sem passar pela API nem pelo JWT. No ambiente de desenvolvimento todas as agências usam a mesma credencial do broker, então a segurança depende apenas de proteger essa URL. Para mitigar, seria possível usar usuários do RabbitMQ com permissões separadas por agência, assinar as mensagens (por exemplo, com um JWT ou HMAC no corpo) e validar a assinatura no consumidor.

8.3 Perguntas - Parte D

1. No Sprint 1, o relógio de Lamport não permitia essa análise. O que exatamente, no relógio vetorial, torna possível essa comparação confiável?

- R: O vetor guarda quanto cada agência sabe sobre os eventos de todas as outras. Cada posição só aumenta quando aquela agência gera um evento ou quando a informação chega por uma mensagem. Se V1 <= V2 em todas as posições, tudo que era conhecido em V1 já era conhecido em V2, então existe um caminho causal de V1 para V2. Se cada vetor tem alguma posição maior que o outro, cada evento conhece algo que o outro não conhece, o que prova a concorrência. O Lamport resume tudo em um único número e perde essa informação por processo.

2. Encontre, no seu próprio teste, um par de eventos que o script classificou como concorrente. Faz sentido, olhando para o que cada evento representa?

- R: `[agencia-0] CRIAR_CONTA [1,0,0]` e `[agencia-1] CRIAR_CONTA [0,1,0]` foram classificados como concorrentes. Faz sentido: são criações de contas em agências diferentes, sem nenhuma mensagem trocada entre elas antes. Cada agência só conhecia o próprio evento. Já `[agencia-0] TRANSFERENCIA_DEBITO [2,0,0]` e `[agencia-1] TRANSFERENCIA_CREDITO_REMOTO [3,2,0]` aparecem como causais (o débito aconteceu antes), pois o crédito só existiu por causa da mensagem publicada pela Agência 0.

3. O algoritmo de comparação de vetores neste script é O(n²) no número de eventos. Isso seria um problema em um sistema real com milhões de eventos? O que se poderia fazer para tornar essa análise mais escalável?

- R: Sim. Com 1 milhão de eventos seriam cerca de 500 bilhões de comparações, cada uma com custo proporcional ao número de agências. Para escalar, é possível limitar a análise a uma janela de tempo ou a uma transação, comparar apenas eventos de interesse (por exemplo, os que acessam a mesma conta) ou processar os eventos em ordem causal mantendo apenas a "fronteira" mais recente de cada agência. Também é possível guardar só as arestas causais diretas (envio → recebimento) e consultar a relação por alcançabilidade nesse grafo, em vez de comparar todos os pares.

2.1 Funcionalidade adicional (Sprint 2) - fila de mensagens não processadas (dead-letter)

- R: Cada fila de agência (`fila-agencia-{id}`) foi configurada com uma dead-letter exchange (`iceibank.dlx`). Quando o consumidor não consegue aplicar um crédito, por exemplo porque a conta não existe mais depois de um reinício, ele registra `CREDITO_REMOTO_FALHOU` e rejeita a mensagem sem reenfileirar (`AmqpRejectAndDontRequeueException`). O RabbitMQ então move a mensagem para `fila-agencia-{id}.dlq`. Assim, a mensagem não entra em loop de reprocessamento e também não é descartada: ela fica disponível no RabbitMQ Manager para análise ou reprocessamento manual. Escolhi esse recurso porque ele trata o cenário de falha da Parte C. Evidência: `evidencias/sprint2/funcionalidade-adicional.png`.
