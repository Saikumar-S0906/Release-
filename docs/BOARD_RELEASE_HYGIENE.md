# Board UI — docs / release hygiene

Honest status for demos, internal tracks, and team handoff. Board Compose is the production UI path; **soft AI** still applies.

## Production chrome (IA)

**5 tabs:** Home · Explore · Create · Community · My Stuff  

| Tab | Root ID |
|-----|---------|
| Home | `b2_home` |
| Explore | `b2_discover` |
| Create | `b_create_hub` |
| Community | `b27_feed` |
| My Stuff | `b18_browse` |

Profile (`b9_open`) is a screen inside My Stuff / avatar — not a tab label. CreateForMe / MemeGenerator routes redirect to board Beautiful / Funny.

## What “Done” means

| Claim | Truth |
|-------|--------|
| Catalog `Status.Done` / UI label **UI Done / soft AI** | Compose screens + edges wired; kits (trust, credits, tips) in place |
| Soft AI | Generate, OAuth, sync, payments, community backends are **simulated** walkthroughs |
| L2 imagery | Hosts use `BoardFlowArt` / `BoardToolArt` packs |
| L3 pixel lock | **Not claimed** until you approve in [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md) |
| Live Gemini / billing / ads | Separate from board soft AI — see [INFRASTRUCTURE.md](INFRASTRUCTURE.md), [GEMINI_TESTING.md](GEMINI_TESTING.md) |

Do **not** describe board generate screens as live generative AI in release notes, Play listings, or investor demos unless backends are actually wired.

## Canonical boards (rematch wins)

Use **only** `flows/additional/REMATCH_*` for Beautiful / Funny / Tell / Stickers / Move / Different. Older core and `FLOW_*` pack copies were removed.

## Release posture (current)

| Item | Value |
|------|--------|
| `applicationId` | `com.pixijoy.studio` |
| `versionName` / `versionCode` | `1.0.1-internal` / `2` (bump again before production Play upload) |
| Board UI | Live path (tabs + onboarding → board hosts) |
| Board generate | Soft / simulated; optional live Gemini probe when `BOARD_LIVE_GENERATE=true` + real key |
| Debug NavHost | Flow Index / Board UI Index / Responsive QA registered only when `BuildConfig.DEBUG` |
| Redirect prune | Legacy `AppDestination` redirects kept for deep links; delete after one internal-track cycle if unused |
| Privacy / terms | In the app. No website. Confirm both screens before store. |
| Secrets | `local.properties` only — never commit keys ([local.properties.example](../local.properties.example)) |

## Doc map

| Doc | Role |
|-----|------|
| [BOARD_UI_CATALOG.md](BOARD_UI_CATALOG.md) | How to open + batch status |
| [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md) | Your phone/tablet approve |
| [BOARD_ALL_FLOWS_PIXEL_PLAN.md](BOARD_ALL_FLOWS_PIXEL_PLAN.md) | L1/L2/L3 map |
| [flows/README.md](../flows/README.md) | Active vs superseded boards |
| [WHAT_NEXT.md](WHAT_NEXT.md) | Manual smoke before Play |
| [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md) | Gemini / billing / ads / URLs checklist |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | Gemini / Firebase / AdMob / Billing |
| [pixijoy-cursor-nav/docs/NAVIGATION.md](../pixijoy-cursor-nav/docs/NAVIGATION.md) | IA lock (5 tabs, Tools \| AI) |

## Polish themes completed (execution quality)

Reliability · Visual consistency · Motion · Performance · Accessibility · Product craft · Docs / release hygiene · Typography & chrome · **QA depth** (batched catalog + DAY + golden keys).
