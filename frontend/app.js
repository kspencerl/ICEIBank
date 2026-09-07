const state = { token: sessionStorage.getItem('iceibank.token') || '', agency: sessionStorage.getItem('iceibank.agency') || '4000' };
const $ = (selector) => document.querySelector(selector);
const apiUrl = () => `http://localhost:${state.agency}`;

function showMessage(target, text, success = false) {
  const element = $(target);
  element.textContent = text || '';
  element.className = `message${success ? ' success' : ''}`;
}

function explainError(error, fallback = 'Nao foi possivel concluir a operacao.') {
  if (error.status === 401) return 'Sessao ausente ou expirada. Faca login novamente.';
  if (error.status === 404) return 'Conta nao encontrada nesta agencia.';
  if (error.status === 400) return error.body?.erro || 'Operacao invalida. Confira os dados informados.';
  if (error.status === 502) return 'A agencia de destino nao respondeu. O debito pode ter sido aplicado.';
  return error.body?.erro || fallback;
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  state.token = sessionStorage.getItem('iceibank.token') || '';
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(`${apiUrl()}${path}`, { ...options, headers });
  const text = await response.text();
  let body = {};
  try { body = text ? JSON.parse(text) : {}; } catch { body = { mensagem: text }; }
  if (!response.ok) { const error = new Error('API error'); error.status = response.status; error.body = body; throw error; }
  return body;
}

function setAgency(value) {
  state.agency = value;
  sessionStorage.setItem('iceibank.agency', value);
  $('#networkLabel').textContent = `API localhost:${value}`;
  $('#agencySelect').value = value;
}

function openApp() {
  $('#loginView').classList.add('hidden');
  $('#appView').classList.remove('hidden');
  $('#signedInAs').textContent = `JWT ativo · localhost:${state.agency}`;
  setAgency(state.agency);
}

function logout() {
  state.token = '';
  sessionStorage.removeItem('iceibank.token');
  $('#appView').classList.add('hidden');
  $('#loginView').classList.remove('hidden');
  showMessage('#loginMessage', 'Sessao encerrada.', true);
}

$('#loginForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  setAgency($('#loginAgency').value);
  showMessage('#loginMessage', 'Autenticando...');
  try {
    const body = await request('/auth/login', { method: 'POST', body: JSON.stringify({ usuario: $('#username').value, senha: $('#password').value }) });
    state.token = body.token;
    sessionStorage.setItem('iceibank.token', state.token);
    showMessage('#loginMessage', 'Login realizado com sucesso.', true);
    openApp();
  } catch (error) { showMessage('#loginMessage', explainError(error, 'Usuario ou senha invalidos.')); }
});

$('#logoutButton').addEventListener('click', logout);
$('#agencySelect').addEventListener('change', (event) => { setAgency(event.target.value); showMessage('#appMessage', `Agencia de entrada alterada para localhost:${event.target.value}.`, true); });

$('#balanceForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const id = $('#balanceAccount').value;
  try {
    const body = await request(`/contas/${id}`);
    $('#balanceValue').textContent = `R$ ${Number(body.saldo).toFixed(2)}`;
    $('#accountCaption').textContent = `Conta ${body.id} · ${body.nomeAluno}`;
    showMessage('#appMessage', 'Saldo atualizado.', true);
  } catch (error) { showMessage('#appMessage', explainError(error, 'Nao foi possivel consultar o saldo.')); }
});

$('#movementForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const operation = event.submitter.dataset.operation;
  const id = $('#movementAccount').value;
  const value = Number($('#movementValue').value);
  try {
    const body = await request(`/contas/${id}/${operation}`, { method: 'POST', body: JSON.stringify({ valor: value }) });
    $('#balanceValue').textContent = `R$ ${Number(body.saldo).toFixed(2)}`;
    $('#accountCaption').textContent = `Conta ${body.id} · operacao concluida`;
    showMessage('#appMessage', `${operation === 'depositar' ? 'Deposito' : 'Saque'} realizado com sucesso.`, true);
  } catch (error) { showMessage('#appMessage', explainError(error, 'Nao foi possivel movimentar a conta.')); }
});

$('#transferForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const payload = { idOrigem: Number($('#originAccount').value), idDestino: Number($('#destinationAccount').value), valor: Number($('#transferValue').value) };
  try {
    const body = await request('/transferencias', { method: 'POST', body: JSON.stringify(payload) });
    showMessage('#appMessage', body.mensagem || 'Transferencia concluida.', true);
  } catch (error) { showMessage('#appMessage', explainError(error, 'Nao foi possivel realizar a transferencia.')); }
});

if (state.token) openApp();
setAgency(state.agency);
