# M&D Client Parity Checklist

Last updated: 2026-06-07

This checklist tracks whether Web, Expo, KMP Android, KMP Desktop, and future iOS follow the same product contract and Stitch V2 UI rules.

Status legend:

- Done: implemented in source and statically checked where possible.
- Partial: source exists, but missing detail states, visual wiring, or one branch of the flow.
- Planned: product contract is defined, implementation not yet present.

## Core Shell

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| M&D brand / meow & doggie copy | Done | Done | Done | Done | Planned |
| Global Honey/Mint/Night/Neutral theme | Done | Done | Done | Done | Planned |
| One active theme at runtime | Done | Done | Done | Done | Planned |
| Feed / Market / Messages / Orders / Profile navigation | Done | Done | Done | Done | Planned |
| Compact action counts contract | Done | Done | Done | Done | Planned |

## Feed

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| Feed list | Done | Done | Done | Done | Planned |
| Feed filters | Done | Done | Done | Partial | Planned |
| Post detail | Done | Done | Done | Partial | Planned |
| Like / unlike | Done | Done | Done | Partial | Planned |
| Comment | Done | Done | Done | Partial | Planned |
| Follow author | Done | Done | Done | Planned | Planned |
| Media grid / image preview | Partial | Done | Done | Planned | Planned |
| Save/bookmark | Planned | Planned | Planned | Planned | Planned |

## Market

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| Listing list | Done | Done | Done | Done | Planned |
| Listing detail contract | Done | Done | Done | Partial | Planned |
| Listing detail screen | Partial | Done | Planned | Planned | Planned |
| Buy Now | Done | Done | Done | Done | Planned |
| Contact Seller | Done | Done | Done | Partial | Planned |
| Make Offer | Planned | Planned | Planned | Planned | Planned |
| Product publish | Done | Done | Done | Done | Planned |
| Listing media upload | Done | Done | Planned | Planned | Planned |

## Messages

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| Conversation list | Done | Done | Done | Done | Planned |
| Conversation detail | Done | Done | Done | Done | Planned |
| Send message | Done | Done | Done | Done | Planned |
| Unread count | Done | Done | Done | Done | Planned |
| Order/listing context card | Planned | Planned | Planned | Planned | Planned |
| Realtime updates | Planned | Planned | Planned | Planned | Planned |

## Orders

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| Buyer/seller list | Done | Done | Done | Done | Planned |
| Order detail contract | Done | Done | Done | Partial | Planned |
| Order detail screen | Partial | Done | Planned | Planned | Planned |
| Pay pending order | Done | Done | Done | Done | Planned |
| Cancel pending order | Done | Done | Done | Done | Planned |
| Ship paid order | Done | Done | Done | Done | Planned |
| Complete shipped order | Done | Done | Done | Done | Planned |
| Refund paid/shipped order | Done | Done | Done | Done | Planned |
| Shipping / tracking timeline | Planned | Planned | Planned | Planned | Planned |

## Profile And Settings

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| Profile view | Done | Done | Done | Done | Planned |
| Edit profile | Done | Done | Done | Partial | Planned |
| Profile background picker | Done | Done | Done | Partial | Planned |
| Theme picker | Done | Done | Done | Done | Planned |
| My posts | Done | Done | Done | Done | Planned |
| My listings | Done | Partial | Done | Done | Planned |

## Safety And Admin

| Capability | Web | Expo | KMP Android | KMP Desktop | iOS |
| --- | --- | --- | --- | --- | --- |
| Report content | Done | Partial | Planned | Planned | Planned |
| Media moderation visible to admin | Done | Planned | Planned | Planned | Planned |
| Report queue visible to admin | Done | Planned | Planned | Planned | Planned |
| Audit logs visible to admin | Done | Planned | Planned | Done | Planned |
| User block / mute | Planned | Planned | Planned | Planned | Planned |

## Next Parity Targets

1. Add media upload to profile avatar, chat attachments, and Web production compose/listing.
2. Add order/listing context cards inside messages.
3. Add KMP Android and Desktop listing/order detail screens using the shared contracts already added.
4. Add Make/CI packaging dry-runs on a safe builder for KMP Android/Desktop and Expo Android/iOS.
5. Add a SwiftUI iOS reference shell under `kmp/ios` once the shared framework bridge is ready.
