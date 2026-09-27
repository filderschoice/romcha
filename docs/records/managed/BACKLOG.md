<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- id: BL-063
  区分: 機能追加
  タスク内容: >-
    リリースビルドを 1 コマンドで行うスクリプト（scripts/release-build.bat と scripts/release-build.ps1）を作る。
    sesami-wear の同名スクリプトを参考に、署名情報（local.properties）の事前確認、版の指定（-VersionName で versionName と
    versionCode を更新）、品質ゲート、:app:releaseDist、apksigner による署名の確認、次の手順（タグ・公開）の表示までを行う
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    スクリプトで署名済みの romcha-vX.Y.Z.apk と SHA-256 が出力され、署名情報が無い・版の形式が違う・ビルド失敗の各場合に
    分かる説明で止まる（ビルド失敗時は版の変更を戻す）。docs/RELEASE.md と README に使い方を記載する
  依存: []
  根拠: >-
    2026-09-27 ユーザー指示。参考の version.properties 方式は採らず、versionCode は RELEASE.md 2章の算出式で versionName から
    求める（版の正本を app/build.gradle.kts の 1 か所に保つため）。スクリプトは署名情報の値を表示しない。
- id: BL-061
  区分: 人手検証
  タスク内容: >-
    ポートフォリオへ載せる実機のスクリーンショットを撮影・選定し（配信者名・チャット本文など第三者の情報が写らないか確認）、
    site/ の画面イメージと差し替える。あわせて公開方法（GitHub Pages 等）を決める
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: 差し替える画像が site/assets/ に置かれ、公開方法が決まっている（差し替え作業自体はエージェントへ依頼してよい）
  依存: []
- id: BL-059
  区分: 人手検証
  タスク内容: >-
    GitHub のリポジトリ設定（Settings の Advanced Security / Code security）で Dependency graph と Dependabot alerts を有効にし、
    Insights の Dependency graph に Gradle の依存（gradle/libs.versions.toml の OkHttp・Compose 等）が表示されるかを確かめる
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: >-
    Dependabot alerts が有効で、Gradle の依存が検出されている。検出されない場合は、dependency submission（GitHub Actions の追加。
    CI 定義の変更のため人が判断）を検討するタスクを起票する
  依存: []
  根拠: >-
    2026-09-27 ユーザー判断で、脆弱性チェックの代替手段は Dependabot アラートとした（BL-057 を閉じて切り出し）。
    ローカルの品質ゲートには入れない。Gradle の version catalog がどこまで静的に検出されるかは未確認のため、有効化後に確かめる。
- id: BL-058
  区分: 人手検証
  タスク内容: >-
    リリース用キーストアの作成とバックアップ、署名 APK のビルドと実機での上書きインストール・更新確認、
    v1.0.0 タグの作成と push、GitHub Releases への APK と SHA-256 の公開（docs/RELEASE.md の手順）
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: GitHub Releases に署名済み romcha-v1.0.0.apk と SHA-256 が公開され、アプリの「更新を確認」が最新と判定する
  依存: []
- id: BL-024
  区分: 人手検証
  タスク内容: >-
    実機でプレミア公開（待機中・公開中）と通常ライブのチャット追従、終了時のリプレイ切り替えを確認する。
    2026-09-27 に D1〜D5（最新追従・遅延設定・一時停止・プレミア待機中）を確認済み。残る D6 は docs/VERIFICATION.md 1 章。
  優先度: P2
  状態: 進行中
  担当: ユーザー
  完了条件: >-
    Pixel 8 Pro でライブ・プレミアのチャットが最新追従表示され、終了後にリプレイへ切り替えられる。
    終了後のリプレイ切り替え（D6）は未実施のため実機確認待ち（未確認）
  依存: []
```

<!-- COPILOT_RECORDS:END -->
