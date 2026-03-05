// Unified REPL terminal — keyboard handling, history, auto-submit

(function() {
  var history = [];
  var historyIndex = -1;
  var pendingInput = '';

  function autoResize(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = textarea.scrollHeight + 'px';
  }

  function scrollToBottom() {
    var container = document.getElementById('repl-container');
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  }

  var wasUnbalanced = false;

  function initRepl() {
    var input = document.getElementById('repl-input');
    var form = document.getElementById('repl-form');
    var container = document.getElementById('repl-container');

    if (!input || !form) return;

    // Auto-resize textarea as content grows
    input.addEventListener('input', function() {
      autoResize(input);
    });

    // Click anywhere in the container to focus input
    if (container) {
      container.addEventListener('click', function(e) {
        if (e.target === container || e.target.id === 'repl-history' ||
            e.target.closest('#repl-history')) {
          input.focus();
        }
      });
    }

    // Keyboard handling
    input.addEventListener('keydown', function(e) {
      // Ctrl+C or Escape: clear input
      if (e.key === 'Escape' || (e.ctrlKey && e.key === 'c')) {
        e.preventDefault();
        input.value = '';
        historyIndex = -1;
        pendingInput = '';
        autoResize(input);
        return;
      }

      // Up arrow: previous history entry
      if (e.key === 'ArrowUp' && input.selectionStart === 0) {
        e.preventDefault();
        if (history.length === 0) return;
        if (historyIndex === -1) {
          pendingInput = input.value;
          historyIndex = history.length - 1;
        } else if (historyIndex > 0) {
          historyIndex--;
        }
        input.value = history[historyIndex];
        autoResize(input);
        return;
      }

      // Down arrow: next history entry
      if (e.key === 'ArrowDown' && input.selectionEnd === input.value.length) {
        e.preventDefault();
        if (historyIndex === -1) return;
        if (historyIndex < history.length - 1) {
          historyIndex++;
          input.value = history[historyIndex];
        } else {
          historyIndex = -1;
          input.value = pendingInput;
        }
        autoResize(input);
        return;
      }

      // Enter: always submit (server decides if balanced)
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        if (input.value.trim() === '') return;
        htmx.trigger(form, 'submit');
        return;
      }

      // Shift+Enter: insert newline (default behavior)
    });

    // Server says input is unbalanced — insert newline
    document.body.addEventListener('unbalanced', function() {
      wasUnbalanced = true;
      var pos = input.selectionStart;
      var val = input.value;
      input.value = val.substring(0, pos) + '\n' + val.substring(pos);
      input.selectionStart = input.selectionEnd = pos + 1;
      autoResize(input);
      input.focus();
    });

    // After HTMX request completes: clear input, scroll, refocus
    form.addEventListener('htmx:afterRequest', function(e) {
      if (wasUnbalanced) {
        wasUnbalanced = false;
        return;
      }
      if (e.detail.successful) {
        var code = input.value.trim();
        if (code && (history.length === 0 || history[history.length - 1] !== code)) {
          history.push(code);
        }
        historyIndex = -1;
        pendingInput = '';
        input.value = '';
        input.removeAttribute('placeholder');
        autoResize(input);
        setTimeout(scrollToBottom, 10);
        input.focus();
      }
    });

    // Initial focus
    input.focus();
    scrollToBottom();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initRepl);
  } else {
    initRepl();
  }
})();
