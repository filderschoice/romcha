<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
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
- id: BL-092
  区分: 品質ゲート
  タスク内容: >-
    クラッシュ情報の送信（BL-088・BL-089）を含む版を公開する時に、site/index.html のプライバシーと注意（現在の「通信先は YouTube と GitHub API
    だけ」「解析・広告の SDK はありません」）を、README と同じ収集項目・送信先・既定オン・オフにする手順へ改める。README の「状態」欄と
    DESIGN の配布と版、site/app.json の version も新しい版へ合わせる。配布中の版に無い機能を site に載せない方針（BL-085）のため、
    公開する版が決まるまで着手しない（BL-091 の実機確認は 2026-10-05 に完了）
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: site のプライバシー欄が README・DESIGN と一致し、markdownlint と validate-records が成功している
  依存: []
  根拠: >-
    クラッシュ情報の送信は未公開の機能で、公開済みの site に先に載せると配布中の版と食い違うため、公開する版が決まるまで保留する（BL-091 の実機確認は完了済み）。
- id: BL-086
  区分: 人手検証
  タスク内容: >-
    GitHub の統計（版別 DL 数・Traffic の閲覧数とクローン数・参照元・人気ページ・Star と Fork）を、複数リポジトリ分まとめて日次で蓄積する
    非公開リポジトリ（app-pulse）を導入する。本リポジトリ側の変更は不要で、ワークフローも秘密情報も置かない。
    計画書は app-pulse の docs/PLAN.md（本リポジトリの作業では作成しない）。作業は、非公開リポジトリの作成、細粒度 PAT の作成と
    Actions シークレットへの登録（対象リポジトリに romcha を含める。読み取り専用・期限付き）、日次ワークフローの動作確認、
    対象の追加（Sesami-wear など）。Traffic API は直近 14 日分しか保持されないため、導入が遅れた期間の分は取り戻せない。
    PAT に必要な権限（Traffic 取得に Administration の読み取りが要るか）は未確認で、最初に 1 リポジトリで実測して確定する。
    Pages や README への反映は自動化せず、必要なときに開発者が要約を手動で追記する
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: app-pulse が日次で本リポジトリの統計を CSV へ蓄積し、2 回連続の手動実行で行が重複せず、失敗時に通知が届くことを確認している
  依存: []
  根拠: >-
    リポジトリの作成と PAT の取り扱い（秘密情報）はエージェントが行えず、本リポジトリの作業範囲外のため、区分を人手検証にして
    自律ループの完了判定から除外する。
```

<!-- COPILOT_RECORDS:END -->
