import { $ } from './state.js';

export const LANGUAGE_NAMES = {
  en: 'English',
  'zh-Hans': '简体中文',
  'zh-Hant': '繁體中文',
  ms: 'Bahasa Melayu'
};

export function text(value, language) {
  if (typeof value === 'string') return value;
  return value?.[language] || (language.startsWith('zh-') ? value?.zh : '') || value?.en || value?.zh || value?.ms || '';
}

export function selectedLanguages() {
  const languages = [...document.querySelectorAll('#languages input:checked')].map((input) => input.value);
  return languages.length ? languages : ['en'];
}

export function displayText(value, languages) {
  return languages.map((language) => `${LANGUAGE_NAMES[language]}: ${text(value, language)}`).join('\n');
}

export function displayQuestion(question, languages, variantIndex) {
  return languages.map((language) => {
    const alternatives = question.variants?.[language] || question.variants?.en || [];
    return `${LANGUAGE_NAMES[language]}: ${alternatives[variantIndex - 1] || text(question.question, language)}`;
  }).join('\n');
}

export function displayReferenceAnswer(question, languages) {
  if (question.solution) return `Solution:\n${displayText(question.solution, languages)}`;
  if (Array.isArray(question.sourceAnswerLogic) && question.sourceAnswerLogic.length) {
    return `Source answer logic:\n${question.sourceAnswerLogic.join('\n')}`;
  }
  return question.answerStatus || 'No static answer was available in the source extraction.';
}
