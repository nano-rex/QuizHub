const HISTORY_KEY = 'quizhub-statistics';

export function readHistory() {
  try {
    const history = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
    return Array.isArray(history) ? history : [];
  } catch {
    return [];
  }
}

export function recordAttempt(attempt) {
  const history = readHistory();
  history.push({ ...attempt, id: `${Date.now()}-${Math.random().toString(36).slice(2)}`, date: new Date().toISOString() });
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
}

export function clearHistory() {
  localStorage.removeItem(HISTORY_KEY);
}

export function aggregateHistory(history, field) {
  const totals = new Map();
  history.forEach((attempt) => (attempt.breakdown || []).forEach((item) => {
    const name = item[field] || 'General';
    const current = totals.get(name) || { name, attempts: 0, questions: 0, score: 0, points: 0 };
    current.attempts++;
    current.questions += item.questions || 0;
    current.score += item.score || 0;
    current.points += item.points || 0;
    totals.set(name, current);
  }));
  return [...totals.values()].sort((a, b) => b.points - a.points || a.name.localeCompare(b.name));
}

export function percentage(score, points) {
  return points ? Math.round((score / points) * 100) : 0;
}
