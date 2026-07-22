import { $, state } from './state.js';
import { addFiles, loadBank, rebuildQuestions } from './banks.js';
import { renderQuiz } from './render.js';
import { startQuiz, checkAnswers } from './quiz.js';

async function boot() {
  try {
    const manifest = await (await fetch('question-banks/index.json')).json();
    const banks = await Promise.all((manifest.files || []).map((file) => loadBank(`question-banks/${file}`)));
    state.banks.push(...banks); rebuildQuestions();
  } catch (error) { $('errors').textContent = `Could not load bundled question banks: ${error.message}`; }
}

function bindEvents() {
  $('start').addEventListener('click', startQuiz);
  $('submit').addEventListener('click', checkAnswers);
  $('json-upload').addEventListener('change', (event) => addFiles([...event.target.files]));
  $('languages').addEventListener('change', (event) => {
    if (!document.querySelector('#languages input:checked')) event.target.checked = true;
    if (state.quiz.length) renderQuiz();
  });
  $('variant').addEventListener('change', () => { if (state.quiz.length) renderQuiz(); });
}

bindEvents();
boot();
