---
trigger: always_on
description: Optional Graphify knowledge graph — regenerate when needed.
---

## graphify

`graphify-out/` is optional (removed from the repo; regenerate locally if useful).

Rules:
- If `graphify-out/graph.json` exists, for codebase/architecture questions prefer `graphify query "<question>"` (CLI) or `query_graph` (MCP). Use `graphify path` / `shortest_path` and `graphify explain` / `get_node` when helpful.
- If `graphify-out/wiki/index.md` exists, navigate it instead of raw files when useful.
- Read `graphify-out/GRAPH_REPORT.md` only for broad architecture review when query/path/explain are not enough.
- To recreate the graph: `graphify update .` (AST-only). Do not assume `graphify-out/` is committed.
