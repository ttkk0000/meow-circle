# kitty-circle — `.cursor` index

## Rules (short, always on)

| File | Contents |
|------|----------|
| `rules/design-system.mdc` | Tokens, components, tone, copy and error patterns |
| `rules/design-user-context.mdc` | Color mental model, motion summary (no hex; avoids duplicating the table above) |
| `rules/karpathy-guidelines.mdc` | Karpathy four-point summary (**full text only in** `skills/karpathy-guidelines/SKILL.md`) |

## Skills (expand on demand)

| Path | Purpose |
|------|---------|
| `skills/karpathy-guidelines/SKILL.md` | Karpathy full detail |
| `skills/kitty-circle-design-user-context/SKILL.md` | `DESIGN.md` §1–11 expanded |

In one chat turn: **do not** read the matching `.mdc` and `SKILL.md` end-to-end twice each.

## User Rules (not project rules; paste locally)

- `USER_RULES_PASTE_DESIGN.md`: Karpathy condensed + kitty-circle user-facing UX; keep `~/.cursor/USER_RULES_PASTE_DESIGN.md` aligned.
- User-level `~/.cursor/skills/kitty-circle-design-user-context/SKILL.md` should be an entry-point only and must not duplicate the full project skill content.

## Maintenance

- Change **long explanations** → edit `SKILL.md` only.
- Change **short constraints attached every turn** → edit the matching `.mdc`.
- Change **User Rules fence** → edit `USER_RULES_PASTE_DESIGN.md`, then overwrite the same filename under `~/.cursor/`.
