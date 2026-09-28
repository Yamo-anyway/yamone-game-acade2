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

## Color Break v0.11.0 score ceiling

After pulling the latest main, apply the additive migration from this directory:

```sh
npx --yes wrangler@latest d1 migrations apply yamone-games-ranking --remote
```

`0003_color_break_endless.sql` raises only `color_break / normal` to 1,000,000,000
points for endless runs. It does not reset rankings or player data, and no Worker
redeploy is required for this data-only change. Existing admin max-score settings
can also be used to raise the same row. Until applied, scores above 100,000 stay
local/pending; the existing server rejects them. The old test leaderboard is not
automatically reset—use the existing admin reset when the owner chooses to do so.

## Orbit Snap v0.12.0 score ceiling

After pulling the latest main, run the same migration command above.
`0004_orbit_snap_endless.sql` raises `orbit_snap / normal` to 1,000,000,000
points for endless play, retaining leaderboard records and both reset epochs.
No Worker deploy or new secret is needed. Scores above 100,000 remain pending
until this migration is applied. Any still-pending Color Break migration is
applied by the same command. These migrations never clear test records.

## Tap Tap v0.13.0 name and score ceiling

After pulling main, run the migration command above to apply
`0005_tap_tap_endless.sql`. It changes the catalog display title to `탭탭` and
raises its ceiling to at least 1,000,000,000. The stable key remains
`twin_tap / normal / points`; rankings, play receipts, placement and reset epochs
are retained. Android shows the new name immediately. The admin/catalog title
and scores above 100,000 need this database migration; no Worker redeploy is
required. Pending Orbit/Color Break migrations are applied by the same command.

## Pocket Pulse v0.14.0 and Line Surf visibility

After `git pull --ff-only`, run from this directory:

```sh
npx --yes wrangler@latest d1 migrations apply yamone-games-ranking --remote --config wrangler.toml
```

`0006_pocket_pulse_endless.sql` raises `pocket_pulse / normal` to 1,000,000,000
points and disables/not-features `line_surf` only in `yamone_arcade2` placement.
Shared game IDs, existing rankings, analytics, reset epochs and other apps are
preserved. The Android app also hides Line Surf when offline or using an older
catalog. Worker redeployment and secret changes are not required for this migration.
