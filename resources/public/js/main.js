// When plain htmx isn't quite enough, you can stick some custom JS here.

// CodeMirror 6 integration for the REPL
(function() {
  // Only run on pages with the editor container
  if (!document.getElementById('editor-container')) {
    return;
  }

  // Load CodeMirror modules dynamically from ESM CDN
  async function loadCodeMirror() {
    const [
      { EditorView, basicSetup },
      { EditorState },
      { keymap },
      { clojure },
      { oneDark }
    ] = await Promise.all([
      import('https://esm.sh/codemirror@6.0.1'),
      import('https://esm.sh/@codemirror/state@6.4.1'),
      import('https://esm.sh/@codemirror/view@6.26.0'),
      import('https://esm.sh/@nextjournal/lang-clojure@1.0.0'),
      import('https://esm.sh/@codemirror/theme-one-dark@6.1.2')
    ]);

    const container = document.getElementById('editor-container');
    const textarea = document.getElementById('code-input');
    const form = container.closest('form');
    const placeholder = container.dataset.placeholder || '(+ 1 2 3)';

    // Custom keybindings for REPL
    const submitKeymap = keymap.of([{
      key: 'Ctrl-Enter',
      mac: 'Cmd-Enter',
      run: (view) => {
        // Sync content to textarea and submit form
        textarea.value = view.state.doc.toString();
        if (form) {
          // Use htmx to submit
          htmx.trigger(form, 'submit');
          // Clear editor after submit
          setTimeout(() => {
            view.dispatch({
              changes: { from: 0, to: view.state.doc.length, insert: '' }
            });
          }, 50);
        }
        return true;
      }
    }]);

    // Theme customization
    const customTheme = EditorView.theme({
      '&': {
        backgroundColor: '#1f2937',
        height: '120px'
      },
      '.cm-content': {
        padding: '12px',
        caretColor: '#10b981'
      },
      '.cm-gutters': {
        backgroundColor: '#1f2937',
        border: 'none'
      },
      '.cm-placeholder': {
        color: '#6b7280'
      },
      '&.cm-focused': {
        outline: 'none'
      }
    });

    // Create editor
    const state = EditorState.create({
      doc: '',
      extensions: [
        basicSetup,
        clojure(),
        oneDark,
        customTheme,
        submitKeymap,
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            textarea.value = update.state.doc.toString();
          }
        }),
        EditorState.tabSize.of(2),
        EditorView.lineWrapping
      ]
    });

    const view = new EditorView({
      state,
      parent: container
    });

    // Focus the editor
    view.focus();

    // Handle form submission to clear editor
    if (form) {
      form.addEventListener('htmx:afterRequest', () => {
        view.dispatch({
          changes: { from: 0, to: view.state.doc.length, insert: '' }
        });
        view.focus();
      });
    }

    // Store view for debugging
    window.replEditor = view;
  }

  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadCodeMirror);
  } else {
    loadCodeMirror();
  }
})();
