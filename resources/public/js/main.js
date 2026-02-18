// When plain htmx isn't quite enough, you can stick some custom JS here.

// REPL enhancements
(function() {
  function initRepl() {
    const textarea = document.getElementById('code-input');
    const form = textarea?.closest('form');
    const editorContainer = document.getElementById('editor-container');

    if (!textarea || !form) return;

    // Ctrl+Enter to submit
    textarea.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        htmx.trigger(form, 'submit');
      }
    });

    // Clear textarea after successful submit
    form.addEventListener('htmx:afterRequest', () => {
      textarea.value = '';
      textarea.focus();
    });

    // Try to load CodeMirror for syntax highlighting
    loadCodeMirror(textarea, form, editorContainer);
  }

  async function loadCodeMirror(textarea, form, container) {
    if (!container) return;

    try {
      const [cmModule, viewModule, langModule, themeModule] = await Promise.all([
        import('https://esm.sh/codemirror@6.0.1'),
        import('https://esm.sh/@codemirror/view@6.26.0'),
        import('https://esm.sh/@nextjournal/lang-clojure@1.0.0'),
        import('https://esm.sh/@codemirror/theme-one-dark@6.1.2')
      ]);

      const { EditorView, basicSetup } = cmModule;
      const { keymap } = viewModule;
      const { clojure } = langModule;
      const { oneDark } = themeModule;

      const submitKeymap = keymap.of([{
        key: 'Ctrl-Enter',
        mac: 'Cmd-Enter',
        run: (view) => {
          textarea.value = view.state.doc.toString();
          htmx.trigger(form, 'submit');
          setTimeout(() => {
            view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: '' } });
          }, 50);
          return true;
        }
      }]);

      const customTheme = EditorView.theme({
        '&': { backgroundColor: '#1f2937', height: '96px' },
        '.cm-content': { padding: '12px', caretColor: '#10b981' },
        '.cm-gutters': { backgroundColor: '#1f2937', border: 'none' },
        '&.cm-focused': { outline: 'none' }
      });

      const view = new EditorView({
        doc: '',
        extensions: [
          basicSetup,
          clojure(),
          oneDark,
          customTheme,
          submitKeymap,
          EditorView.updateListener.of((update) => {
            if (update.docChanged) textarea.value = update.state.doc.toString();
          }),
          EditorView.lineWrapping
        ],
        parent: container
      });

      // Hide textarea, show CodeMirror
      textarea.style.display = 'none';
      view.focus();

      form.addEventListener('htmx:afterRequest', () => {
        view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: '' } });
        view.focus();
      });

      window.replEditor = view;
    } catch (err) {
      console.log('CodeMirror not loaded, using textarea:', err.message);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initRepl);
  } else {
    initRepl();
  }
})();
