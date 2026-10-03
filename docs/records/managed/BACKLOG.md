<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- id: BL-076
  区分: 品質ゲート
  タスク内容: >-
    Dependabot alerts が Gradle の依存を検出できるよう、dependency submission（GitHub Actions のワークフローで Gradle の依存グラフを
    GitHub へ提出する）を導入するかを決める。2026-10-04 時点で、Dependency graph の SBOM にはリポジトリ自身の 1 件だけが出ており、
    gradle/libs.versions.toml の依存（OkHttp・Compose 等）は検出されていない（Dependabot alerts 自体は有効化済み）。
    導入する場合は .github/workflows にワークフローを追加する（エージェントが作成し、人がレビューして取り込む）
  優先度: P3
  状態: 要確認
  担当: ユーザー
  完了条件: 導入する・しないの方針が決まり、導入する場合は Gradle の依存が Dependency graph に表示される
  依存: []
  根拠: >-
    CI 定義の変更は自律ループの禁止範囲（guardrails 12.2）で、本リポジトリには CI が無く新設になるため、既定値を選ばず要確認として保留する。
    BL-059（Dependabot alerts の有効化と検出の確認）は、有効化の完了と未検出の確認をもって閉じ、残りを本タスクへ切り出した。
```

<!-- COPILOT_RECORDS:END -->
