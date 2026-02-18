// REPL keyboard shortcuts and enhancements

(function() {
  function initRepl() {
    const textarea = document.getElementById('code-input');
    const form = document.getElementById('repl-form');

    if (!textarea || !form) return;

    // Ctrl+Enter / Cmd+Enter to submit
    textarea.addEventListener('keydown', function(e) {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        htmx.trigger(form, 'submit');
      }
    });

    // Clear textarea and refocus after successful submit
    form.addEventListener('htmx:afterRequest', function(e) {
      if (e.detail.successful) {
        textarea.value = '';
        textarea.focus();
      }
    });

    // Auto-focus on page load
    textarea.focus();
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initRepl);
  } else {
    initRepl();
  }
})();
