<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用設計記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.design.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

# 実装用プロンプト設計書（Romcha）

## 目的

- YouTube 公式アプリと並べて使う、閲覧専用のフローティングチャットビューワー「Romcha」を、
  同一要件で AI エージェントに再実装させるための設計書。
- 要件の原本は `docs/PLAN.md`。本書は実装済みの内容と、実装上の決定事項を統合したもの。

## 対象システム概要

- 対象: Android アプリ（applicationId `io.github.filderschoice.romcha`、ライセンス MIT）
- 前提環境: minSdk 34 / targetSdk 36 / compileSdk 36、JDK 17、Gradle 8.13、AGP 8.13.0、Kotlin 2.0.21
- 動作確認端末: Pixel 8 Pro（実機確認は人手検証）

## 実装済み機能要件

- プロジェクト雛形と品質ゲート（静的解析・型検査・単体テスト）

## 設計方針

### モジュール構成

| モジュール | 種別 | 役割 |
| --- | --- | --- |
| `app` | Android アプリ | 起動・権限案内・設定画面・共有インテント受信 |
| `core:chat` | Kotlin/JVM | InnerTube クライアント・応答解析・メッセージモデル（非公式 API 依存をここへ閉じ込める。N-09） |
| `core:sync` | Kotlin/JVM | SyncEngine（位置推定・シーク判定・バッファ管理）。Android 非依存 |
| `core:media` | Android ライブラリ | PlaybackMonitor（NotificationListener + MediaSession） |
| `feature:overlay` | Android ライブラリ | OverlayService・フローティング UI（Compose） |

- 依存の向き: `app` → `feature:overlay` / `core:media` → `core:sync` → `core:chat`。
- 依存バージョンは `gradle/libs.versions.toml` に集約する。

## 非機能要件

- PLAN.md 3章（N-01〜N-11）に従う。
- バックアップ: 設定・履歴は端末内のみ（N-06）。`data_extraction_rules.xml` でクラウドバックアップと端末間移行から除外し、
  `allowBackup=false` とする。

## 実装制約

- 静的解析: ktlint（`ktlint_official`、`@Composable` 関数は命名規則の対象外）、detekt（既定設定＋`config/detekt/detekt.yml` の差分）、
  Android lint（`warningsAsErrors = true`。依存の新版警告のみ `lint.xml` で無効化）。
- 署名鍵（`*.jks` / `*.keystore` / `keystore.properties`）は `.gitignore` で除外する。
- アプリ名・アイコンに YouTube のロゴ・名称を使わない（PLAN 5.5）。

## エージェント実装指示

- 品質ゲートは `CLAUDE.md`「本リポジトリの品質ゲート定義」のコマンドを使う。
- 外部 API（YouTube・GitHub）への実通信はテストで行わない。MockWebServer と保存済み JSON fixture を使う（guardrails 12.5）。
- 参考にした外部実装は `docs/REFERENCES.md` へ記録し、GPL・ライセンス無しのコードは流用しない（PLAN 5章）。
- 要件トレーサビリティ: 実装時は PLAN.md の要件ID（F-*/N-*）をコミット・EXECUTE.md の変更内容へ記載する。

<!-- COPILOT_RECORDS:END -->
