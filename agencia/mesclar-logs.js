const fs = require('fs');
const path = require('path');
const pastaLogs = path.join(__dirname, 'logs');
const extensoesAceitas = new Set(['.log', '.jsonl']);

function normalizarEvento(evento, arquivo) {
  const timestampLamport = evento.timestampLamport ?? evento.lamportTime;
  const horaParede = evento.horaParede ?? evento.relogioParede;
  const agencia = evento.agencia ?? `agencia-${evento.agenciaId}`;
  const tipo = evento.tipo ?? evento.operacao;

  if (!Number.isInteger(timestampLamport) || !horaParede || !agencia || !tipo) {
    throw new Error(`evento inválido em ${arquivo}`);
  }

  let detalhes = evento.detalhes;
  if (typeof detalhes === 'string') {
    detalhes = detalhes;
  }

  return { agencia, tipo, timestampLamport, horaParede, detalhes };
}

function lerEventos() {
  if (!fs.existsSync(pastaLogs)) {
    throw new Error(`diretório de logs não encontrado: ${pastaLogs}`);
  }

  const arquivos = fs.readdirSync(pastaLogs)
    .filter((arquivo) => extensoesAceitas.has(path.extname(arquivo)))
    .sort();
  const eventos = [];

  for (const arquivo of arquivos) {
    const caminho = path.join(pastaLogs, arquivo);
    const conteudo = fs.readFileSync(caminho, 'utf-8');
    for (const [indice, linha] of conteudo.split(/\r?\n/).entries()) {
      if (!linha.trim()) continue;
      try {
        eventos.push(normalizarEvento(JSON.parse(linha), `${arquivo}:${indice + 1}`));
      } catch (erro) {
        console.warn(`[aviso] ${erro.message}`);
      }
    }
  }

  return eventos;
}

const eventos = lerEventos();
eventos.sort((a, b) => {
  const porLamport = a.timestampLamport - b.timestampLamport;
  return porLamport || a.horaParede.localeCompare(b.horaParede);
});

console.log('=== Linha do tempo unificada (ordenada por relógio de Lamport) ===');
for (const evento of eventos) {
  console.log(
    `[Lamport ${evento.timestampLamport}] (${evento.horaParede}) ${evento.agencia} - ${evento.tipo}`,
    JSON.stringify(evento.detalhes)
  );
}

const grupos = new Map();
for (const evento of eventos) {
  const grupo = grupos.get(evento.timestampLamport) ?? [];
  grupo.push(evento);
  grupos.set(evento.timestampLamport, grupo);
}

const empates = [...grupos.entries()]
  .filter(([, grupo]) => new Set(grupo.map((evento) => evento.agencia)).size > 1);

console.log('\n=== Empates entre agências ===');
if (empates.length === 0) {
  console.log('Nenhum empate entre agências foi encontrado. Gere eventos concorrentes e execute novamente.');
} else {
  for (const [timestamp, grupo] of empates) {
    console.log(`Lamport ${timestamp}:`);
    for (const evento of grupo) {
      console.log(`  ${evento.agencia} (${evento.horaParede}) - ${evento.tipo}`);
    }
  }
}
