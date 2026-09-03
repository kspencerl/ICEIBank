6.4 Perguntas - Parte B

Por que o relógio de Lamport usa max(contador_local, timestampRecebido) + 1 ao receber uma mensagem, em vez de simplesmente adotar o timestamp recebido diretamente?

- R: Para garantir que o relógio lógico nunca retroceda. Se adotasse um timestamp recebido menor que o local, o tempo da agência "voltaria no passado". O + 1 serve para registrar o próprio ato de recebimento como um novo evento seguinte.

Se a Agência 0 está no evento de contador 10 e recebe uma mensagem com timestamp 3 (de uma agência mais “atrasada”), qual o novo valor do contador da Agência 0? O que isso implica sobre agências que processam muitos eventos rapidamente versus agências mais lentas?

- R: O novo valor será 11 (max(10, 3) + 1 = 11). Isso implica que o relógio mede o volume de eventos, não o tempo real. Agências muito ativas terão contadores altos, e receber mensagens de agências lentas não atrasa as rápidas. Porém se a agência lenta receber mensagem da rápida, seu relógio vai dar um salto para acompanhar o fluxo de eventos que já ocorreram.