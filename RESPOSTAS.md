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