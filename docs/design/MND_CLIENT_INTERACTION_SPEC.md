# M&D Client Interaction Spec

Last updated: 2026-06-03

This spec maps the Figma-ready M&D direction into the runtime clients, with KMP
Android as the first implementation target and Expo as the matching follow-up.

## 1. Home

Goal: make the first screen feel like a warm pet-life magazine that is still fast
to use.

- Top bar stays compact: avatar, `M&D`, `meow & doggie`, notifications.
- First content block is `今日猫猫编辑台`, not a large logo area.
- Feed filters are `推荐`, `最新`, `关注`; selected state uses the warm crimson
  brand color sparingly.
- Search is a restrained 8dp input on a warm surface, not a large blue pill.
- The editorial module promotes `M&D 猫猫主角季` with concise copy and one icon.
- Feed tiles always have a cover area. If the API has no image, render an
  editorial placeholder cover instead of a bare text card.
- Loading uses skeleton-like warm surfaces.
- Empty state explains what happened and how to continue.
- Error state never exposes Kotlin/JSON/internal API strings. It offers `重试`
  and `看离线演示`.

## 2. Discover

- Search targets circles, activities, and marketplace-adjacent content.
- Circle chips are horizontal pills: `猫猫新手村`, `橘猫联盟`, `黑猫部`,
  `猫咪摄影`, `领养中心`.
- Selecting a circle filters the feed and updates the subheading.
- The weekly topic block explains the current community theme and leads into
  posts, not a generic hero card.
- Empty state should distinguish "no posts in this circle" from "no search
  results".

## 3. Compose

- Compose is a first-class middle tab on KMP.
- The first view should offer modes: `猫猫日常`, `求助问答`, `活动`, `好物交易`.
- Validate title and body before submit.
- Keep a local draft per mode.
- Publish button has loading, success, and retry states.
- Failed publish errors should say what to do next, not show HTTP internals.

## 4. Post Detail

- Media leads when present; otherwise the title/content leads in an editorial
  panel.
- Author row includes follow and private-message affordances when allowed.
- Like uses optimistic feedback and can roll back on failure.
- Comments are part of the reading surface; the empty state invites the first
  useful reply.
- The comment composer is fixed at the bottom for signed-in users.

## 5. Messages

- Message home has four sections: private chats, likes/saves, new followers,
  notifications.
- API failure offers the same offline demo affordance as Home.
- Conversation rows show peer, last message, time, and unread count.
- Product/market conversations may later include a compact listing card.

## 6. Profile

- Profile is a hub: identity, bio, stats, posts, settings, theme switch, logout.
- Theme switch labels should be clear: `Sugar`, `Mint`, `Night`.
- Profile post grid should use image-first cells when possible and editorial
  placeholders otherwise.
- Logout is present but visually secondary until the settings section.

## 7. Visual Rules

- Standard cards, panels, inputs, and tiles use 8dp radius.
- True pills are reserved for filters, chips, and compact actions.
- Sugar theme uses warm cream, near-black text, and warm crimson; avoid large
  candy-pink blocks.
- Do not expose `Mock`, `JsonArray`, `JsonNull`, route names, or serializer
  details on user-facing screens.
- Doggie appears as companion content in activities, services, adoption, and
  secondary stories; cats remain the emotional center.
