const DB_NAME = 'quizhub-data';
const STORE_NAME = 'uploaded-banks';

function database() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE_NAME, { keyPath: 'key' });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveUploadedBank(bank) {
  const db = await database();
  await new Promise((resolve, reject) => {
    const request = db.transaction(STORE_NAME, 'readwrite').objectStore(STORE_NAME).put({ key: `${bank.source}:${bank.id}`, bank });
    request.onsuccess = resolve; request.onerror = () => reject(request.error);
  });
  db.close();
}

export async function loadUploadedBanks() {
  const db = await database();
  const banks = await new Promise((resolve, reject) => {
    const request = db.transaction(STORE_NAME, 'readonly').objectStore(STORE_NAME).getAll();
    request.onsuccess = () => resolve(request.result.map((entry) => entry.bank));
    request.onerror = () => reject(request.error);
  });
  db.close();
  return banks;
}
