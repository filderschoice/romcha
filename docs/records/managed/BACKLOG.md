<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- id: BL-096
  区分: 人手検証
  タスク内容: >-
    設定の復元を実機で確認する（bmgr とローカル転送先での確認は 2026-10-08 に済み。残りは Google アカウントのバックアップと
    アンインストール・再インストールでの自動復元）。バックアップを有効にした端末でウィンドウの位置・不透明度・NG ワードなどを変えてから
    `adb shell bmgr backupnow io.github.filderschoice.romcha` でバックアップし、アンインストール後に再インストールして復元されることを見る
    （または `adb shell bmgr restore` を使う）。動画の特定結果のキャッシュが復元されないことも確かめる
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: >-
    再インストール後に設定が復元され、キャッシュは空から始まる。「設定のバックアップ」をオフにしてバックアップした場合は復元されない。
    「設定を初期化する」でウィンドウと表示の設定が初期値に戻る
  依存: []
  根拠: >-
    バックアップの実行と復元は Google アカウントに紐づく端末の状態に依存し、エージェントが扱えないため、区分を人手検証にして
    自律ループの完了判定から除外する。
- id: BL-093
  区分: 人手検証
  タスク内容: >-
    dependency submission のワークフロー（.github/workflows/dependency-submission.yml。BL-076 で作成）をレビューして main へ取り込み、
    Actions の実行が成功して Gradle の依存（OkHttp・Compose 等）が Dependency graph に表示されることを確認する。
    失敗する場合は Actions のログを基にワークフローを直す
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: Dependency graph の SBOM に gradle/libs.versions.toml の依存が表示されている
  依存: []
  根拠: >-
    ワークフローの取り込み（push・PR のマージ）と GitHub 上での実行確認は人が行うため、区分を人手検証にして自律ループの完了判定から除外する。
```

<!-- COPILOT_RECORDS:END -->
