# QuizHub

QuizHub is a static JSON-powered practice site. Open `index.html` in a browser, or serve this directory with any static web server.

All question-bank JSON files are stored exclusively under [`question-banks/`](question-banks/). The former category pages are retained in the hidden `.legacy-backup/` directory for reference.

## Question-bank JSON format

Add any number of `.json` files with this shape. Add bundled files to [`question-banks/index.json`](question-banks/index.json); users can also select any number of files with **Add JSON files** in the interface.

```json
{
  "id": "my-exam",
  "title": "My exam",
  "questions": [
    {
      "type": "multiple-choice",
      "id": "q-1",
      "subject": "Networking",
      "topic": "DNS",
      "question": {"en": "...", "zh": "...", "ms": "..."},
      "variants": {
        "en": ["Alternative wording", "Another wording"],
        "zh": ["另一种问法", "另一种问法"],
        "ms": ["Soalan dengan cara lain", "Soalan dengan cara lain"]
      },
      "answers": [
        {"id": "A", "text": {"en": "...", "zh": "...", "ms": "..."}},
        {"id": "B", "text": {"en": "...", "zh": "...", "ms": "..."}}
      ],
      "correctAnswer": "A"
    }
  ]
}
```

The quiz selects questions randomly from the checked subjects and topics. If the requested count is larger than the filtered pool, all matching questions are used. Browser file uploads have no application-defined file-count limit.

At quiz start, **Answer type** controls how questions are presented: Objective shows choices, Subjective shows a text field, and Mixed randomly chooses between them for each regular question. A regular question can provide optional `subjectiveAnswers` arrays when typed answers need to differ from the correct option’s displayed text. Multi-step questions always use step-by-step text fields.

## Multi-step mathematics

Use `type: "multi-step"` for mathematics or other worked problems. Every step gets its own input and contributes one point. `acceptedAnswers` can contain equivalent answers, and `tolerance` allows numeric answers within a permitted difference.

```json
{
  "type": "multi-step",
  "question": {"en": "Solve 2x + 4 = 14.", "zh": "解方程 2x + 4 = 14。", "ms": "Selesaikan 2x + 4 = 14."},
  "steps": [
    {"prompt": {"en": "Subtract 4 from both sides:", "zh": "等式两边减去 4：", "ms": "Tolak 4 pada kedua-dua belah:"}, "acceptedAnswers": [10]},
    {"prompt": {"en": "Divide both sides by 2. x =", "zh": "等式两边除以 2。x =", "ms": "Bahagi kedua-dua belah dengan 2. x ="}, "acceptedAnswers": [5]}
  ]
}
```
