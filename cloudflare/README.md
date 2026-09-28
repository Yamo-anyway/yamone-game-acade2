# Shared ranking operations Worker

Deployable source for the existing `yamone-games-ranking-api` Worker and D1.
The public ranking/play/catalog contract is shared by Yamone Games and Arcade 2.

## Admin hardening — prepared, not deployed

Read [ADMIN_SECURITY.md](ADMIN_SECURITY.md) for findings, Cloudflare references,
required Access configuration, rollout, verification and rollback instructions.
This patch replaces the admin Bearer login with a Cloudflare Access session.
**Do not deploy before configuring and reviewing Access and `ADMIN_ORIGIN`.**
Empty Access variables deliberately disable administration while public game
APIs remain available. `/admin` moves to the configured administrator hostname.

No new D1 migration or secret change is required for this hardening patch.
Preserve `RANKING_SIGNING_SECRET`: it is used for existing player HMAC identity.
The existing `RANKING_ADMIN_SECRET` is unused by this code; do not rotate/delete
it as part of this patch. Never commit secrets, cookies or tokens.

## Local verification

From the repository root, with Node 22+ and Python 3:

```bash
npm ci --prefix cloudflare
bash scripts/test-operations.sh
npm --prefix cloudflare run check:bundle
```

The bundle command is `wrangler deploy --dry-run`; it does not deploy a Worker.
The tests generate ephemeral signing keys and never call production.

- Public health: `https://yamone-games-ranking-api.yamone-game.workers.dev/health`
- Public catalog: `/v1/catalog?appId=yamone_arcade2`
- Admin after approved rollout: `${ADMIN_ORIGIN}/admin`

The existing `0002_game_operations.sql` migration belongs to the previous M09
rollout; this security patch does not reapply it. Hiding a game preserves rankings
and analytics. Ranking reset advances the selected board's epoch and preserves
play history. The Android/public API contract is unchanged.
