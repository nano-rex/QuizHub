import { $, state } from './state.js';
import { loadBundledBanks } from './banks.js';
import { renderQuiz } from './render.js';
import { checkAnswers, createQuiz } from './quiz.js';
import { readSettings } from './settings.js';
import { initializeTheme } from './theme.js';
import { loadUploadedBanks } from './upload-storage.js';

async function boot() {
  try {
    state.banks.push(...await loadBundledBanks(), ...await loadUploadedBanks());
    const settings = readSettings();
    const selected = new Set(settings.filters);
    const pool = state.banks.flatMap((bank) => bank.questions).filter((question) =>
      !selected.size || (selected.has(`subject:${question.subject}`) && selected.has(`topic:${question.topic}`)));
    if (new URLSearchParams(location.search).get('start') !== '1') return;
    if (!pool.length) throw new Error('No questions match the saved settings. Open Settings and choose a subject or topic.');
    state.quiz = createQuiz(pool, settings);
    $('quiz').classList.remove('hidden');
    $('quiz-title').textContent = state.banks.map((bank) => bank.title).join(' · ');
    renderQuiz();
  } catch (error) { $('errors').textContent = `Could not load quiz: ${error.message}`; }
}

$('submit').addEventListener('click', checkAnswers);
$('new-quiz').addEventListener('click', () => {
  const settings = readSettings();
  const selected = new Set(settings.filters);
  const pool = state.questions.filter((question) =>
    !selected.size || (selected.has(`subject:${question.subject}`) && selected.has(`topic:${question.topic}`)));
  if (!pool.length) {
    $('errors').textContent = 'No questions match the saved settings. Open Settings and choose a subject or topic.';
    return;
  }
  state.quiz = createQuiz(pool, settings);
  $('errors').textContent = '';
  renderQuiz();
});
initializeTheme();
boot();
