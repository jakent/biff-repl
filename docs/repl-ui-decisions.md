# REPL UI — Design Decisions & Implementation Notes

## Overview

This document covers the decisions made while building the web REPL's terminal-style UI, including server-side balance checking, terminal layout, and input behavior.

## 1. Server-Side Paren Balance Checking

### Problem
The original implementation had paren/bracket/brace balancing logic duplicated in JavaScript (`isBalanced()` and `hasOpenDelimiters()` — ~50 lines). This violated the project convention of keeping business logic in Clojure with minimal JS.

### Decision
Move balance checking to the server. The JS client always submits on Enter and lets the server decide what to do.

### Implementation
- **`balanced?` function** in `src/biff_repl/repl.clj` — walks the string character by character, tracking depth of `(`, `[`, `{` and whether we're inside a string literal. Returns true only when all depths are zero and not inside a string.
- **`eval-handler`** checks `(balanced? code)` before evaluation. If unbalanced, returns an empty 200 response with an `HX-Trigger: unbalanced` header.
- **JS listens for the `unbalanced` event** on `document.body` and inserts a newline at the cursor position, letting the user continue typing.

### Race condition: `wasUnbalanced` flag
HTMX fires `HX-Trigger` events (like `unbalanced`) during response processing, **before** `htmx:afterRequest`. Without a guard, `afterRequest` sees a successful 200 response and clears the input — wiping out the newline that `unbalanced` just inserted.

Fix: a `wasUnbalanced` flag is set in the `unbalanced` handler and checked/reset in `afterRequest` to skip the input-clearing logic.

### Flow
```
User presses Enter
  → JS always submits via HTMX (no client-side balance check)
  → Server checks balanced?
    → If unbalanced: returns empty body + HX-Trigger: unbalanced
      → JS inserts newline, user keeps typing
    → If balanced: evaluates code, returns result HTML
      → JS clears input, adds to history, scrolls down
```

## 2. Terminal Layout — Top-to-Bottom Flow

### Problem
The original layout used `flex flex-col` with `flex-1` on the history div and `flex-shrink-0` on the form. This pinned the input to the bottom of the container, leaving a large empty gap between the welcome message and the input — not how a real terminal behaves.

### Decision
Use natural document flow. The input sits directly after the history content and moves downward as history accumulates.

### Implementation
- `#repl-container` uses `overflow-y-auto` (no flex) with `min-height: 70vh; max-height: 85vh`
- `#repl-history` is a plain div (normal flow)
- `#repl-form` sits right after history in the DOM
- JS scrolls the **container** (not history) to bottom after each eval

```
#repl-container (overflow-y-auto, no flex)
  #repl-history
    ;; Welcome...
    => (+ 1 2)
    3
  #repl-form          ← flows naturally after history
    => [textarea]
```

## 3. Consistent `=>` Prompt Alignment

### Problem
History entries used `"=> "` (trailing space inside `whitespace-pre-wrap`), while the input prompt used `mr-2` (CSS margin). These produced visually different spacing.

### Decision
Use the same rendering approach for both: flex layout with `mr-2` on the `=>` span.

### Implementation
- **History entries** (`result-html`): `[:div.flex.items-start [:span.text-yellow-500.mr-2 "=>"] [:span ... code]]`
- **Input prompt**: `[:span.text-yellow-500.mr-2.leading-6.select-none "=>"]`

## 4. Output Alignment

### Problem
Result output (success/error) was indented with `pl-6` (1.5rem left padding), visually pushing it past the `=>` marker.

### Decision
Output should align with the start of `=>`, not be indented past it. Removed `pl-6` from result divs.

## 5. Textarea Focus Ring Removal

### Problem
The textarea showed a blue browser focus ring/border despite having the `outline-none` Tailwind class.

### Decision
Belt-and-suspenders approach to kill all focus indicators:
- Tailwind classes: `outline-none`, `border-0`, `p-0`, `m-0`
- Inline styles: `outline: none; box-shadow: none`

This handles browser-specific focus styles that Tailwind's `outline-none` alone misses.

## 6. Placeholder Behavior

### Problem
The placeholder text `(+ 1 2 3)` showed every time the input was empty, including after evaluations. This felt noisy for users who already know how the REPL works.

### Decision
Show the placeholder only on first load. After the first successful eval, remove it.

### Implementation
In the `htmx:afterRequest` handler: `input.removeAttribute('placeholder')` after clearing the input on a successful eval.

## 7. JS Responsibilities (Minimal)

After refactoring, JS handles only UI concerns:
- **Enter** → always submit via HTMX
- **Shift+Enter** → insert newline (intentional multiline)
- **`unbalanced` event** → insert newline (server said input is incomplete)
- **Up/Down arrow** → command history navigation
- **Ctrl+C / Escape** → clear input
- **Click container** → focus input
- **After eval** → clear input, remove placeholder, scroll to bottom, refocus
- **Auto-resize** textarea on input

No parsing, no balancing, no Clojure-awareness in JS.

## Files Modified
- `src/biff_repl/repl.clj` — `balanced?` function, `eval-handler` unbalanced branch, `repl-page` layout, `result-html` alignment
- `resources/public/js/main.js` — removed paren logic, added `unbalanced` listener, added `wasUnbalanced` flag, placeholder removal
