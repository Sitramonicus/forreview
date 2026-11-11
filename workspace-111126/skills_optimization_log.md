# Eight-pass skills.md optimization

`skills.md` was refined in eight sequential passes. Each pass searched the verbatim compilation and read the relevant source skill(s); source instructions were filtered for this user's course, learning preferences, and scope.

1. **Incremental work and clarity** — S0061 (Addy, incremental implementation) + S0051 (code simplification). Added a small task contract, runnable slices, and clarity-over-line-count.
2. **Skill routing** — S0071 (Addy, using agent skills) + S0065 (planning/task breakdown). Added task classification, intent-specific search, candidate inspection, 1–3 source limit, and rejection of irrelevant/overly strict source instructions.
3. **Clarification discipline** — S0004 (Superpowers, brainstorming) + S0975 (Matt Pocock, grilling) + S0062 (Addy, interview-me). Inspect context first; ask only a material blocker; no forced interview or design-approval ritual.
4. **Teaching and ADHD accessibility** — S0002 (ADHD-friendly output) + S0977 (teaching). Added one-concept explanations, small worked examples, optional practice, and no forced lesson files/quizzes.
5. **Simplicity and course scope** — S0003 (Karpathy guidelines) + S0040 (Caveman, lean-build). Added observable acceptance, explicit non-goals for broad work, reuse, and justified abstractions.
6. **Debugging** — S0012 (Superpowers, systematic debugging) + S0054 (Addy, debugging/error recovery). Added proportional evidence-first root-cause steps and a stop condition for repeated failed attempts.
7. **Tests and safe refactoring** — S0013 (Superpowers TDD) + S0016 (verification) + S0043 (Caveman safe refactor). Kept red/green when practical, added behavior-preservation checks, and rejected destructive TDD dogma.
8. **Review and integration** — S0050 (Addy code review) + S0992 (Ponytail review). Prioritized concrete correctness/safety findings, small fixes, and relevance; excluded style/performance speculation. Re-read the complete guide for conflicts and duplicate rules.

The active file routes only relevant source material per task. It does not load or apply all 992 source skills at once.
