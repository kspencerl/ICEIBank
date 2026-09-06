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