# Exam practice

This is a static JSON-powered practice exam site. Open `index.html` in a browser, or serve this directory with any static web server.

## Question-bank JSON format

Add any number of `.json` files with this shape. Add bundled files to `data/index.json`; users can also select any number of files with **Add JSON files** in the interface.

```json
{
  "id": "my-exam",
  "title": "My exam",
  "questions": [
    {
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
