import { $, state } from './state.js';
import { saveUploadedBank } from './upload-storage.js';

export function normalizeBank(bank, source) {
  if (!bank || !Array.isArray(bank.questions)) throw new Error(`${source}: expected a questions array`);
  const questions = bank.questions.map((question, index) => {
    const isMultiStep = question.type === 'multi-step';
    const isReference = question.type === 'source-reference';
    if (!question.question || (isMultiStep ? !Array.isArray(question.steps) || !question.steps.length :
      (isReference ? false : !Array.isArray(question.answers) || !question.correctAnswer))) {
      throw new Error(`${source}: question ${index + 1} is missing required question data`);
    }
    return { ...question, id: question.id || `${source}-${index + 1}`, subject: question.subject || 'General', topic: question.topic || 'General', variants: question.variants || {} };
  });
  return { ...bank, title: bank.title || source, questions, source };
}

export async function loadBank(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`${url}: ${response.status} ${response.statusText}`);
  return normalizeBank(await response.json(), url);
}

export async function loadBundledBanks() {
  const root = document.body?.dataset.appRoot || '';
  const manifest = await (await fetch(`${root}question-banks/index.json`)).json();
  return Promise.all((manifest.files || []).map((file) => loadBank(`${root}question-banks/${file}`)));
}

export function selectedPool() {
  const checked = new Set([...document.querySelectorAll('#filters input:checked')].map((input) => input.value));
  return state.questions.filter((question) => checked.has(`subject:${question.subject}`) && checked.has(`topic:${question.topic}`));
}

export function updateAvailability() {
  const pool = selectedPool();
  $('question-count').max = pool.length || 1;
  $('question-count').placeholder = `1–${pool.length}`;
}

export function renderFilters() {
  const values = new Map();
  for (const question of state.questions) {
    values.set(`subject:${question.subject}`, `Subject: ${question.subject}`);
    values.set(`topic:${question.topic}`, `Topic: ${question.topic}`);
  }
  const filters = $('filters');
  filters.replaceChildren();
  if (!values.size) {
    filters.innerHTML = '<p class="muted">No valid questions are loaded.</p>';
    return;
  }
  for (const [value, label] of [...values].sort((a, b) => a[1].localeCompare(b[1]))) {
    const wrapper = document.createElement('label');
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox'; checkbox.value = value; checkbox.checked = true;
    checkbox.addEventListener('change', updateAvailability);
    wrapper.append(checkbox, document.createTextNode(label)); filters.append(wrapper);
  }
  updateAvailability();
}

export function rebuildQuestions() {
  state.questions = state.banks.flatMap((bank) => bank.questions);
  renderFilters();
  $('bank-status').textContent = `${state.banks.length} bank(s), ${state.questions.length} question(s) available`;
}

export async function addFiles(files) {
  const errors = [];
  for (const file of files) {
    try {
      const bank = normalizeBank(JSON.parse(await file.text()), file.name);
      state.banks.push(bank); await saveUploadedBank(bank);
    }
    catch (error) { errors.push(error.message); }
  }
  $('errors').textContent = errors.join('\n');
  rebuildQuestions();
}
