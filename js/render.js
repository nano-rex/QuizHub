import { $, state } from './state.js';
import { displayQuestion, displayReferenceAnswer, displayText, selectedLanguages, text } from './languages.js';

function appendImages(parent, images, languages, fallbackAlt) {
  if (!Array.isArray(images) || !images.length) return;
  const gallery = document.createElement('div'); gallery.className = 'image-gallery';
  images.forEach((image) => {
    const source = typeof image === 'string' ? image : image?.src;
    if (!source) return;
    const element = document.createElement('img'); element.className = 'quiz-image';
    element.src = source;
    element.alt = typeof image === 'object' && image.alt ? displayText(image.alt, languages) : fallbackAlt;
    element.loading = 'lazy';
    gallery.append(element);
  });
  if (gallery.children.length) parent.append(gallery);
}

export function renderQuiz() {
  const languages = selectedLanguages();
  const variantIndex = Number($('variant').value);
  const container = $('questions'); container.replaceChildren();
  state.quiz.forEach((question, index) => {
    const fieldset = document.createElement('fieldset'); fieldset.className = 'question';
    const legend = document.createElement('legend');
    legend.textContent = `${index + 1}. ${displayQuestion(question, languages, variantIndex)}`;
    fieldset.append(legend);
    appendImages(fieldset, question.images, languages, 'Question illustration');
    if (question.type === 'source-reference') {
      const reference = document.createElement('p'); reference.className = 'reference-answer';
      reference.textContent = `${displayReferenceAnswer(question, languages)}\nSource: ${question.sourcePath || question.source?.url || 'included reference metadata'}`;
      fieldset.append(reference);
    } else if (question.type === 'multi-step' || question.answerMode === 'subjective') {
      if (question.type === 'multi-step') question.steps.forEach((step, stepIndex) => {
        const wrapper = document.createElement('div'); wrapper.className = 'math-step';
        const label = document.createElement('label'); label.textContent = `${stepIndex + 1}. ${displayText(step.prompt, languages)}`;
        const input = document.createElement('input'); input.type = 'text'; input.autocomplete = 'off';
        input.dataset.questionIndex = index; input.dataset.stepIndex = stepIndex;
        label.append(input); wrapper.append(label); fieldset.append(wrapper);
      });
      else {
        const label = document.createElement('label'); label.className = 'math-step'; label.textContent = 'Answer:';
        const input = document.createElement('input'); input.type = 'text'; input.autocomplete = 'off';
        input.dataset.questionIndex = index; label.append(input); fieldset.append(label);
      }
    } else question.answers.forEach((answer) => {
      const label = document.createElement('label'); label.className = 'answer';
      const input = document.createElement('input'); input.type = Array.isArray(question.correctAnswer) ? 'checkbox' : 'radio'; input.name = `question-${index}`; input.value = answer.id;
      label.append(input, document.createTextNode(`${answer.id}. ${displayText(answer.text, languages)}`));
      appendImages(label, answer.images, languages, `Answer ${answer.id} illustration`);
      fieldset.append(label);
    });
    container.append(fieldset);
  });
  $('progress').textContent = `${state.quiz.length} question(s)`;
  $('score').textContent = '';
}
