# 参考元の記録（REFERENCES）

PLAN.md 5.3 に基づき、機能の実現方法を参考にした外部実装を 1 件ずつ記録します。
参考範囲の区分は PLAN.md 5.2 に従います（動作／方式／コード流用）。コードを流用・移植した場合は、
対象ファイルの先頭に元のリポジトリ・コミット・ライセンスを記載し、`THIRD_PARTY_NOTICES.md` へライセンス文を追加します。

現時点でコードの流用・移植はありません（すべて「方式」の参考で、Kotlin で独自に実装しています）。

| 名称 | URL | ライセンス（確認結果） | 参考範囲 | 参考にした機能要件ID | 確認日 |
| --- | --- | --- | --- | --- | --- |
| chat-downloader | <https://github.com/xenova/chat-downloader> | MIT（GitHub API で確認） | 方式: InnerTube のチャット取得手順（`next` 応答からの continuation 取得、`get_live_chat_replay` / `get_live_chat` の呼び方、応答内のメッセージ種別と項目名） | F-CHAT-01, F-CHAT-04, F-CHAT-08 | 2026-09-27 |
| pytchat | <https://github.com/taizan-hokuto/pytchat> | MIT（GitHub API で確認） | 方式: リプレイの `playerOffsetMs` 指定、ライブの `timeoutMs` に従うポーリング | F-CHAT-01〜04 | 2026-09-27 |
| yt-dlp | <https://github.com/yt-dlp/yt-dlp> | Unlicense（GitHub API で確認） | 方式: InnerTube クライアント情報（`clientName` / `clientVersion`）の送り方 | F-CHAT-01 | 2026-09-27 |

補足:

- 上記の「方式」は、実装したエージェントが持っていた一般的な知識（公開 OSS で広く知られた通信手順と応答構造）に
  基づくもので、実装時に各リポジトリのソースコードを参照・転記していません。
- 応答構造の実物との一致は、実通信を伴うため人手検証（BACKLOG BL-022）で確認します。
