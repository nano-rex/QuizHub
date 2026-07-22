import { $, state } from './state.js';
import { addFiles, loadBundledBanks, rebuildQuestions, updateAvailability } from './banks.js';
import { applySettingsToForm, readSettings, saveSettings, settingsFromForm } from './settings.js';
import { initializeTheme } from './theme.js';
import { loadUploadedBanks } from './upload-storage.js';

async function boot() {
  try {
    state.banks.push(...await loadBundledBanks(), ...await loadUploadedBanks());
    rebuildQuestions();
    const settings = readSettings();
    applySettingsToForm(settings);
    if (settings.filters.length) document.querySelectorAll('#filters input').forEach((input) => { input.checked = settings.filters.includes(input.value); });
    updateAvailability();
  } catch (error) { $('errors').textContent = `Could not load question banks: ${error.message}`; }
}

$('start').addEventListener('click', () => {
  const settings = settingsFromForm();
  if (!settings.languages.length) { $('errors').textContent = 'Select at least one display language.'; return; }
  if (!settings.filters.length) { $('errors').textContent = 'Select at least one subject and topic.'; return; }
  saveSettings(settings);
  location.href = '../index.html?start=1';
});
$('json-upload').addEventListener('change', async (event) => {
  await addFiles([...event.target.files]);
  const settings = readSettings();
  applySettingsToForm(settings);
});
$('languages').addEventListener('change', (event) => { if (!document.querySelector('#languages input:checked')) event.target.checked = true; });
initializeTheme();
boot();
