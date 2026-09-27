# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## 2026-09-27

- `docs/records/managed/BACKLOG.md` へ PLAN.md（M0〜M5）をタスク分解して登録した（自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` の先頭へ `markdownlint-disable-file MD041` を追加し、DESIGN.md / EXECUTE.md と体裁を揃えた（ユーザー承認済み）。
- `CLAUDE.md`「本リポジトリの品質ゲート定義」の `replace-me` を Gradle のコマンド（ktlint・detekt・Android lint、
  compileDebugKotlin、単体テスト）へ置き換えた。脆弱性チェックはコストに見合わないため導入しない（ユーザー判断）。
  導入完了に伴い、同節の記入方法コメントを削除した。
- `README.md` を追加した（概要・免責・使い方と権限の用途・動作環境・ビルドと品質ゲート・構成・プライバシー・ライセンス）。
