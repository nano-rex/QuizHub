import { $, state } from './state.js';
import { loadBundledBanks } from './banks.js';
import { displayText, text } from './languages.js';
import { initializeTheme } from './theme.js';

function searchable(question, language) {
  const answerText = (question.answers || []).map((answer) => text(answer.text, language)).join(' ');
  return `${text(question.question, language)} ${answerText} ${question.subject} ${question.topic}`.toLowerCase();
}

function renderFilters() {
  const values = new Map();
  state.questions.forEach((question) => {
    values.set(`subject:${question.subject}`, `Subject: ${question.subject}`);
    values.set(`topic:${question.topic}`, `Topic: ${question.topic}`);
  });
  const container = $('search-filters'); container.replaceChildren();
  [...values].sort((a, b) => a[1].localeCompare(b[1])).forEach(([value, label]) => {
    const item = document.createElement('label'); item.className = 'filter-option';
    item.innerHTML = `<input type="checkbox" value="${value}" checked><span>${label}</span>`;
    item.firstElementChild.addEventListener('change', renderResults); container.append(item);
  });
}

function renderResults() {
  const query = $('search-query').value.trim().toLowerCase();
  const language = $('search-language').value;
  const type = $('search-type').value;
  const checked = new Set([...document.querySelectorAll('#search-filters input:checked')].map((input) => input.value));
  const results = state.questions.filter((question) => (!query || searchable(question, language).includes(query)) &&
    (!type || question.type === type) && checked.has(`subject:${question.subject}`) && checked.has(`topic:${question.topic}`));
  $('search-status').textContent = `${results.length} question(s) found`;
  const container = $('search-results'); container.replaceChildren();
  if (!results.length) { container.innerHTML = '<p class="muted">No questions match those filters.</p>'; return; }
  results.forEach((question) => {
    const article = document.createElement('article'); article.className = 'search-result';
    const heading = document.createElement('h3'); heading.textContent = `${question.subject} · ${question.topic}`;
    const prompt = document.createElement('p'); prompt.textContent = displayText(question.question, [language]);
    const typeLabel = document.createElement('span'); typeLabel.className = 'result-type'; typeLabel.textContent = question.type || 'multiple-choice';
    article.append(heading, typeLabel, prompt); container.append(article);
  });
}

async function boot() {
  try {
    state.banks.push(...await loadBundledBanks());
    state.questions = state.banks.flatMap((bank) => bank.questions);
    renderFilters(); renderResults();
  } catch (error) { $('search-status').textContent = `Could not load question banks: ${error.message}`; }
}

['search-query', 'search-language', 'search-type'].forEach((id) => $(id).addEventListener('input', renderResults));
initializeTheme();
boot();
