// Linha do tempo causal (Sprint 2): junta os logs das agências e usa os relógios
// vetoriais para apontar quais pares de eventos de agências diferentes são concorrentes.
// Uso: node mesclar-logs.js
const fs = require('fs');
const path = require('path');

const pastaLogs = path.join(__dirname, 'logs');

// Lê todos os eventos (JSON Lines) de logs/*.log. Eventos antigos do Sprint 1 (Lamport) são ignorados.
function lerEventos() {
  const arquivos = fs.readdirSync(pastaLogs).filter((arquivo) => arquivo.endsWith('.log'));
  const eventos = [];
  let ignorados = 0;

  for (const arquivo of arquivos) {
    const linhas = fs.readFileSync(path.join(pastaLogs, arquivo), 'utf-8').split(/\r?\n/).filter(Boolean);
    for (const linha of linhas) {
      const evento = JSON.parse(linha);
      if (Array.isArray(evento.timestampVetorial)) eventos.push(evento);
      else ignorados++;
    }
  }

  if (ignorados > 0) console.warn(`[aviso] ${ignorados} evento(s) sem timestampVetorial (Sprint 1) ignorado(s).\n`);
  return eventos;
}

// Compara dois vetores posição a posição.
// ANTES: v1 <= v2 em tudo (e diferentes) | DEPOIS: o contrário | CONCORRENTES: nenhum dos dois.
function compararVetores(v1, v2) {
  let v1MenorOuIgual = true;
  let v2MenorOuIgual = true;
  for (let i = 0; i < v1.length; i++) {
    if (v1[i] > v2[i]) v1MenorOuIgual = false;
    if (v2[i] > v1[i]) v2MenorOuIgual = false;
  }
  if (v1MenorOuIgual && v2MenorOuIgual) return 'IGUAIS';
  if (v1MenorOuIgual) return 'ANTES';
  if (v2MenorOuIgual) return 'DEPOIS';
  return 'CONCORRENTES';
}

function descrever(evento) {
  return `[${evento.agencia}] ${evento.tipo} ${JSON.stringify(evento.timestampVetorial)}`;
}

function imprimirLinhaDoTempo(eventos) {
  console.log('=== Linha do tempo (ordenada por hora de parede) ===');
  for (const evento of eventos) {
    console.log(`${descrever(evento)} ${evento.horaParede}`, JSON.stringify(evento.detalhes));
  }
}

// Classifica todos os pares de eventos de agências diferentes.
function classificarPares(eventos) {
  const concorrentes = [];
  const causais = [];
  for (let i = 0; i < eventos.length; i++) {
    for (let j = i + 1; j < eventos.length; j++) {
      const [e1, e2] = [eventos[i], eventos[j]];
      if (e1.agencia === e2.agencia) continue;
      const relacao = compararVetores(e1.timestampVetorial, e2.timestampVetorial);
      if (relacao === 'CONCORRENTES') concorrentes.push([e1, e2]);
      else causais.push([e1, e2, relacao]);
    }
  }
  return { concorrentes, causais };
}

function imprimirPares({ concorrentes, causais }) {
  console.log('\n=== Pares de eventos CONCORRENTES entre agências diferentes ===');
  if (concorrentes.length === 0) {
    console.log('(nenhum par concorrente encontrado - gere mais eventos em paralelo e rode de novo)');
  }
  for (const [e1, e2] of concorrentes) console.log(`${descrever(e1)}  ||  ${descrever(e2)}`);

  console.log('\n=== Pares CAUSALMENTE relacionados entre agências diferentes ===');
  if (causais.length === 0) console.log('(nenhum)');
  for (const [e1, e2, relacao] of causais) {
    const [antes, depois] = relacao === 'DEPOIS' ? [e2, e1] : [e1, e2];
    console.log(`${descrever(antes)}  ->  ${descrever(depois)}`);
  }

  console.log(`\nResumo: ${concorrentes.length} par(es) concorrente(s), ${causais.length} par(es) causal(is).`);
}

const eventos = lerEventos().sort((a, b) => a.horaParede.localeCompare(b.horaParede));
imprimirLinhaDoTempo(eventos);
imprimirPares(classificarPares(eventos));
