const fs = require('node:fs');
const path = require('node:path');

const pastaLogs = path.join(__dirname, 'logs');
const tiposBully = new Set([
  'ELEICAO_INICIADA', 'ELEICAO_RECEBIDA', 'TORNOU_LIDER',
  'LIDER_ANUNCIADO', 'LIDER_NAO_RESPONDEU',
]);

const eventos = fs.readdirSync(pastaLogs)
  .filter(arquivo => /^eventos-agencia-\d+\.log$/.test(arquivo))
  .flatMap(arquivo => fs.readFileSync(path.join(pastaLogs, arquivo), 'utf8')
    .split(/\r?\n/)
    .flatMap((linha, indice) => {
      if (!linha.trim()) return [];
      try {
        return [JSON.parse(linha)];
      } catch {
        console.warn(`${arquivo}:${indice + 1}: linha inválida ignorada.`);
        return [];
      }
    }))
  .filter(evento => tiposBully.has(evento.tipo))
  .sort((a, b) => a.horaParede.localeCompare(b.horaParede));

console.log('=== Histórico de eventos de eleição ===');
eventos.forEach(evento => console.log(
  `(${evento.horaParede}) [${evento.agencia}] ${evento.tipo}`, JSON.stringify(evento.detalhes),
));

const lideres = eventos.filter(evento => evento.tipo === 'TORNOU_LIDER');
console.log('\n=== Linha do tempo de líderes ===');
lideres
  .filter((evento, indice) => indice === 0 || evento.detalhes.idAgencia !== lideres[indice - 1].detalhes.idAgencia)
  .forEach(evento => console.log(
    `A partir de ${evento.horaParede}: Agência ${evento.detalhes.idAgencia} é a líder`,
  ));

if (!eventos.length) console.log('Nenhum evento de eleição registrado.');
console.log('\nOrdenação por hora de parede; representa os registros locais, sem garantir consenso global.');
