import { $, state } from './state.js';
import { displayText, selectedLanguages } from './languages.js';
import { loadBundledBanks } from './banks.js';
import { loadUploadedBanks } from './upload-storage.js';
import { initializeTheme } from './theme.js';

function appendText(parent, value, className = '') { const element = document.createElement('p'); element.className = className; element.textContent = value; parent.append(element); }
function render(question, index, languages) { const article = document.createElement('article'); article.className = 'bank-question'; const heading = document.createElement('h3'); heading.textContent = `${index}. ${question.subject || 'General'} · ${question.topic || 'General'}`; article.append(heading); appendText(article, displayText(question.question, languages), 'bank-prompt'); if (question.type === 'multi-step') (question.steps || []).forEach((step, stepIndex) => appendText(article, `${stepIndex + 1}. ${displayText(step.prompt, languages)}\nAnswer: ${(step.acceptedAnswers || [step.correctAnswer]).join(' / ')}`, 'bank-step')); else (question.answers || []).forEach((answer) => appendText(article, `${answer.id}. ${displayText(answer.text, languages)}${(Array.isArray(question.correctAnswer) ? question.correctAnswer : [question.correctAnswer]).includes(answer.id) ? ' ✓' : ''}`, 'bank-answer')); return article; }
async function boot() { try { state.banks.push(...await loadBundledBanks(), ...await loadUploadedBanks()); const index = Number(new URLSearchParams(location.search).get('bank')); const bank = state.banks[index]; if (!bank) throw new Error('Question bank was not found.'); $('view-title').textContent = bank.title || bank.id; bank.questions.forEach((question, questionIndex) => $('view-questions').append(render(question, questionIndex + 1, selectedLanguages()))); } catch (error) { $('view-title').textContent = `Could not load bank: ${error.message}`; } }
initializeTheme(); boot();
