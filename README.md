# 语录随机器（QuotePicker）

功能：
- 白屏隐私入口：右上×3 → 左上×1 → 左下×2（需在 10 秒内完成）
- 分组管理（添加/切换/查看）
- 语录：文本或图片（Base64存储，JPEG质量85）
- 权重设置（按权重加权随机抽取）
- Material3 中文 UI

## 本地构建步骤
1. Android Studio 打开项目根目录
2. 等 Gradle 同步完成
3. 菜单 `Build > Build APK(s)`（或直接 Run 安装）
4. APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 备注
- 最低支持 Android 8.0（API 26）
- 无需任何网络权限，数据保存在本地（Room）