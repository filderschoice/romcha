# CLAUDE.md

Claude Code 固有の差分と追加規約です。全エージェント共通規約の正本（`.github/copilot-instructions.md`）と
ガードレールは下記の `@import` で同時に読み込まれます。共通規約と矛盾する場合は本ファイルを優先します。
その他の参照先と読むタイミングは、共通規約「参照するドキュメント」を参照してください。

@rules/guardrails-unified.v1.md
@.github/copilot-instructions.md

## 共通規約に対する Claude Code 固有の差分（MUST）

| 項目 | Claude Code での差分 |
| --- | --- |
| git操作 | 自律ループ実行モード中の作業ブランチへの `git add` / `git commit` のみ実行してよい。`git push` はモードを問わず常にユーザーが実行する |
| 指示参照の優先順位 1位 | Claude Code ハーネスのシステムプロンプト |
| PR説明文・コードレビュー・PR作成 | `.github/instructions/pr.instructions.md` を自動適用する機構が無いため、Skill `pr-create` の手順で同ファイルを**明示的に読んでから**従う |
| 記録ファイルの編集権限 | `.claude/settings.json` の `permissions.allow` により権限プロンプトなしで反映される（下記） |

## 本リポジトリの品質ゲート定義（MUST）

全エージェント共通で使用する正本です。自律ループ実行モードでは各イテレーションで実行し、記録へ残します。
`区分: 人手検証` のBACKLOGタスクは合否判定から除外します。
Windows の PowerShell では `./gradlew` を `.\gradlew.bat` と読み替えます。

| ゲート | コマンド | 合否基準 |
| --- | --- | --- |
| フォーマット/静的解析 | `./gradlew ktlintCheck detekt lintDebug` | 終了コード0、指摘0件（Android lint は `warningsAsErrors`） |
| 型検査 | `./gradlew compileDebugKotlin` | 終了コード0 |
| 単体テスト | `./gradlew testDebugUnitTest :core:chat:test :core:sync:test` | 全件成功 |
| 脆弱性チェック | `(対象外)` | ローカルのゲートには導入しない判断のため（下記） |
| Markdown の静的解析 | `npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml` | `Summary: 0 issues`、かつ出力の `Linting: N files` が `git ls-files --cached --others --exclude-standard "*.md"` の件数と一致（対象漏れの検出） |
| 記録ファイルの検証 | `python scripts/validate-records.py`（PyYAMLが必要） | 終了コード0 |

脆弱性チェックはローカルの品質ゲートに導入しません（2026-09-27 ユーザー判断）。OWASP Dependency-Check は NVD API キー（秘密情報で
エージェントは扱えない）が無いと脆弱性DBの取得に長時間かかり、コストに見合わないためです。
代替として GitHub の Dependabot アラートを使います（2026-09-27 ユーザー判断。リポジトリ設定での有効化は人が行い、
Gradle の依存が検出されるかは BACKLOG の人手検証で確かめます）。

## 記録ファイルの権限設定（MUST）

`docs/records/managed/` 配下の3ファイルは、`.claude/settings.json` の `permissions.allow` により権限プロンプト
なしで編集できます（人手編集を前提とせず、妥当性はコミット前の差分確認と `FORMAT.md` 準拠で担保するため）。
ルールファイル（`CLAUDE.md` / `rules/` / `CONTRIBUTING.md`）・`.github/` 配下・`docs/records/spec/FORMAT.md` の
編集は確認を挟みます。自律ループ実行モードでは、各イテレーションの完了を記録ファイルの更新契機とします。

<!-- 配布バンドルの .claude/settings.json は記録ファイル関連の許可のみを含む。本リポジトリ固有の許可を追加する
場合は、確認を挟まずに編集させてよい範囲かを判断してから permissions.allow へ追記する。 -->

## 自律ループ実行モード（Loop Engineering）

人の応答を待たずに複数イテレーションを連続実行する、Claude Code 固有の運用モードです（Copilot は対象外）。

<!-- 本モードを採用しない場合は、本節と .claude/skills/autonomous-loop/ を削除する
（採否の判断は docs/guidelines/ADOPTION.md「1.2 自律ループ実行モードの採否」）。 -->

- **適用条件（MUST）**: ユーザーが開始を明示的に指示していること。指示されたスコープ内でのみ有効で、
  ループ終了と同時に通常の対話モードへ戻る。満たさない場合は `git add` / `git commit` を実行してはならない
- **起動方法（MUST）**: Skill `autonomous-loop` を起動し、その手順に従う。手順を記憶や推測で代用しない
- 統制要件（禁止操作・停止条件・秘密情報の取り扱い）は常時読み込みの `rules/guardrails-unified.v1.md`
  セクション12、ブランチ・コミット規約は `CONTRIBUTING.md`「自律ループ実行モードのブランチ・コミット規約」が正本

## 保守

共通規約の変更は `.github/copilot-instructions.md` へ、Claude Code 固有の変更は本ファイルへ反映します
（役割分担は `CONTRIBUTING.md`「エージェント指示ファイルの構成規約」）。

<!-- 本ファイルは配布元の templates/CLAUDE-template.md を基にしている。配布元の更新へ追従する場合は、
CHANGELOG.md で差分を確認し、本リポジトリ固有に書き換えた節（品質ゲート定義など）を上書きしないようマージする。 -->
