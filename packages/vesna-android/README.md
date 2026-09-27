# vesna-android

Android 开发工具包（for Vesna）。需要 Vesna **2.6+**。

```ves
import android,
```

## 项目骨架

| 函数 | 说明 |
| --- | --- |
| `android_project(dir; app_id; app_name; min_sdk; target_sdk)` | 生成完整 Android 项目骨架（settings/build.gradle、app 模块、Manifest、MainActivity、布局、strings、主题、.gitignore） |
| `android_manifest(pkg; app_name)` | 生成 AndroidManifest.xml 源码（不落盘） |
| `android_activity(pkg; name)` | 生成 Activity Java 源码（不落盘） |
| `android_strings(app_name)` | 生成 strings.xml 源码（不落盘） |
| `android_gradle(pkg; min_sdk; target_sdk)` | 生成 app/build.gradle 源码（不落盘） |

## 构建与签名

| 函数 | 说明 |
| --- | --- |
| `android_build(dir)` | 运行 `gradlew.bat assembleDebug`（无 wrapper 时回退 `gradle`），返回退出码 |
| `android_permissions()` | 常用危险权限 → 用途对照表（dict） |
| `android_sign_info()` | keystore 签名配置说明文本 |

## 示例

```ves
import android,
android_project("F:\\MyApp"; "com.example.myapp"; "My App"; "24"; "34"),
android_build("F:\\MyApp"),
```
