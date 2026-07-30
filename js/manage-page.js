import { $, state } from './state.js';
import { loadBundledBanks, normalizeBank } from './banks.js';
import { deleteUploadedBank, loadUploadedBanks, saveUploadedBank } from './upload-storage.js';
import { initializeTheme } from './theme.js';

let editingBank = null;
let matchIndex = 0;

const isUploaded = (bank) => !String(bank.source || '').includes('question-banks/');
const selectedBanks = () => [...document.querySelectorAll('#bank-file-list input:checked')].map((input) => state.banks[Number(input.value)]);

function escapeHtml(value) { return String(value).replace(/[&<>"']/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character])); }
function searchMatches(value, query) { if (!query) return []; const flags = 'gi'; const expression = new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), flags); const matches = []; let match; while ((match = expression.exec(value))) { matches.push({ start: match.index, end: match.index + match[0].length }); if (!match[0]) expression.lastIndex++; } return matches; }

function renderEditor() {
  if (!editingBank) return;
  const editor = $('json-editor'); const value = editor.textContent;
  const query = $('editor-find').value; const matches = searchMatches(value, query); matchIndex = matches.length ? Math.min(matchIndex, matches.length - 1) : 0;
  let output = ''; let position = 0;
  matches.forEach((match, index) => { output += escapeHtml(value.slice(position, match.start)); output += `<mark class="editor-match${index === matchIndex ? ' current' : ''}">${escapeHtml(value.slice(match.start, match.end))}</mark>`; position = match.end; });
  editor.innerHTML = output + escapeHtml(value.slice(position));
  $('editor-status').textContent = query ? `${matches.length} match(es)${matches.length ? ` · ${matchIndex + 1} of ${matches.length}` : ''}` : 'Ready to edit';
}

function renderList() {
  const list = $('bank-file-list'); list.replaceChildren();
  state.banks.forEach((bank, index) => {
    const row = document.createElement('label'); row.className = 'manage-file';
    const checkbox = document.createElement('input'); checkbox.type = 'checkbox'; checkbox.value = index; checkbox.addEventListener('change', updateActions);
    const name = document.createElement('span'); name.textContent = `${bank.title || bank.id || 'Question bank'} · ${bank.questions.length} question(s)`;
    const kind = document.createElement('small'); kind.textContent = isUploaded(bank) ? 'Uploaded / editable' : 'Bundled / protected';
    row.append(checkbox, name, kind); list.append(row);
  });
  updateActions();
}

function updateActions() {
  const selected = selectedBanks(); const one = selected.length === 1;
  $('export-selected').disabled = !selected.length; $('delete-selected').disabled = !selected.length; $('edit-selected').disabled = !one;
  $('manage-status').textContent = `${state.banks.length} JSON file(s), ${selected.length} selected`;
}

function crc32(bytes) { let crc = 0xffffffff; for (const byte of bytes) { crc ^= byte; for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0); } return (crc ^ 0xffffffff) >>> 0; }
function u16(value) { return [value & 255, (value >>> 8) & 255]; }
function u32(value) { return [value & 255, (value >>> 8) & 255, (value >>> 16) & 255, (value >>> 24) & 255]; }
function zipArchive(files) {
  const encoder = new TextEncoder(); const local = []; const central = []; let offset = 0;
  files.forEach(({ name, content }) => { const filename = encoder.encode(name); const data = encoder.encode(content); const crc = crc32(data); const header = new Uint8Array([80, 75, 3, 4, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(filename.length), 0, 0]); local.push(header, filename, data); const directory = new Uint8Array([80, 75, 1, 2, 20, 0, 20, 0, 0, 0, 0, 0, 0, 0, ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(filename.length), 0, 0, 0, 0, 0, 0, 0, 0, ...u32(offset)]); central.push(directory, filename); offset += header.length + filename.length + data.length; });
  const end = new Uint8Array([80, 75, 5, 6, 0, 0, 0, 0, ...u16(files.length), ...u16(files.length), ...u32(central.reduce((sum, part) => sum + part.length, 0)), ...u32(offset), 0, 0]); return new Blob([...local, ...central, end], { type: 'application/zip' });
}
function download(blob, name) { const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = name; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }

function exportSelected() { const files = selectedBanks().map((bank) => ({ name: `${bank.id || bank.title || 'question-bank'}.json`, content: `${JSON.stringify(bank, null, 2)}\n` })); download(zipArchive(files), 'quizhub-question-banks.zip'); $('manage-status').textContent = `Exported ${files.length} JSON file(s).`; }

async function deleteSelected() {
  const selected = selectedBanks(); const removable = selected.filter(isUploaded); if (!removable.length) { $('manage-status').textContent = 'Bundled JSON files cannot be deleted.'; return; }
  if (!window.confirm(`Delete ${removable.length} uploaded JSON file(s)? This cannot be undone.`)) return;
  for (const bank of removable) await deleteUploadedBank(bank);
  state.banks = state.banks.filter((bank) => !removable.includes(bank)); renderList(); $('manage-status').textContent = `Deleted ${removable.length} uploaded file(s).`;
}

function editSelected() { editingBank = selectedBanks()[0]; $('editor-title').textContent = `Editing: ${editingBank.title || editingBank.id}`; $('json-editor').textContent = JSON.stringify(editingBank, null, 2); $('editor-find').value = ''; $('editor-replace').value = ''; $('editor-panel').classList.remove('hidden'); renderEditor(); $('editor-panel').scrollIntoView({ behavior: 'smooth' }); }
async function saveEditor() { try { const parsed = JSON.parse($('json-editor').textContent); const source = isUploaded(editingBank) ? editingBank.source : `managed-${Date.now()}-${parsed.id || 'question-bank'}.json`; const bank = normalizeBank({ ...parsed, source }, source); await saveUploadedBank(bank); const index = state.banks.indexOf(editingBank); if (index >= 0 && isUploaded(editingBank)) state.banks[index] = bank; else state.banks.push(bank); editingBank = bank; renderList(); $('editor-status').textContent = 'Saved successfully.'; } catch (error) { $('editor-status').textContent = `Could not save JSON: ${error.message}`; } }
function replaceCurrent() { const value = $('json-editor').textContent; const matches = searchMatches(value, $('editor-find').value); if (!matches.length) return; const match = matches[matchIndex]; $('json-editor').textContent = value.slice(0, match.start) + $('editor-replace').value + value.slice(match.end); renderEditor(); }
function replaceAll() { const query = $('editor-find').value; if (!query) return; const value = $('json-editor').textContent; const matches = searchMatches(value, query); if (!matches.length) return; const expression = new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'); $('json-editor').textContent = value.replace(expression, $('editor-replace').value); matchIndex = 0; renderEditor(); }

async function boot() { try { state.banks.push(...await loadBundledBanks(), ...await loadUploadedBanks()); renderList(); } catch (error) { $('manage-status').textContent = `Could not load JSON files: ${error.message}`; } }
$('select-all').addEventListener('click', () => { document.querySelectorAll('#bank-file-list input').forEach((input) => { input.checked = true; }); updateActions(); });
$('clear-selection').addEventListener('click', () => { document.querySelectorAll('#bank-file-list input').forEach((input) => { input.checked = false; }); updateActions(); });
$('export-selected').addEventListener('click', exportSelected); $('delete-selected').addEventListener('click', deleteSelected); $('edit-selected').addEventListener('click', editSelected);
$('editor-find').addEventListener('input', () => { matchIndex = 0; renderEditor(); }); $('editor-prev').addEventListener('click', () => { const count = searchMatches($('json-editor').textContent, $('editor-find').value).length; if (count) { matchIndex = (matchIndex - 1 + count) % count; renderEditor(); } }); $('editor-next').addEventListener('click', () => { const count = searchMatches($('json-editor').textContent, $('editor-find').value).length; if (count) { matchIndex = (matchIndex + 1) % count; renderEditor(); } });
$('editor-replace-one').addEventListener('click', replaceCurrent); $('editor-replace-all').addEventListener('click', replaceAll); $('editor-save').addEventListener('click', saveEditor); $('editor-close').addEventListener('click', () => { editingBank = null; $('editor-panel').classList.add('hidden'); });
initializeTheme(); boot();
