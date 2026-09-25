# Vesna 包花园 — Package Garden

Vesna 官方包下载站：静态 registry 索引 + zip 包分发，托管于 GitHub Pages。

[English](README.md) | [中文](README.zh-CN.md)

## 站点结构

| 文件 | 说明 |
|---|---|
| `index.html` | 包浏览页（列表 / 搜索 / 详情 / 复制安装命令） |
| `registry.json` | vpm 索引（包列表，含下载 URL 与 SHA-256） |
| `packages/<name>/<name>-<version>.zip` | 包文件（内含 `vesna-pkg.json` + 源码） |

## vpm 使用

```bat
rem 一次性配置索引（也可省略 URL，使用默认值）
vesna --pkg registry https://youakasakura-YAS.github.io/vesna-pkg/registry.json

rem 搜索并安装
vesna --pkg search strutil
vesna --pkg install strutil
```

安装后的包位于 `<VESNA_HOME>\packages\<name>\`，直接 `import <name>` 使用。

## 发布新包

1. 在 `packages/<name>/` 下放好 `vesna-pkg.json` 与源码。
2. 打 zip：`Compress-Archive -Path vesna-pkg.json,*.ves -DestinationPath <name>-<version>.zip`。
3. 计算 SHA-256 并追加条目到 `registry.json`（`url` 指向 `https://youakasakura-YAS.github.io/vesna-pkg/packages/<name>/<name>-<version>.zip`）。
4. 推送本仓库（main 分支），GitHub Pages 自动发布。

## License

MIT
