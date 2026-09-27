# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## 2026-09-28

- `README.md` に「問題の報告」節（GitHub Issues の窓口、報告に書く情報、個人情報を書かないお願い）を追加した（BL-068）。
- `.github/CODEOWNERS` の配布テンプレートのプレースホルダ（`@your-org/...`）を、リポジトリ所有者（`@filderschoice`）へ置き換えた（BL-067）。
- `docs/VERIFICATION.md` から検証端末のシリアル番号を除き、`<シリアル>`（`adb devices -l` で確認する値）へ置き換えた（BL-066）。
- リポジトリの公開（public 化）に向けた点検の指摘を `docs/records/managed/BACKLOG.md` へ登録した（BL-066〜BL-070。
  検証端末のシリアル番号・CODEOWNERS のプレースホルダ・README の問い合わせ窓口・公開の操作・履歴に残るシリアルの扱い。自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、v1.0.0 の GitHub Releases 公開をドキュメントへ反映するタスク（BL-064）と、
  Releases の公開を誰が行うかの規定の見直し（BL-065。要確認）を登録した（自律ループ第1イテレーション）。
- v1.0.0 の GitHub Release 作成を `README.md` の状態表・DESIGN・BACKLOG（BL-058 を残りの確認へ絞った）・`site/app.json`・`docs/RELEASE.md` 6章へ反映した（BL-064）。
  リポジトリが非公開の間は「更新を確認」が「公開されている版はまだありません。」になること（API が 404 を返す）を記載した。

## 2026-09-27

- `docs/records/managed/BACKLOG.md` へ、リリースビルドを簡易化するスクリプトの作成（BL-063。ユーザー指示）を登録した
  （自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、リリース署名の情報を `local.properties` から読むよう変える修正（BL-062。ユーザー指示）を
  登録した（自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、ユーザー指示のアプリ紹介ポートフォリオ（BL-060）と、その実機画像の差し替え・公開方法の
  決定（人手検証 BL-061）を登録した（自律ループ第1イテレーション）。
- 脆弱性チェックの代替手段を GitHub の Dependabot アラートとした（ユーザー判断）。`CLAUDE.md`「本リポジトリの品質ゲート定義」の
  説明と PLAN 9章を更新し、BL-057 を閉じて、リポジトリ設定での有効化と検出範囲の確認を人手検証 BL-059 として登録した。
- `docs/records/managed/BACKLOG.md` の BL-027（M5 配布）を、ユーザー回答（コード整備までを範囲とし、更新確認は手動のみ）に
  基づいて BL-054〜BL-058 へ分割した（自律ループ第1イテレーション）。
- 実機検証 BL-053（`docs/VERIFICATION.md` の G1。フローティングからアプリ本体を開く）が OK だったため完了とし、
  BACKLOG と `docs/VERIFICATION.md` から削除した。DESIGN に確認済みを反映した。
- `docs/records/managed/BACKLOG.md` へ、フローティングの設定メニューからアプリ本体を起動する機能（BL-052）と、その人手検証（BL-053）を
  登録した（自律ループ第1イテレーション）。
- 実機検証 BL-048（`docs/VERIFICATION.md` の F1〜F15。M3・M4 と BL-034〜BL-051 の表示・操作）がすべて OK だったため完了とし、
  BACKLOG と `docs/VERIFICATION.md` から削除した。README の状態表から確認済みの「実機確認待ち」を外し、DESIGN に確認済みを反映した。
- `docs/records/managed/BACKLOG.md` へ、実機確認での指摘（退避中のつまみの横幅を広げる）を BL-051 として登録した
  （自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、実機確認での指摘（退避のドラッグが画面幅で止まる）を BL-050 として登録した
  （自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、ユーザー指示のフローティングウィンドウの画面端への退避と復帰（BL-049）を登録し、
  人手検証 BL-048 の確認対象に加えた（自律ループ第1イテレーション）。
- `docs/VERIFICATION.md` から確認済みの項目（BL-021・BL-022・BL-023 の全項目、BL-024 の D1〜D5）を削除し、未確認の
  BL-024 の D6 と BL-048 だけを残した（ユーザー指示）。章番号を詰め、BACKLOG・PLAN の参照を合わせた。
- `README.md` の状態表と使い方を M3・M4 の実装（BL-034〜BL-047）に合わせて更新し、`docs/VERIFICATION.md` に
  BL-048（表示の調整と操作）の確認表（5 章）を追加した。以降の章番号を 1 つずつ繰り下げた。
- `docs/records/managed/BACKLOG.md` へ、ユーザー指示の2件（チャットの文字サイズ、ヘッダーの配色）と M3・M4 を要件単位に分解して登録した
  （BL-034〜BL-048。BL-025 / BL-026 は分割により削除。自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ PLAN.md（M0〜M5）をタスク分解して登録した（自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` の先頭へ `markdownlint-disable-file MD041` を追加し、DESIGN.md / EXECUTE.md と体裁を揃えた（ユーザー承認済み）。
- `CLAUDE.md`「本リポジトリの品質ゲート定義」の `replace-me` を Gradle のコマンド（ktlint・detekt・Android lint、
  compileDebugKotlin、単体テスト）へ置き換えた。脆弱性チェックはコストに見合わないため導入しない（ユーザー判断）。
  導入完了に伴い、同節の記入方法コメントを削除した。
- `README.md` を追加した（概要・免責・使い方と権限の用途・動作環境・ビルドと品質ゲート・構成・プライバシー・ライセンス）。
- `README.md` の状態表と使い方を M2（ライブ・プレミア対応）の実装に合わせて更新した。
- 実機検証手順書 `docs/VERIFICATION.md`（BL-021〜BL-024）を追加し、README からリンクした。各確認表に結果欄と記入値の凡例を設け、
  最新の結果だけを上書きで保持する（履歴は残さない）運用とした（BL-030）。
- 実機検証（`docs/VERIFICATION.md`、Premium 有り／無しの両環境）の結果を反映した。BL-021（MediaSession）と BL-023（リプレイ同期）は
  完了し BACKLOG から削除した。BL-022 は実応答からの fixture の作り直し、BL-024 は D6（終了後のリプレイ切り替え）を残して進行中とした。
  PLAN の Q-01 / Q-02 を決定済みにし、4.2・4.3・K-01 / K-02 / K-05 を検証結果で更新した。
  広告中の扱いを確認する人手検証 BL-031 を起票した。DESIGN に検証結果を追記した。
- 広告中の同期ずれを既知の制約として `README.md` に追記し、動作環境とテストの説明を検証結果に合わせて更新した（BL-031 の決定）。
- 常駐通知の動画タイトルが動画の切り替えに追従することを実機で確認し、BL-033 を完了とした。
