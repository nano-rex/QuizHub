import { $, state } from './state.js';
import { selectedPool } from './banks.js';
import { selectedLanguages, text } from './languages.js';
import { renderQuiz } from './render.js';
import { recordAttempt } from './statistics.js';

export function shuffled(items) {
  const result = [...items];
  for (let index = result.length - 1; index > 0; index--) {
    const other = Math.floor(Math.random() * (index + 1));
    [result[index], result[other]] = [result[other], result[index]];
  }
  return result;
}

export function startQuiz() {
  const pool = selectedPool();
  const requested = Math.max(1, Number.parseInt($('question-count').value, 10) || 1);
  if (!pool.length) { $('errors').textContent = 'Select at least one subject and topic.'; return; }
  const answerMode = $('answer-mode').value;
  state.quiz = shuffled(pool).slice(0, Math.min(requested, pool.length)).map((question) => ({
    ...question,
    answerMode: question.type === 'multi-step' ? 'subjective' :
      (answerMode === 'mixed' ? (Math.random() < 0.5 ? 'objective' : 'subjective') : answerMode)
  }));
  state.attemptRecorded = false;
  $('errors').textContent = requested > pool.length ? `Only ${pool.length} matching question(s) are available; loading all of them.` : '';
  $('quiz').classList.remove('hidden'); $('quiz-title').textContent = state.banks.map((bank) => bank.title).join(' · ');
  renderQuiz(); $('quiz').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

export function createQuiz(pool, settings) {
  const requested = Math.max(1, Number.parseInt(settings.questionCount, 10) || 1);
  const answerMode = settings.answerMode || 'objective';
  state.attemptRecorded = false;
  return shuffled(pool).slice(0, Math.min(requested, pool.length)).map((question) => ({
    ...question,
    answerMode: question.type === 'multi-step' ? 'subjective' :
      (answerMode === 'mixed' ? (Math.random() < 0.5 ? 'objective' : 'subjective') : answerMode)
  }));
}

export function isStepCorrect(value, step) {
  const accepted = step.acceptedAnswers || [step.correctAnswer];
  const normalized = value.trim().replace(/,/g, '').toLowerCase();
  if (accepted.some((answer) => String(answer).trim().replace(/,/g, '').toLowerCase() === normalized)) return true;
  if (step.tolerance !== undefined && normalized !== '') {
    const actual = Number(normalized);
    return Number.isFinite(actual) && accepted.some((answer) => Math.abs(actual - Number(answer)) <= Number(step.tolerance));
  }
  return false;
}

export function isSubjectiveCorrect(value, question, languages) {
  const correctAnswer = question.answers.find((answer) => answer.id === question.correctAnswer);
  const accepted = languages.flatMap((language) => question.subjectiveAnswers?.[language] || (correctAnswer ? [text(correctAnswer.text, language)] : []));
  const normalize = (answer) => String(answer).trim().replace(/\s+/g, ' ').toLowerCase();
  return accepted.some((answer) => normalize(answer) === normalize(value));
}

export function checkAnswers() {
  let score = 0; let points = 0;
  const breakdown = new Map();
  const addBreakdown = (question, earned, possible, questionCount = 1) => {
    const key = `${question.subject || 'General'}\u0000${question.topic || 'General'}`;
    const current = breakdown.get(key) || { subject: question.subject || 'General', topic: question.topic || 'General', questions: 0, score: 0, points: 0 };
    current.questions += questionCount; current.score += earned; current.points += possible; breakdown.set(key, current);
  };
  document.querySelectorAll('.question').forEach((element, index) => {
    const question = state.quiz[index];
    if (question.type === 'source-reference') return;
    if (question.type === 'multi-step') {
      let questionScore = 0;
      question.steps.forEach((step, stepIndex) => {
        const wrapper = element.querySelectorAll('.math-step')[stepIndex];
        const correct = isStepCorrect(wrapper.querySelector('input').value, step);
        wrapper.classList.toggle('correct', correct); wrapper.classList.toggle('wrong', !correct);
        points++; if (correct) { score++; questionScore++; }
      });
      addBreakdown(question, questionScore, question.steps.length);
      return;
    }
    points++;
    if (question.answerMode === 'subjective') {
      const input = element.querySelector('input[type="text"]');
      const correct = isSubjectiveCorrect(input.value, question, selectedLanguages());
      input.parentElement.classList.toggle('correct', correct); input.parentElement.classList.toggle('wrong', !correct);
      if (correct) score++;
      addBreakdown(question, correct ? 1 : 0, 1);
      return;
    }
    const selected = element.querySelector('input:checked')?.value;
    element.querySelectorAll('.answer').forEach((answer) => {
      const input = answer.querySelector('input');
      answer.classList.toggle('correct', input.value === question.correctAnswer);
      answer.classList.toggle('wrong', input.checked && input.value !== question.correctAnswer);
    });
    if (selected === question.correctAnswer) score++;
    addBreakdown(question, selected === question.correctAnswer ? 1 : 0, 1);
  });
  if (!points) { $('score').textContent = 'Reference entries are available for study and are not graded.'; return; }
  const percentage = Math.round((score / points) * 100);
  $('score').textContent = `You scored ${score} out of ${points} point(s) (${percentage}%).`;
  if (!state.attemptRecorded) {
    recordAttempt({ score, points, questions: state.quiz.filter((question) => question.type !== 'source-reference').length, breakdown: [...breakdown.values()] });
    state.attemptRecorded = true;
  }
}
