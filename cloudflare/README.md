# Shared ranking operations Worker

This directory is the deployable source for the existing
`yamone-games-ranking-api` Worker and D1 database. It keeps the public ranking
contract while adding play analytics, a remotely managed game catalog and an
authenticated administration page.

## One-time deployment

Run these commands from this directory with the Cloudflare account that owns
`yamone-games-ranking`:

```bash
npx wrangler d1 migrations apply yamone-games-ranking --remote
npx wrangler secret put RANKING_ADMIN_SECRET
npx wrangler deploy
```

`RANKING_SIGNING_SECRET` already belongs to the existing Worker and must be
preserved. `RANKING_ADMIN_SECRET` must be a separate long random value. It is
entered only in the `/admin` page and kept in that browser tab's session
storage. Never put either secret in Android source or git.

After deployment:

- Health: `https://yamone-games-ranking-api.yamone-game.workers.dev/health`
- Admin: `https://yamone-games-ranking-api.yamone-game.workers.dev/admin`
- Public catalog: `/v1/catalog?appId=yamone_arcade2`

The migration is additive and repeatable. Hiding a game does not remove its
rankings or analytics. A ranking reset deletes only the selected game/mode
leaderboard and advances its epoch. Play receipts and aggregate history remain.

## Safe rollout order

1. Apply the D1 migration.
2. Set the new admin secret.
3. Deploy and smoke-test the Worker.
4. Release Android v0.9.0.

The v0.9.0 client keeps play events offline if it temporarily reaches the older
Worker, while the legacy best-score API continues retrying independently.
