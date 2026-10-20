# Android Editor (`apps/cesdk_android`)

```bash
cd apps/cesdk_android && ./gradlew build                     # library and examples
cd apps/cesdk_android_showcases && ./gradlew assembleDebug   # showcases and starter kits
./scripts/dev/install-android-sdk.sh                         # SDK essentials, once
./scripts/dev/setup-android-emulator.sh                      # template AVD cesdk-smoke-api34; AVD_NAME overrides it
```

- `**/strings.xml` is generated. Edit the TSVs in `scripts/localization/` instead, such as `mobile-editor.tsv`;
  the pre-commit hook runs `scripts/localization/generate.py` and stages the result.
- Generate Compose icons from SVG, from the repo root, with
  `kotlin scripts/icons/svg_to_compose.main.kts --svg <path> --accessor <AccessorName> --package <package> --icon-name <Name>`.
  Output defaults to `apps/cesdk_android/sources/editor-core/src/main/java`. Never hand-write vector paths, never
  commit the source SVGs, and expose each icon through an `IconPack.<Name>` bridge extension.
