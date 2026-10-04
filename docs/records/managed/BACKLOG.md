<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- id: BL-091
  区分: 人手検証
  タスク内容: >-
    Crashlytics を実際に動かすための Firebase 側の準備と実機確認を行う。Firebase プロジェクトの作成、Android アプリ
    （io.github.filderschoice.romcha）の登録、google-services.json の取得とリポジトリのルートでなく app/ へのローカル配置（コミットしない）、
    リリース署名ビルドでのテストクラッシュ送信と Firebase コンソールでの受信確認、設定のオフ時に送信されないことの確認。
    Analytics は有効化しない（Crashlytics のみ）。Firebase の課金プランは無料の Spark のままにする
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: テストクラッシュが Firebase コンソールに届き、設定オフでは届かないことを確認している
  依存: []
  根拠: >-
    Firebase プロジェクトの作成と google-services.json の取得はアカウント操作と秘密情報の取り扱いを伴い、エージェントが行えないため、
    区分を人手検証にして自律ループの完了判定から除外する。
- id: BL-092
  区分: 品質ゲート
  タスク内容: >-
    クラッシュ情報の送信（BL-088・BL-089）を含む版を公開する時に、site/index.html のプライバシーと注意（現在の「通信先は YouTube と GitHub API
    だけ」「解析・広告の SDK はありません」）を、README と同じ収集項目・送信先・既定オン・オフにする手順へ改める。README の「状態」欄と
    DESIGN の配布と版、site/app.json の version も新しい版へ合わせる。配布中の版に無い機能を site に載せない方針（BL-085）のため、
    公開する版が決まるまで着手しない
  優先度: P2
  状態: ブロック
  担当: AIエージェント
  完了条件: site のプライバシー欄が README・DESIGN と一致し、markdownlint と validate-records が成功している
  依存: [BL-091]
  根拠: >-
    クラッシュ情報の送信は未公開の機能で、公開済みの site に先に載せると配布中の版と食い違うため、BL-091 の実機確認後の公開時まで保留する。
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
