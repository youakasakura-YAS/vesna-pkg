# Vesna Package Garden

Official Vesna package download site: a static registry index + zip distribution, hosted on GitHub Pages.

[English](README.md) | [中文](README.zh-CN.md)

## Site layout

| File | Description |
|---|---|
| `index.html` | Package browser (list / search / details / copy install command) |
| `registry.json` | vpm index (package list with download URLs and SHA-256) |
| `packages/<name>/<name>-<version>.zip` | Package files (contain `vesna-pkg.json` + source) |

## Using with vpm

```bat
rem one-time index setup (URL optional; default is used if omitted)
vesna --pkg registry https://youakasakura-YAS.github.io/vesna-pkg/registry.json

rem search and install
vesna --pkg search strutil
vesna --pkg install strutil
```

Installed packages live in `<VESNA_HOME>\packages\<name>\` and are used with `import <name>`.

## Publishing a new package

1. Put `vesna-pkg.json` and the source under `packages/<name>/`.
2. Create the zip: `Compress-Archive -Path vesna-pkg.json,*.ves -DestinationPath <name>-<version>.zip`.
3. Compute the SHA-256 and append an entry to `registry.json` (`url` points to `https://youakasakura-YAS.github.io/vesna-pkg/packages/<name>/<name>-<version>.zip`).
4. Push this repository (main branch); GitHub Pages publishes automatically.

## License

MIT
