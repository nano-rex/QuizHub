const state = { banks: [], questions: [], quiz: [] };
const $ = (id) => document.getElementById(id);

function text(value, language) {
  if (typeof value === 'string') return value;
  return value?.[language] || value?.en || value?.zh || value?.ms || '';
}

function normalizeBank(bank, source) {
  if (!bank || !Array.isArray(bank.questions)) throw new Error(`${source}: expected a questions array`);
  const questions = bank.questions.map((question, index) => {
    if (!question.question || !Array.isArray(question.answers) || !question.correctAnswer) {
      throw new Error(`${source}: question ${index + 1} is missing question, answers, or correctAnswer`);
    }
    return {
      ...question,
      id: question.id || `${source}-${index + 1}`,
      subject: question.subject || 'General',
      topic: question.topic || 'General',
      variants: question.variants || {}
    };
  });
  return { ...bank, title: bank.title || source, questions, source };
}

async function loadBank(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`${url}: ${response.status} ${response.statusText}`);
  return normalizeBank(await response.json(), url);
}

function rebuildQuestions() {
  state.questions = state.banks.flatMap((bank) => bank.questions);
  renderFilters();
  $('bank-status').textContent = `${state.banks.length} bank(s), ${state.questions.length} question(s) available`;
}

function renderFilters() {
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

function updateAvailability() {
  const checked = new Set([...document.querySelectorAll('#filters input:checked')].map((input) => input.value));
  const pool = state.questions.filter((question) => checked.has(`subject:${question.subject}`) && checked.has(`topic:${question.topic}`));
  $('question-count').max = pool.length || 1;
  $('question-count').placeholder = `1–${pool.length}`;
}

function shuffled(items) {
  const result = [...items];
  for (let index = result.length - 1; index > 0; index--) {
    const other = Math.floor(Math.random() * (index + 1));
    [result[index], result[other]] = [result[other], result[index]];
  }
  return result;
}

function selectedPool() {
  const checked = new Set([...document.querySelectorAll('#filters input:checked')].map((input) => input.value));
  return state.questions.filter((question) => checked.has(`subject:${question.subject}`) && checked.has(`topic:${question.topic}`));
}

function renderQuiz() {
  const language = $('language').value;
  const variantIndex = Number($('variant').value);
  const container = $('questions'); container.replaceChildren();
  state.quiz.forEach((question, index) => {
    const fieldset = document.createElement('fieldset'); fieldset.className = 'question';
    const legend = document.createElement('legend');
    const alternatives = question.variants?.[language] || question.variants?.en || [];
    legend.textContent = `${index + 1}. ${alternatives[variantIndex - 1] || text(question.question, language)}`;
    fieldset.append(legend);
    shuffled(question.answers).forEach((answer) => {
      const label = document.createElement('label'); label.className = 'answer';
      const input = document.createElement('input'); input.type = 'radio'; input.name = `question-${index}`; input.value = answer.id;
      label.append(input, document.createTextNode(`${answer.id}. ${text(answer.text, language)}`)); fieldset.append(label);
    });
    container.append(fieldset);
  });
  $('progress').textContent = `${state.quiz.length} question(s)`;
  $('score').textContent = '';
}

function startQuiz() {
  const pool = selectedPool();
  const requested = Math.max(1, Number.parseInt($('question-count').value, 10) || 1);
  if (!pool.length) return $('errors').textContent = 'Select at least one subject and topic.';
  state.quiz = shuffled(pool).slice(0, Math.min(requested, pool.length));
  $('errors').textContent = requested > pool.length ? `Only ${pool.length} matching question(s) are available; loading all of them.` : '';
  $('quiz').classList.remove('hidden'); $('quiz-title').textContent = state.banks.map((bank) => bank.title).join(' · ');
  renderQuiz(); $('quiz').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function checkAnswers() {
  let score = 0;
  document.querySelectorAll('.question').forEach((element, index) => {
    const selected = element.querySelector('input:checked')?.value;
    element.querySelectorAll('.answer').forEach((answer) => {
      const input = answer.querySelector('input');
      answer.classList.toggle('correct', input.value === state.quiz[index].correctAnswer);
      answer.classList.toggle('wrong', input.checked && input.value !== state.quiz[index].correctAnswer);
    });
    if (selected === state.quiz[index].correctAnswer) score++;
  });
  const percentage = Math.round((score / state.quiz.length) * 100);
  $('score').textContent = `You scored ${score} out of ${state.quiz.length} (${percentage}%).`;
}

async function addFiles(files) {
  const errors = [];
  for (const file of files) {
    try { state.banks.push(normalizeBank(JSON.parse(await file.text()), file.name)); }
    catch (error) { errors.push(error.message); }
  }
  $('errors').textContent = errors.join('\n'); rebuildQuestions();
}

async function boot() {
  try {
    const manifest = await (await fetch('data/index.json')).json();
    const banks = await Promise.all((manifest.files || []).map((file) => loadBank(`data/${file}`)));
    state.banks.push(...banks); rebuildQuestions();
  } catch (error) { $('errors').textContent = `Could not load bundled question banks: ${error.message}`; }
}

$('start').addEventListener('click', startQuiz);
$('submit').addEventListener('click', checkAnswers);
$('json-upload').addEventListener('change', (event) => addFiles([...event.target.files]));
$('language').addEventListener('change', () => { if (state.quiz.length) renderQuiz(); });
$('variant').addEventListener('change', () => { if (state.quiz.length) renderQuiz(); });
boot();
