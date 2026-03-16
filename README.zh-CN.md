# offer-matrix-android

[中文](./README.zh-CN.md) | [English](./README.en.md)

`offer-matrix-android` 是一个面向求职场景的 AI 模拟面试 Android 客户端，聚焦“岗位化训练 + 简历驱动提问 + 面试记录分析”这一完整闭环，帮助候选人在移动端完成更贴近真实业务场景的面试准备。

## 项目亮点

- AI 模拟面试，支持语音互动
- 基于目标岗位进行差异化练习
- 支持读取简历内容，生成更贴近背景的提问
- 支持题目训练、收藏与学习路径沉淀
- 支持面试记录保存与后续分析反馈
- 支持岗位资料与简历管理

## 适用场景

- 求职前的移动端刷题与模拟面试
- 针对前端、后端、AI、产品、测试、运维等岗位做专项训练
- 想把简历内容带入面试问答流程，提升练习真实性
- 希望复盘历史面试记录、持续优化表现

## 技术栈

- Kotlin
- Jetpack Compose
- Android View + Compose 混合界面
- Retrofit
- Media3
- CameraX
- ByteDance Speech Dialog SDK

## 主要模块

```text
app/src/main/java/com/example/offermatrix/
├── interview/      # 语音面试会话与语音对话逻辑
├── network/        # Retrofit 接口与数据模型
├── ui/navigation/  # 导航逻辑
├── ui/screens/     # 登录、岗位、训练、面试、分析、个人中心页面
├── ui/viewmodel/   # 页面状态管理
└── utils/          # 通用工具
```

## 本地配置

请不要提交语音 SDK 的凭证或 license 文件。

在本地 `local.properties` 中添加：

```properties
speechDialogAppId=your_app_id
speechDialogAppKey=your_app_key
speechDialogAccessToken=your_access_token
```

如果语音 SDK 需要本地 license 文件，请放到：

```text
app/src/main/assets/tts_license
```

该文件已被 gitignore 忽略，应仅保留在本地环境。

## 关联仓库

- 后端服务：[offer-matrix-api](https://github.com/wangyxs/offer-matrix-api)

## 关键词

AI 模拟面试、Android 面试练习、简历驱动面试、岗位训练、面试分析、Jetpack Compose、求职准备
