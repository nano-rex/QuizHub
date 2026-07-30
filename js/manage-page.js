import { $, state } from './state.js';
import { addFiles, loadBundledBanks } from './banks.js';
import { deleteUploadedBank, loadUploadedBanks } from './upload-storage.js';
import { initializeTheme } from './theme.js';

const isUploaded = (bank) => !String(bank.source || '').includes('question-banks/');
const selectedBanks = () => [...document.querySelectorAll('#bank-file-list input:checked')].map((input) => state.banks[Number(input.value)]);

function renderList() {
  const list = $('bank-file-list'); list.replaceChildren();
  state.banks.forEach((bank, index) => {
    const row = document.createElement('div'); row.className = 'manage-file';
    const checkbox = document.createElement('input'); checkbox.type = 'checkbox'; checkbox.value = index; checkbox.addEventListener('change', updateActions);
    const details = document.createElement('span'); details.textContent = `${bank.title || bank.id || 'Question bank'} · ${bank.questions.length} question(s)`;
    const kind = document.createElement('small'); kind.textContent = isUploaded(bank) ? 'Uploaded / editable' : 'Bundled / protected';
    const view = document.createElement('button'); view.type = 'button'; view.textContent = 'View'; view.addEventListener('click', () => { location.href = `manage-view.html?bank=${index}`; });
    const edit = document.createElement('button'); edit.type = 'button'; edit.textContent = 'Edit'; edit.addEventListener('click', () => { location.href = `json-editor.html?bank=${index}`; });
    row.append(checkbox, details, kind, view, edit); list.append(row);
  });
  updateActions();
}
function updateActions() { const selected = selectedBanks(); $('export-selected').disabled = !selected.length; $('delete-selected').disabled = !selected.length; $('manage-status').textContent = `${state.banks.length} JSON file(s), ${selected.length} selected`; }
function u16(value) { return [value & 255, (value >>> 8) & 255]; }
function u32(value) { return [value & 255, (value >>> 8) & 255, (value >>> 16) & 255, (value >>> 24) & 255]; }
function crc32(bytes) { let crc = 0xffffffff; for (const byte of bytes) { crc ^= byte; for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0); } return (crc ^ 0xffffffff) >>> 0; }
function zipArchive(files) { const encoder = new TextEncoder(); const local = []; const central = []; let offset = 0; files.forEach(({ name, content }) => { const filename = encoder.encode(name); const data = encoder.encode(content); const crc = crc32(data); const header = new Uint8Array([80, 75, 3, 4, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(filename.length), 0, 0]); local.push(header, filename, data); const directory = new Uint8Array([80, 75, 1, 2, 20, 0, 20, 0, 0, 0, 0, 0, 0, 0, ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(filename.length), 0, 0, 0, 0, 0, 0, 0, 0, ...u32(offset)]); central.push(directory, filename); offset += header.length + filename.length + data.length; }); const centralSize = central.reduce((sum, part) => sum + part.length, 0); const end = new Uint8Array([80, 75, 5, 6, 0, 0, 0, 0, ...u16(files.length), ...u16(files.length), ...u32(centralSize), ...u32(offset), 0, 0]); return new Blob([...local, ...central, end], { type: 'application/zip' }); }
function exportSelected() { const files = selectedBanks().map((bank) => ({ name: `${bank.id || bank.title || 'question-bank'}.json`, content: `${JSON.stringify(bank, null, 2)}\n` })); const url = URL.createObjectURL(zipArchive(files)); const link = document.createElement('a'); link.href = url; link.download = 'quizhub-question-banks.zip'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
async function deleteSelected() { const selected = selectedBanks(); const removable = selected.filter(isUploaded); if (!removable.length) { $('manage-status').textContent = 'Bundled JSON files cannot be deleted.'; return; } if (!window.confirm(`Delete ${removable.length} uploaded JSON file(s)? This cannot be undone.`)) return; for (const bank of removable) await deleteUploadedBank(bank); await reload(); }
async function importFiles(event) { await addFiles([...event.target.files]); await reload(); event.target.value = ''; }
async function reload() { state.banks = [...await loadBundledBanks(), ...await loadUploadedBanks()]; renderList(); }
async function boot() { try { await reload(); } catch (error) { $('manage-status').textContent = `Could not load JSON files: ${error.message}`; } }
$('select-all').addEventListener('click', () => { document.querySelectorAll('#bank-file-list input').forEach((input) => { input.checked = true; }); updateActions(); });
$('clear-selection').addEventListener('click', () => { document.querySelectorAll('#bank-file-list input').forEach((input) => { input.checked = false; }); updateActions(); });
$('export-selected').addEventListener('click', exportSelected); $('delete-selected').addEventListener('click', deleteSelected); $('import-json').addEventListener('change', importFiles);
initializeTheme(); boot();
