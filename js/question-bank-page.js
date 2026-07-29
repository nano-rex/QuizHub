import { $, state } from './state.js';
import { displayText, LANGUAGE_NAMES, text } from './languages.js';
import { loadBundledBanks } from './banks.js';
import { loadUploadedBanks } from './upload-storage.js';
import { initializeTheme } from './theme.js';

function appendImages(parent, images, languages, alt) {
  if (!Array.isArray(images) || !images.length) return;
  const gallery = document.createElement('div'); gallery.className = 'image-gallery';
  images.forEach((image) => {
    const source = typeof image === 'string' ? image : image?.src;
    if (!source) return;
    const element = document.createElement('img'); element.className = 'quiz-image'; element.src = source;
    element.alt = typeof image === 'object' && image.alt ? displayText(image.alt, languages) : alt;
    element.loading = 'lazy'; gallery.append(element);
  });
  if (gallery.children.length) parent.append(gallery);
}

function languages() {
  const selected = [...document.querySelectorAll('#bank-languages input:checked')].map((input) => input.value);
  return selected.length ? selected : ['en'];
}

function answerText(answer, selected) {
  return `${answer.id}. ${displayText(answer.text, selected)}`;
}

function renderQuestion(question, number, selected, bankTitle) {
  const article = document.createElement('article'); article.className = 'bank-question';
  const heading = document.createElement('h3'); heading.textContent = `${number}. ${bankTitle ? `${bankTitle} · ` : ''}${question.subject || 'General'} · ${question.topic || 'General'}`;
  const type = document.createElement('span'); type.className = 'result-type'; type.textContent = question.type || 'multiple-choice';
  heading.append(' ', type); article.append(heading);
  const prompt = document.createElement('p'); prompt.className = 'bank-prompt'; prompt.textContent = displayText(question.question, selected); article.append(prompt);
  appendImages(article, question.images, selected, 'Question illustration');
  if (question.type === 'multi-step') {
    const list = document.createElement('ol'); list.className = 'bank-steps';
    (question.steps || []).forEach((step) => {
      const item = document.createElement('li'); item.textContent = `${displayText(step.prompt, selected)} Answer: ${(step.acceptedAnswers || [step.correctAnswer]).join(' / ')}`;
      appendImages(item, step.images, selected, 'Step illustration'); list.append(item);
    });
    article.append(list);
  } else if (question.type === 'source-reference') {
    const reference = document.createElement('p'); reference.className = 'reference-answer';
    reference.textContent = question.solution ? `Solution: ${displayText(question.solution, selected)}` : (question.answerStatus || 'Reference entry'); article.append(reference);
  } else {
    const list = document.createElement('div'); list.className = 'bank-answers';
    const correct = Array.isArray(question.correctAnswer) ? question.correctAnswer : [question.correctAnswer];
    (question.answers || []).forEach((answer) => {
      const item = document.createElement('div'); item.className = `bank-answer${correct.includes(answer.id) ? ' correct' : ''}`;
      item.textContent = `${answerText(answer, selected)}${correct.includes(answer.id) ? ' ✓' : ''}`;
      appendImages(item, answer.images, selected, `Answer ${answer.id} illustration`); list.append(item);
    });
    article.append(list);
  }
  return article;
}

function render() {
  const bankIndex = Number($('bank-select').value); const query = $('bank-search').value.trim().toLowerCase();
  const selected = languages(); const container = $('bank-questions'); container.replaceChildren();
  const banks = bankIndex === -1 ? state.banks : (state.banks[bankIndex] ? [state.banks[bankIndex]] : []);
  if (!banks.length) { $('bank-view-status').textContent = 'No JSON question banks are available.'; return; }
  const results = banks.flatMap((bank) => bank.questions.map((question) => ({ bank, question }))).filter(({ question }) => !query || questionToSearchText(question).includes(query));
  $('bank-view-status').textContent = `${results.length} question(s) found${bankIndex === -1 ? ' across all banks' : ` in ${banks[0].title}`}`;
  results.forEach(({ bank, question }, index) => container.append(renderQuestion(question, index + 1, selected, bankIndex === -1 ? bank.title : '')));
}

function questionToSearchText(question) { return JSON.stringify(question).toLowerCase(); }

async function boot() {
  try {
    state.banks.push(...await loadBundledBanks(), ...await loadUploadedBanks());
    const select = $('bank-select'); select.replaceChildren();
    const all = document.createElement('option'); all.value = '-1'; all.textContent = `All question banks (${state.banks.reduce((sum, bank) => sum + bank.questions.length, 0)})`; select.append(all);
    state.banks.forEach((bank, index) => { const option = document.createElement('option'); option.value = index; option.textContent = `${bank.title} (${bank.questions.length})`; select.append(option); });
    render();
  } catch (error) { $('bank-view-status').textContent = `Could not load question banks: ${error.message}`; }
}

$('bank-select').addEventListener('change', render);
$('bank-search').addEventListener('input', render);
document.querySelectorAll('#bank-languages input').forEach((input) => input.addEventListener('change', render));
initializeTheme();
boot();
