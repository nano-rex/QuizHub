# QuizHub application structure

QuizHub is a static, browser-native application. It does not require a build server or backend for the bundled question banks.

```text
index.html              Quiz screen and application entry page
pages/                  Secondary HTML pages
  settings.html         Quiz configuration and JSON-bank upload
  search.html           Question-library search
  statistics.html       Persistent performance summaries
css/                    Stylesheets
  styles.css            CSS entry point and imports
  base.css              Global and layout rules
  components.css        Reusable interface components
  themes.css            Color tokens and theme variants
js/                     ES modules
question-banks/         Bundled JSON data and reference extractions
images/                 Question and answer media
.legacy-backup/         Historical HTML files; not part of the application
```

## Naming conventions

- Use lowercase kebab-case for HTML, JavaScript, CSS, JSON, and directory names.
- Name files after their responsibility: `search-page.js`, `settings-page.js`, `upload-storage.js`.
- Keep shared state and behavior in reusable modules; page entry modules should primarily wire events and boot data.
- Use `question-banks/index.json` as the only bundled-bank manifest.
- Use stable lowercase IDs for banks and questions. Do not rename an existing ID casually because uploaded or external JSON may refer to it.
- Use `zh-Hans` and `zh-Hant` explicitly; do not add the deprecated generic `zh` field.

The `a+`, `network+`, and `security+` directory names under `.legacy-backup/` are historical source paths and are intentionally exempt from the naming convention.

## Page and asset rules

Pages under `pages/` must reference shared assets with `../` paths. The shared bank loader reads the page's `data-app-root` attribute so both root and nested pages can load the same manifest. New themes should add design-token variables in `css/themes.css` rather than hard-coding colors in component rules.
