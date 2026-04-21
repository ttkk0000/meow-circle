# Cursor User Rules — Karpathy coding + bestTry user-facing UX (Git-tracked)

This file is versioned in the repo. **Inside the fence is one complete User Rules block**: it merges the four condensed Karpathy principles from [andrej-karpathy-skills](https://github.com/forrestchang/andrej-karpathy-skills) with the bestTry user-facing UX block; the two themes differ and do not duplicate clauses. Paste into **Cursor → Settings → Rules (or Rules & Memory) → User Rules** (once per machine).

**Global user copy** (keep in sync with this file): `~/.cursor/USER_RULES_PASTE_DESIGN.md`.

Expanded versions: `.cursor/skills/karpathy-guidelines/SKILL.md`, `.cursor/skills/besttry-design-user-context/SKILL.md` (mirrors may also live under `~/.cursor/skills/.../SKILL.md`).

---

```
Follow these four behavioral principles on every coding task (Karpathy-inspired; upstream: https://github.com/forrestchang/andrej-karpathy-skills):

1. Think before coding. State assumptions. If ambiguous, present options instead of picking silently. If unclear, stop and ask. Push back when a simpler approach exists.

2. Simplicity first. Write the minimum code that solves the stated problem. No speculative abstractions, no unasked-for flexibility, no error handling for impossible cases. If it could be half the size, rewrite it.

3. Surgical changes. Every changed line must trace directly to the user's request. Don't "improve" adjacent code, comments, or formatting. Don't refactor things that aren't broken. Match existing style. Mention unrelated dead code — don't delete it.

4. Goal-driven execution. Turn imperative asks into verifiable goals ("write a failing test, then make it pass"). For multi-step work, state a brief plan with a verify step per item, then loop until verified.

For the full Karpathy rubric, read `.cursor/skills/karpathy-guidelines/SKILL.md` in this workspace or `~/.cursor/skills/karpathy-guidelines/SKILL.md` when the task is non-trivial. These bias toward caution over speed; use judgment on trivial edits.

---

In the bestTry project, or when the user explicitly mentions DESIGN.md / bestTry UI, also follow these user-facing experience constraints:

- Tone: warm, restrained, readable; warm cream surfaces + warm near-black type; reserve strong contrast for key information and primary actions. Feel like a well-typeset magazine plus a solid tool UI — not a cold blue-gray admin dashboard.
- Color semantics: green = success; warm red = errors or strong alerts / some interactive feedback; orange = links and guidance; soft color bands = process steps, not alarms. Users should not need to memorize hex values.
- Typography: headlines that stand out, body copy that scans well (long-form reads smoothly), monospace for technical content, small text does not carry critical decisions.
- Controls: primary buttons feel lightly physical; hover uses warm red to signal clickability; filters/tags use pills; cards use fine borders and light shadows; focus stays warm — no cold blue halos.
- Themes: light/warm for long reading; dark themes keep semantic color distinction; font and language preferences stay independent of theme.
- Narrow screens: single column first, adequate touch targets, same corner/border language as desktop (not “a different product”).
- Motion: short and smooth; meaning must not depend on “only the animation explains it.”
- Copy: clear, friendly, low jargon; errors explain what happened + what the user can do; orders/payments/security stay concise and trustworthy; warm ≠ childish.
- Pixel-level tokens and English specs: repo `DESIGN.md` from section 12 onward, `web/theme.css`, and project `.cursor/rules/design-system.mdc`.

Outside the bestTry repo you may ignore the preceding UX bullets unless the user asks; the four Karpathy principles still apply.
```

---

## How to open User Rules

1. Press `Ctrl + ,` to open Settings.
2. In the sidebar, open **Rules** (or **Rules & Memory**).
3. Find **User Rules**, paste the fenced block above, and save.

## How this relates to other repo context (layered, minimal duplication)

| Layer | Path | Notes |
|------|------|------|
| Always attached (short) | `design-system.mdc`, `design-user-context.mdc`, `karpathy-guidelines.mdc` | Summaries; **do not** re-read the full SKILL files in the same turn |
| On demand | `skills/karpathy-guidelines/SKILL.md`, `skills/besttry-design-user-context/SKILL.md` | Full detail |
| Local User Rules | Paste the fenced block above | Not in Git; keep aligned with this file |
