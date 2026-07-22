const state = { banks: [], questions: [], quiz: [] };
const $ = (id) => document.getElementById(id);
const LANGUAGE_NAMES = { en: 'English', 'zh-Hans': '简体中文', 'zh-Hant': '繁體中文', ms: 'Bahasa Melayu' };

function text(value, language) {
  if (typeof value === 'string') return value;
  return value?.[language] || (language.startsWith('zh-') ? value?.zh : '') || value?.en || value?.zh || value?.ms || '';
}

function selectedLanguages() {
  const languages = [...document.querySelectorAll('#languages input:checked')].map((input) => input.value);
  return languages.length ? languages : ['en'];
}

function displayText(value, languages) {
  return languages.map((language) => `${LANGUAGE_NAMES[language]}: ${text(value, language)}`).join('\n');
}

function displayQuestion(question, languages, variantIndex) {
  return languages.map((language) => {
    const alternatives = question.variants?.[language] || question.variants?.en || [];
    return `${LANGUAGE_NAMES[language]}: ${alternatives[variantIndex - 1] || text(question.question, language)}`;
  }).join('\n');
}

function appendImages(parent, images, languages, fallbackAlt) {
  if (!Array.isArray(images) || !images.length) return;
  const gallery = document.createElement('div'); gallery.className = 'image-gallery';
  images.forEach((image) => {
    const source = typeof image === 'string' ? image : image?.src;
    if (!source) return;
    const element = document.createElement('img'); element.className = 'quiz-image';
    element.src = source;
    element.alt = typeof image === 'object' && image.alt ? displayText(image.alt, languages) : fallbackAlt;
    element.loading = 'lazy';
    gallery.append(element);
  });
  if (gallery.children.length) parent.append(gallery);
}

function normalizeBank(bank, source) {
  if (!bank || !Array.isArray(bank.questions)) throw new Error(`${source}: expected a questions array`);
  const questions = bank.questions.map((question, index) => {
    const isMultiStep = question.type === 'multi-step';
    if (!question.question || (isMultiStep ? !Array.isArray(question.steps) || !question.steps.length : !Array.isArray(question.answers) || !question.correctAnswer)) {
      throw new Error(`${source}: question ${index + 1} is missing required question data`);
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
  const languages = selectedLanguages();
  const variantIndex = Number($('variant').value);
  const container = $('questions'); container.replaceChildren();
  state.quiz.forEach((question, index) => {
    const fieldset = document.createElement('fieldset'); fieldset.className = 'question';
    const legend = document.createElement('legend');
    legend.textContent = `${index + 1}. ${displayQuestion(question, languages, variantIndex)}`;
    fieldset.append(legend);
    appendImages(fieldset, question.images, languages, 'Question illustration');
    if (question.type === 'multi-step' || question.answerMode === 'subjective') {
      if (question.type === 'multi-step') {
      question.steps.forEach((step, stepIndex) => {
        const wrapper = document.createElement('div'); wrapper.className = 'math-step';
        const label = document.createElement('label');
        label.textContent = `${stepIndex + 1}. ${displayText(step.prompt, languages)}`;
        const input = document.createElement('input'); input.type = 'text'; input.autocomplete = 'off';
        input.dataset.questionIndex = index; input.dataset.stepIndex = stepIndex;
        label.append(input); wrapper.append(label); fieldset.append(wrapper);
      });
      } else {
        const label = document.createElement('label'); label.className = 'math-step';
        label.textContent = 'Answer:';
        const input = document.createElement('input'); input.type = 'text'; input.autocomplete = 'off';
        input.dataset.questionIndex = index;
        label.append(input); fieldset.append(label);
      }
    } else {
      shuffled(question.answers).forEach((answer) => {
        const label = document.createElement('label'); label.className = 'answer';
        const input = document.createElement('input'); input.type = 'radio'; input.name = `question-${index}`; input.value = answer.id;
        label.append(input, document.createTextNode(`${answer.id}. ${displayText(answer.text, languages)}`));
        appendImages(label, answer.images, languages, `Answer ${answer.id} illustration`);
        fieldset.append(label);
      });
    }
    container.append(fieldset);
  });
  $('progress').textContent = `${state.quiz.length} question(s)`;
  $('score').textContent = '';
}

function startQuiz() {
  const pool = selectedPool();
  const requested = Math.max(1, Number.parseInt($('question-count').value, 10) || 1);
  if (!pool.length) return $('errors').textContent = 'Select at least one subject and topic.';
  const answerMode = $('answer-mode').value;
  state.quiz = shuffled(pool).slice(0, Math.min(requested, pool.length)).map((question) => ({
    ...question,
    answerMode: question.type === 'multi-step' ? 'subjective' :
      (answerMode === 'mixed' ? (Math.random() < 0.5 ? 'objective' : 'subjective') : answerMode)
  }));
  $('errors').textContent = requested > pool.length ? `Only ${pool.length} matching question(s) are available; loading all of them.` : '';
  $('quiz').classList.remove('hidden'); $('quiz-title').textContent = state.banks.map((bank) => bank.title).join(' · ');
  renderQuiz(); $('quiz').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function checkAnswers() {
  let score = 0; let points = 0;
  document.querySelectorAll('.question').forEach((element, index) => {
    const question = state.quiz[index];
    if (question.type === 'multi-step') {
      question.steps.forEach((step, stepIndex) => {
        const wrapper = element.querySelectorAll('.math-step')[stepIndex];
        const input = wrapper.querySelector('input');
        const correct = isStepCorrect(input.value, step);
        wrapper.classList.toggle('correct', correct); wrapper.classList.toggle('wrong', !correct);
        points++; if (correct) score++;
      });
      return;
    }
    points++;
    if (question.answerMode === 'subjective') {
      const input = element.querySelector('input[type="text"]');
      const correct = isSubjectiveCorrect(input.value, question, selectedLanguages());
      input.parentElement.classList.toggle('correct', correct); input.parentElement.classList.toggle('wrong', !correct);
      if (correct) score++;
      return;
    }
    const selected = element.querySelector('input:checked')?.value;
    element.querySelectorAll('.answer').forEach((answer) => {
      const input = answer.querySelector('input');
      answer.classList.toggle('correct', input.value === state.quiz[index].correctAnswer);
      answer.classList.toggle('wrong', input.checked && input.value !== state.quiz[index].correctAnswer);
    });
    if (selected === question.correctAnswer) score++;
  });
  const percentage = Math.round((score / points) * 100);
  $('score').textContent = `You scored ${score} out of ${points} point(s) (${percentage}%).`;
}

function isStepCorrect(value, step) {
  const accepted = step.acceptedAnswers || [step.correctAnswer];
  const normalized = value.trim().replace(/,/g, '').toLowerCase();
  if (accepted.some((answer) => String(answer).trim().replace(/,/g, '').toLowerCase() === normalized)) return true;
  if (step.tolerance !== undefined && normalized !== '') {
    const actual = Number(normalized);
    return Number.isFinite(actual) && accepted.some((answer) => Math.abs(actual - Number(answer)) <= Number(step.tolerance));
  }
  return false;
}

function isSubjectiveCorrect(value, question, languages) {
  const correctAnswer = question.answers.find((answer) => answer.id === question.correctAnswer);
  const accepted = languages.flatMap((language) => question.subjectiveAnswers?.[language] ||
    (correctAnswer ? [text(correctAnswer.text, language)] : []));
  const normalize = (answer) => String(answer).trim().replace(/\s+/g, ' ').toLowerCase();
  return accepted.some((answer) => normalize(answer) === normalize(value));
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
    const manifest = await (await fetch('question-banks/index.json')).json();
    const banks = await Promise.all((manifest.files || []).map((file) => loadBank(`question-banks/${file}`)));
    state.banks.push(...banks); rebuildQuestions();
  } catch (error) { $('errors').textContent = `Could not load bundled question banks: ${error.message}`; }
}

$('start').addEventListener('click', startQuiz);
$('submit').addEventListener('click', checkAnswers);
$('json-upload').addEventListener('change', (event) => addFiles([...event.target.files]));
$('languages').addEventListener('change', (event) => {
  if (!document.querySelector('#languages input:checked')) event.target.checked = true;
  if (state.quiz.length) renderQuiz();
});
$('variant').addEventListener('change', () => { if (state.quiz.length) renderQuiz(); });
boot();
