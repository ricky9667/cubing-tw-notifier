# Issue tracker: GitHub

Issues and specs for this repo live in GitHub Issues. Use `gh` from this repo; it resolves the repository from the Git remote.

Create, read, list, comment on, label, and close issues with `gh issue`. When a skill says "publish to the issue tracker", create a GitHub issue. When it says "fetch the relevant ticket", read the issue and its comments.

## Pull requests as a triage surface

PRs as a request surface: no.

## Wayfinding

Use one issue labelled `wayfinder:map` as the map. Link child tickets as GitHub sub-issues when available; otherwise list them in the map and add `Part of #<map>` to each child. Use native issue dependencies when available; otherwise record `Blocked by: #<n>` in the child. Claim the first unassigned, unblocked open child in map order.
