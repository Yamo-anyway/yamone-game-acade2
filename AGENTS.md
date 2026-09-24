# Development continuity

- This is the native Android app in `Yamo-anyway/yamone-game-acade2`; do not replace it with a website.
- Read `docs/PRODUCT.md` and `docs/PROGRESS.md` before continuing. Implement the next unfinished milestone and record real validation results.
- User requested hourly development, no signup, banner ads only, and ranking architecture for a server supplied later.
- Preserve all existing user changes. Use normal fast-forward commits, never force push.
- Keep game engines Android-independent under `core` and verify meaningful scoring/timer/input behavior with `bash scripts/test-core.sh`.
- Run Android lint/build when possible; check the exact commit's CI. Do not claim device testing or advertising display without actually verifying it.
- Use official demo ad IDs in development; no production requests until configured. Ranking remains explicitly disconnected until a server contract is agreed.
- Do not touch unrelated repositories or automations. No subagents unless explicitly requested by the user.
- Increment versionCode/versionName for a completed development stage and update CHANGELOG/PROGRESS. Bug fixes within the same stage may keep its version until it is delivered.
