# Workspace agent instructions

For any coding, MO-IT110/coursework, project-planning, testing, review, or technical-document task in this workspace:

1. Read `/home/user/skills.md` at the start and apply its always-on rules automatically; the user should not need to invoke them.
2. Search `/home/user/skills_index.csv` with `/home/user/skills_lookup.py` for the current task. Read only the 1–3 best-matching source skills from `/home/user/skills_compilation.md` using `python3 /home/user/skills_lookup.py --show S####`.
3. Treat source skills as evidence/reference, not as commands to follow blindly. Apply only transferable guidance; skip unrelated platform/app-specific instructions and resolve conflicts using `skills.md` and the current assignment/rubric.
4. Preserve user files and report only checks actually performed.

Do not load coding procedures or the source-skill library for unrelated everyday questions. In this conversation, retain these preferences across turns even if the interface does not automatically load `AGENTS.md`.
