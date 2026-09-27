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
- F-SYNC-03/04/05: 位置推定・一時停止・シーク判定・速度追従（`core:sync` の `SyncEngine`）

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

### 同期エンジン（`core:sync`）

- `PlaybackSnapshot`（状態・位置・位置の報告時刻・速度）を入力とし、
  `推定位置 = position + (now - updatedAt) × speed`（再生中のみ加算）で現在位置を求める（`PositionEstimator`）。
- `SeekDetector`: 前回推定位置＋経過時間×前回速度 を期待値とし、±2 秒を超えたらシーク。初回は false。
- `SyncEngine<T>` はメッセージ型に依存しない（オフセットとキーの取り出し関数を受け取る）。通信はせず、
  `tick()`（250ms 間隔）の戻り値 `SyncFrame.fetchRequest` で取得を要求し、結果を `onFetched` / `onFetchFailed` で受ける。
  - 初回・シーク時: 表示をクリアし、`推定位置 - 30 秒` から `restart = true` で取り直す。世代番号を進め、古い世代の応答は捨てる。
  - 先読み: 取得済み範囲の終端が `推定位置 + 60 秒` 未満なら続きを要求する。一時停止中は先読みしない（N-03）。
    応答待ちの間は重ねて要求しない。`hasMore = false` で終端とみなし要求を止める。
  - 表示: 未表示バッファ（オフセット昇順）から推定位置以下のものを表示へ移す。表示位置より前に遅れて届いたものは時刻順に差し込む。
    キーで重複を除く。表示保持は既定 500 件で、超えたら古いものから捨てる（N-04）。
  - `changed = false` の時は `visible` を空で返し、呼び出し側は前回表示を維持する（毎回のリストコピーを避ける）。
  - スレッドセーフではない。単一のコルーチン（単一スレッド）から呼ぶ。

## 非機能要件

- PLAN.md 3章（N-01〜N-11）に従う。
- バックアップ: 設定・履歴は端末内のみ（N-06）。`data_extraction_rules.xml` でクラウドバックアップと端末間移行から除外し、
  `allowBackup=false` とする。

## 実装制約

- ビルド環境のメモリ: 開発機（8GB）に合わせ `org.gradle.jvmargs=-Xmx2g`、`workers.max=2`、
  Kotlin コンパイラは Gradle デーモン内で実行する（`kotlin.compiler.execution.strategy=in-process`）。
- テスト名は日本語で振る舞いを書く。ktlint の関数命名規則はテストソースのみ無効化する（`.editorconfig`）。
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
