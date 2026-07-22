import { $ } from './state.js';
import { aggregateHistory, clearHistory, percentage, readHistory } from './statistics.js';
import { initializeTheme } from './theme.js';

function renderTable(id, rows) {
  const body = $(id); body.replaceChildren();
  if (!rows.length) { body.innerHTML = '<tr><td colspan="4" class="muted">No completed quizzes yet.</td></tr>'; return; }
  rows.forEach((row) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${row.name}</td><td>${row.questions}</td><td>${row.score} / ${row.points}</td><td>${percentage(row.score, row.points)}%</td>`;
    body.append(tr);
  });
}

function render() {
  const history = readHistory();
  const points = history.reduce((sum, attempt) => sum + (attempt.points || 0), 0);
  const score = history.reduce((sum, attempt) => sum + (attempt.score || 0), 0);
  const questions = history.reduce((sum, attempt) => sum + (attempt.questions || 0), 0);
  const scores = history.map((attempt) => percentage(attempt.score || 0, attempt.points || 0));
  $('tests-taken').textContent = history.length;
  $('questions-answered').textContent = questions;
  $('average-score').textContent = percentage(score, points) + '%';
  $('best-score').textContent = (scores.length ? Math.max(...scores) : 0) + '%';
  $('history-status').textContent = history.length ? `${history.length} recorded test(s)` : 'No tests recorded';
  renderTable('subject-stats', aggregateHistory(history, 'subject'));
  renderTable('topic-stats', aggregateHistory(history, 'topic'));
}

$('clear-statistics').addEventListener('click', () => {
  if (confirm('Clear all QuizHub statistics?')) { clearHistory(); render(); }
});
initializeTheme();
render();
