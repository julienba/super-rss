# Clojure REPL Evaluation

The command `clj-nrepl-eval` is installed on your path for evaluating Clojure code via nREPL.

**Discover nREPL servers:**

`clj-nrepl-eval --discover-ports`

**Evaluate code:**

`clj-nrepl-eval -p <port> "<clojure-code>"`

With timeout (milliseconds)

`clj-nrepl-eval -p <port> --timeout 5000 "<clojure-code>"`

The REPL session persists between evaluations - namespaces and state are maintained.
Always use `:reload` when requiring namespaces to pick up changes.

# Linting with clj-kondo

Use `clj-kondo` to lint Clojure code.

**Lint files or directories:**

`clj-kondo --lint <path>`

**Lint multiple directories:**

`clj-kondo --lint src test`

**Lint the entire project:**

`clj-kondo --lint .`
