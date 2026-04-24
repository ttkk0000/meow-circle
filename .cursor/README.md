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

## Stitch MCP (Google)

**Settings → Tools & MCP** can show Stitch as **On** with many tools. That only means Stitch is installed for Cursor clients that load your merged MCP config (`~/.cursor/mcp.json` plus optional project `.cursor/mcp.json`).

Some **Agent / Chat backends** do not attach the same MCP servers to the model. Use **View → Output → MCP Logs** to confirm what loaded.

**Cursor agent `call_mcp_tool` server id:** Settings may label the server **`stitch`**, but the registered identifier is often **`user-<key>`** from `mcp.json` — e.g. **`user-stitch`** for `"mcpServers": { "stitch": { ... } }`. Confirm in workspace metadata: `.cursor/projects/<workspace-folder>/mcps/user-stitch/SERVER_METADATA.json` (`serverIdentifier`). Calling **`stitch`** can fail with “MCP server does not exist” even when Stitch is On; try **`user-stitch`** first. Cloud Agents still need MCP configured on that product if the server list is empty there.

**Secrets:** never commit API keys or bearer tokens. Keep them only in local Cursor config and ensure those files stay out of git.

**CLI fallback:** `scripts/stitch-export.ps1` can write HTML into `web/_stitch_ref/` when you have Stitch CLI auth set up locally.

### Documentation links (official + machine-readable)

- **Stitch product docs (browser):** [stitch.withgoogle.com/docs](https://stitch.withgoogle.com/docs)
- **MCP setup:** [stitch.withgoogle.com/docs/mcp/setup](https://stitch.withgoogle.com/docs/mcp/setup)
- **MCP tool reference:** [stitch.withgoogle.com/docs/mcp/reference](https://stitch.withgoogle.com/docs/mcp/reference)
- **Remote MCP URL (Cursor `url`):** `https://stitch.googleapis.com/mcp` — authenticate with a Stitch **API key** header per Google’s setup doc (do not paste keys into the repo).
- **Programmatic / agent reference (`@google/stitch-sdk`, tools + API table):** [google-labs-code/stitch-sdk `README.md` (raw)](https://raw.githubusercontent.com/google-labs-code/stitch-sdk/main/README.md)

The `stitch.withgoogle.com/docs/*` pages are largely **client-rendered**; plain HTTP fetches from tooling often return an empty shell. Prefer the browser for those URLs, and the **stitch-sdk raw README** when you need copy-pasteable tool names and parameters inside an agent workflow.
