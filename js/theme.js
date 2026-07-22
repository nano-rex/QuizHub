const THEME_KEY = 'quizhub-theme';

export function applyTheme(theme) {
  if (!['light', 'dark', 'system'].includes(theme)) theme = 'light';
  document.documentElement.dataset.theme = theme;
  localStorage.setItem(THEME_KEY, theme);
}

export function initializeTheme() {
  const selector = document.getElementById('theme');
  const saved = localStorage.getItem(THEME_KEY) || 'light';
  applyTheme(saved);
  if (!selector) return;
  selector.value = saved;
  selector.addEventListener('change', () => applyTheme(selector.value));
}
