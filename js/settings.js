const SETTINGS_KEY = 'quizhub-settings';

export const DEFAULT_SETTINGS = {
  languages: ['en'],
  variant: '0',
  answerMode: 'objective',
  questionCount: 10,
  filters: []
};

export function readSettings() {
  try {
    return { ...DEFAULT_SETTINGS, ...(JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}')) };
  } catch {
    return { ...DEFAULT_SETTINGS };
  }
}

export function saveSettings(settings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify({ ...DEFAULT_SETTINGS, ...settings }));
}

export function settingsFromForm() {
  return {
    languages: [...document.querySelectorAll('#languages input:checked')].map((input) => input.value),
    variant: document.getElementById('variant').value,
    answerMode: document.getElementById('answer-mode').value,
    questionCount: Math.max(1, Number.parseInt(document.getElementById('question-count').value, 10) || 1),
    filters: [...document.querySelectorAll('#filters input:checked')].map((input) => input.value)
  };
}

export function applySettingsToForm(settings) {
  settings.languages.forEach((language) => {
    const input = document.querySelector(`#languages input[value="${language}"]`);
    if (input) input.checked = true;
  });
  document.getElementById('variant').value = settings.variant;
  document.getElementById('answer-mode').value = settings.answerMode;
  document.getElementById('question-count').value = settings.questionCount;
}
