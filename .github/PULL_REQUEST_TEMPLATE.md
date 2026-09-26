<!-- markdownlint-disable-file MD041 -->
<!-- I want to review and to create summaries in Japanese. -->
<!-- for GitHub Copilot review rule -->
<!--
本文の書き方とチェックリストの扱いは .github/instructions/pr.instructions.md を参照してください。
実行していない検証はチェックせず、対象外の項目は理由を添えて未チェックのまま残します。
-->

## 概要

<!-- 変更内容の簡潔な説明 -->

## 変更内容

<!-- 箇条書きで主な変更点を列挙。BACKLOG.mdの対象タスクがある場合はidを記載 -->

## 変更理由・背景

<!-- なぜこの変更が必要か -->

## テスト方法

<!-- 実行した品質ゲートのコマンドと結果。実機・実環境で確認した場合はその環境も記載 -->

## 関連事項

<!-- 関連するIssue番号、参考リンクなど -->

## チェックリスト

- [ ] `CLAUDE.md`「本リポジトリの品質ゲート定義」のうち、変更に該当するゲートをローカルで実行し、すべて成功した
- [ ] 秘密情報（トークン、鍵、資格情報）や個人情報が差分へ混入していないことを確認した
- [ ] コード修正を伴う場合、`docs/records/managed/EXECUTE.md` を更新した
- [ ] ルール・ドキュメントの変更の場合、`CHANGELOG.md` を更新した
- [ ] 記録ファイル（`docs/records/managed/`）を変更した場合、`python scripts/validate-records.py` が成功した
