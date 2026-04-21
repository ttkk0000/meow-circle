---
name: besttry-design-user-context
description: >-
  Use when building or reviewing bestTry UI/UX (web or mobile), user-visible
  copy, theming, or layout; when the user references DESIGN.md, bestTry design,
  warm minimalism, or Cursor-inspired product visuals. Provides user-facing
  experience principles from DESIGN.md sections 1–11; pixel tokens stay in-repo.
---

# bestTry UI and experience (user-facing)

**This file is Git-tracked in the repo and is canonical.** The user-level file `~/.cursor/skills/besttry-design-user-context/SKILL.md` should remain an entry-point only (no full mirror). **Rules** already include short summaries (`.cursor/rules/design-user-context.mdc` + `design-system.mdc`); this SKILL is the **expanded narrative** for `DESIGN.md` §1–11 without repeating the token tables line by line.

When building UI, motion, themes, or user-visible copy in **bestTry**, treat the following as the source of truth for *what users feel*; for hex, type scale, and pixel specs open `DESIGN.md` from section 12 onward, or `web/theme.css`.

## When to read this skill

- Writing or editing **React Native (mobile)** or **Web** screens and components
- Writing **product copy, errors, empty states**
- The user mentions **design system, DESIGN, warm palette, Cursor-like look**

## User-facing principles (maps to `DESIGN.md` sections 1–11)

### What this doc is for

- Shared by collaborators and implementers: UI should feel **warm, restrained, readable**; strong contrast for what truly matters.

### Overall tone

- **Warm canvas:** soft cream base, not harsh white; paper/print, not “console gray.”
- **Deep type:** warm near-black for comfortable long reading.
- **Layering:** light blocks + soft shadows + fine borders; important modules lift slightly — no exaggeration.
- **Brand accents:** emphasis color for links and highlights; interaction uses signature **warm deep red** to signal clickability and response.
- In one line: **well-typeset magazine + capable tool UI** — not traditional cold blue-gray admin chrome.

### What colors mean to users

| Perception | Meaning |
|-------------|---------|
| Dark type on cream/light gray | Primary reading |
| Orange accent | Links, brand highlight, guidance without panic |
| Warm / rose red | Errors, danger, or strong interactive feedback (context-dependent) |
| Green | Success, positive |
| Soft colored bands | Process/timeline stages, not alarms |

Mnemonic: **green good, red bad or strong alert, orange/links guide** (users should not memorize hex).

### Type and reading

- Large headlines: tight, expressive; hierarchy stays clear when scaled down on narrow screens.
- Body: serif, slightly editorial, **reads well in long form**.
- Codes, order IDs, technical labels: monospace, clearly distinct from body.
- Small text: supporting only — not for critical decisions.

Principle: **headlines pop, body scans, secondary text stays quiet.**

### Controls (user lens)

- **Primary buttons:** light fill + dark type; hover text often shifts **warm red** to signal clickability.
- **Secondary / tags:** pill shapes; selected state reads slightly deeper.
- **Ghost buttons:** lighter for cancel and secondary paths.
- **Cards / lists:** one idea per card; fine border, moderate radius; hover may deepen shadow if the product implies drill-in.
- **Inputs:** subtle default border; stronger but **warm** focus — no harsh cold-blue halos.
- **Navigation:** clean; current location obvious; auth flows don’t clutter browsing.

### Themes

- Multiple themes (light/warm/dark): light/warm for daytime long reads; dark for low light while keeping success/error/link distinction.
- Font and language prefs are **remembered separately** from theme — switching theme should not scramble reading settings.

### Mobile and narrow screens

- **Single column first**; touch targets stay generous.
- **Same** corner/border language as desktop — avoid feeling like “another product.”
- Tighter spacing but keep breathing room between blocks to reduce mis-taps.

### Motion

- Color and shadow transitions **short and smooth**.
- Hover/focus gives clear feedback; **do not** rely on “only animation explains the state.”
- Avoid long harsh flashes or large jumps.

### Voice and copy

- **Clear, friendly, low jargon**; errors: **what happened + what the user can do** — not raw field names or stack traces alone.
- **Warm ≠ childish**; orders, payments, and account security stay concise and trustworthy.

### Design lineage (one line)

Visual DNA references **Cursor’s site**: warm minimal — cream base, near-black type, warm-red interaction, three-type hierarchy. Product-facing paraphrase here; pixels live in `DESIGN.md` §12+ and `web/theme.css`.

### Pointers for implementers

- Single source of tokens: `web/theme.css` (plus page/component styles).
- Project rule `.cursor/rules/design-system.mdc` (code-level tokens/components) complements this skill.

## While executing

- If User Rules already include the merged condensed block (see `.cursor/USER_RULES_PASTE_DESIGN.md` or `~/.cursor/USER_RULES_PASTE_DESIGN.md` with Karpathy + this repo’s user-facing bullets), this file is the **expanded user-facing layer**; on conflict, `DESIGN.md` and `theme.css` win.
- Do not sacrifice readability, contrast, or touch targets for decoration.
