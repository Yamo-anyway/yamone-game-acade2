# Public debug signing key

`debug.keystore` is deliberately public and is used only for development APKs.
Its alias is `androiddebugkey`; store/key passwords are the standard `android`.
It is not a Cloudflare credential or a production signing key. The release build
does not use this key. Configure private release signing separately for the store.

From v0.14.0 onward, keep this file stable so debug APK updates from disposable
CI runners preserve installed app data. Previous CI builds generated a different
key per runner. The delivered v0.13.0 certificate differs from this key, so a
direct in-place upgrade from that APK is not possible. Its original private key
was not included in the old artifacts and is not available here.

Do not tell testers to uninstall without explaining that this clears their local
records and installation identity. Server history is not removed by this source
change. Uninstalling/reinstalling creates a separate player, as before.

Expected APK certificate SHA-256:
`09cb96bf0b37d78fd318189f78e49be2735dff62c7a037b1ed149452c8dd6f76`.
