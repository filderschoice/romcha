# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## 2026-09-27

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
