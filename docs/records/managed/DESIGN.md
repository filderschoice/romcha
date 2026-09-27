<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用設計記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.design.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

# 実装用プロンプト設計書（Romcha）

## 目的

- YouTube 公式アプリと並べて使う、閲覧専用のフローティングチャットビューワー「Romcha」を、
  同一要件で AI エージェントに再実装させるための設計書。
- 要件の原本は `docs/PLAN.md`。本書は実装済みの内容と、実装上の決定事項を統合したもの。

## 対象システム概要

- 対象: Android アプリ（applicationId `io.github.filderschoice.romcha`、ライセンス MIT）
- 前提環境: minSdk 34 / targetSdk 36 / compileSdk 36、JDK 17、Gradle 8.13、AGP 8.13.0、Kotlin 2.0.21
- 動作確認端末: Pixel 8 Pro（実機確認は人手検証）

## 実装済み機能要件

- プロジェクト雛形と品質ゲート（静的解析・型検査・単体テスト）
- F-CHAT-01/08、N-08: メッセージモデルとチャット応答の解析（`core:chat` の `ChatResponseParser`）
- F-CHAT-10、F-VID-07: InnerTube クライアント（`next` からの continuation 取得・チャット無効の判定、リプレイ／ライブ取得、指数バックオフ）
- F-VID-04/05: YouTube URL からの動画ID抽出（`core:chat` の `VideoUrlParser`）
- F-VID-01/02: 動画の自動特定パイプライン手順1・2・4（`core:chat` の `resolve.VideoResolver`）
- F-SYNC-01/02: 公式アプリの MediaSession からの再生状態・メタデータ取得（`core:media` の `PlaybackMonitor`）
- F-SYNC-03/04/05: 位置推定・一時停止・シーク判定・速度追従（`core:sync` の `SyncEngine`）
- F-CHAT-02/03: リプレイの先読み取得とシーク時の再取得（`core:sync` の `ReplaySession`）

## 設計方針

### モジュール構成

| モジュール | 種別 | 役割 |
| --- | --- | --- |
| `app` | Android アプリ | 起動・権限案内・設定画面・共有インテント受信 |
| `core:chat` | Kotlin/JVM | InnerTube クライアント・応答解析・メッセージモデル（非公式 API 依存をここへ閉じ込める。N-09） |
| `core:sync` | Kotlin/JVM | SyncEngine（位置推定・シーク判定・バッファ管理）。Android 非依存 |
| `core:media` | Android ライブラリ | PlaybackMonitor（NotificationListener + MediaSession） |
| `feature:overlay` | Android ライブラリ | OverlayService・フローティング UI（Compose） |

- 依存の向き: `app` → `feature:overlay` / `core:media` → `core:sync` → `core:chat`。
- 依存バージョンは `gradle/libs.versions.toml` に集約する。

### チャット応答の解析（`core:chat`）

- モデル: `ChatMessage`（id・種別・投稿者・本文 runs・投稿時刻 μs・動画内オフセット ms（リプレイのみ）・金額と色・見出し）。
  種別は TEXT / SUPER_CHAT / SUPER_STICKER / MEMBERSHIP / GIFT_PURCHASE / GIFT_REDEMPTION。
  投稿者の役割は OWNER / MODERATOR / MEMBER（バッジに customThumbnail）/ VERIFIED。
  本文は `MessageRun.Text` と `MessageRun.Emoji`（カスタム絵文字はショートカット、標準絵文字は絵文字そのものを代替テキストに）。
- 解析対象の応答: `continuationContents.liveChatContinuation`。リプレイは `replayChatItemAction`（`videoOffsetTimeMsec`）の中の
  `addChatItemAction.item`、ライブは直下の `addChatItemAction.item`。項目は renderer 名で種別を判定する。
  - `liveChatTextMessageRenderer` / `liveChatPaidMessageRenderer` / `liveChatPaidStickerRenderer` /
    `liveChatMembershipItemRenderer` / `liveChatSponsorshipsGiftPurchaseAnnouncementRenderer`（投稿者は `header` 内）/
    `liveChatSponsorshipsGiftRedemptionAnnouncementRenderer`
  - 色（`bodyBackgroundColor` / `headerBackgroundColor` / `backgroundColor`）は符号なし整数の ARGB を Int へ変換する。
  - それ以外の renderer と id の無い項目は読み飛ばして `skipped` に数える。チャット項目以外のアクション（ティッカー等）は数えない。
- 継続トークン: `continuations` から `liveChatReplayContinuationData` → `timedContinuationData` → `invalidationContinuationData`
  → `reloadContinuationData` の順で最初に見つかったもの。`timeoutMs` を推奨間隔として持つ。無ければ終端・終了。
- JSON は `kotlinx.serialization` の `JsonElement` を必要箇所だけ辿る（`internal/JsonNav.kt`）。型不一致・欠落は null とし、
  `liveChatContinuation` が無い・JSON でない場合は `ChatParseResult.Failure` を返す（例外を投げない）。
- `InnerTubeClient`（通信先は `https://www.youtube.com/` のみ。Cookie を保持しない）:
  - 共通: `POST youtubei/v1/<path>?prettyPrint=false`、本文に `context.client`（`clientName=WEB`・`clientVersion`・`hl=ja`・`gl=JP`）。
    `clientVersion` の既定値は定数で持ち、実応答での有効性は人手検証で確認する。
  - `fetchVideoChatInfo(videoId)`: `next` 応答の `twoColumnWatchNextResults` から、タイトル（`videoPrimaryInfoRenderer.title`）、
    チャンネル名（`videoSecondaryInfoRenderer.owner.videoOwnerRenderer.title`）、`conversationBar.liveChatRenderer` の
    `isReplay` と continuation（見出しの切り替えメニュー 0 = 上位チャット、1 = すべてのチャット。無ければ `reloadContinuationData`）を読む。
    `liveChatRenderer` が無ければ `VideoChatInfo.Unavailable`（`conversationBarRenderer.availabilityMessage` の説明文付き）。
  - `fetchReplay(continuation, playerOffsetMs)`: `live_chat/get_live_chat_replay`。初回・シーク後のみ
    `currentPlayerState.playerOffsetMs`（文字列）を付ける。`fetchLive(continuation)`: `live_chat/get_live_chat`。
  - 結果は `FetchResult`（成功／`FetchFailure.Network`・`Http(code)`・`Parse(reason)`）。
  - 再試行（F-CHAT-10）: 通信断・429・5xx のみ。待ち時間 1 秒から倍々で最大 30 秒、既定 5 回。`RetryListener` で UI へ通知する。
    解析失敗と 4xx（429 以外）は再試行しない。
- `VideoUrlParser.extractVideoId(text)`: 文中の最初の YouTube 動画 URL から 11 桁の動画IDを取り出す。
  受理するのは `youtu.be/ID`、`(www.|m.)youtube.com/watch?v=ID`、`/live/ID`、`/shorts/ID`（http/https・スキーム省略可）。
  ホストは完全一致で判定し、ID は `[A-Za-z0-9_-]{11}` のみ受理する。ID 単体の入力は受理しない。
- `resolve.VideoResolver.resolve(TrackMetadata)`（PLAN 4.3）: 次の順に試し、確定した時点で打ち切る。
  1. `videoIdHints` の先頭 → `Confirmed(METADATA)`（キャッシュにも登録）
  2. `ResolutionCache`（キーは `TrackMetadata.identity`）→ `Confirmed(CACHE)`
  3. （手順3 のライブ・プレミア照合は M2 で追加）
  4. `InnerTubeClient.search("タイトル チャンネル名")` の結果を採点。タイトル完全一致 50・部分一致 25、チャンネル名一致 30、
     長さ ±2 秒一致 20。比較は NFKC 正規化・小文字化・空白除去後。1 位が 80 点以上かつ 2 位との差 10 点以上なら
     `Confirmed(SEARCH, alternatives=他の候補)`、それ以外で 1 点以上の候補があれば `Ambiguous`（上位 5 件）、無ければ `NotFound`。
     通信失敗は `Failed`。
  - ユーザーが候補を選んだら `remember()` でキャッシュへ登録する（`ResolutionSource.USER`）。
  - `InMemoryResolutionCache`: 最大 200 件の LRU。`snapshot()` で永続化用に取り出せる（端末内にのみ保存。N-06）。
- `resolve.SearchResultParser`: `twoColumnSearchResultsRenderer…sectionListRenderer.contents[].itemSectionRenderer.contents[].videoRenderer`
  から videoId・タイトル・チャンネル名（`ownerText` → `longBylineText` → `shortBylineText`）・長さ（`lengthText` の `h:mm:ss`）・
  ライブ表示（`BADGE_STYLE_TYPE_LIVE_NOW`）を読む。
- テストの fixture（`core/chat/src/test/resources/fixtures/`）は既知の応答構造に基づく合成データ。実応答との照合は人手検証。

### 再生状態の取得（`core:media`）

- `MediaListenerService`（`NotificationListenerService`）は「通知へのアクセス」の許可を受けるためだけに置き、通知内容は読まない。
  manifest は `exported=true`・`permission=BIND_NOTIFICATION_LISTENER_SERVICE`。
- `PlaybackMonitor`: `MediaSessionManager.getActiveSessions(リスナーのコンポーネント)` と `addOnActiveSessionsChangedListener` で
  パッケージ `com.google.android.youtube` の `MediaController` を追跡し、`MediaController.Callback` で状態・メタデータの変化を
  `state: StateFlow<NowPlaying>`（スナップショット・`TrackMetadata`・セッション有無・デバッグ用の全キー一覧）へ反映する。
  コールバックはメインスレッド。許可が無い・`SecurityException` の場合は開始しない。
- `MediaMapping`（純粋関数。JVM テスト可能）:
  - 状態: PLAYING / FAST_FORWARDING / REWINDING → PLAYING、PAUSED → PAUSED、BUFFERING / CONNECTING / SKIPPING_* → BUFFERING、
    STOPPED / ERROR → STOPPED、その他 → NONE。速度が 0 以下なら 1.0 とみなす。負の位置は 0。
  - `TrackMetadata`（`core:chat` の `resolve` パッケージ）: タイトル（無ければメタデータ無し）、ARTIST をチャンネル名、DURATION を長さ、
    `videoIdHints`。`identity`（タイトル・チャンネル名・長さ）の変化で動画の切り替えとみなす（F-VID-03）。
  - `videoIdHints`（PLAN 4.3 手順1）: MediaMetadata の全キー、MediaDescription の mediaId / mediaUri / extras、controller と
    PlaybackState の extras、キューの mediaId / mediaUri を集め、値に YouTube URL があればその ID、キー名の末尾要素に `id` を含み
    値が 11 桁 ID 形式ならその値を候補にする。
  - デバッグ一覧（Q-02 の実機確認用）は画面表示のみで、ログへ出さない（N-07）。

### 同期エンジン（`core:sync`）

- `PlaybackSnapshot`（状態・位置・位置の報告時刻・速度）を入力とし、
  `推定位置 = position + (now - updatedAt) × speed`（再生中のみ加算）で現在位置を求める（`PositionEstimator`）。
- `SeekDetector`: 前回推定位置＋経過時間×前回速度 を期待値とし、±2 秒を超えたらシーク。初回は false。
- `SyncEngine<T>` はメッセージ型に依存しない（オフセットとキーの取り出し関数を受け取る）。通信はせず、
  `tick()`（250ms 間隔）の戻り値 `SyncFrame.fetchRequest` で取得を要求し、結果を `onFetched` / `onFetchFailed` で受ける。
  - 初回・シーク時: 表示をクリアし、`推定位置 - 30 秒` から `restart = true` で取り直す。世代番号を進め、古い世代の応答は捨てる。
  - 先読み: 取得済み範囲の終端が `推定位置 + 60 秒` 未満なら続きを要求する。一時停止中は先読みしない（N-03）。
    応答待ちの間は重ねて要求しない。`hasMore = false` で終端とみなし要求を止める。
  - 表示: 未表示バッファ（オフセット昇順）から推定位置以下のものを表示へ移す。表示位置より前に遅れて届いたものは時刻順に差し込む。
    キーで重複を除く。表示保持は既定 500 件で、超えたら古いものから捨てる（N-04）。
  - `changed = false` の時は `visible` を空で返し、呼び出し側は前回表示を維持する（毎回のリストコピーを避ける）。
  - スレッドセーフではない。単一のコルーチン（単一スレッド）から呼ぶ。
- `ReplaySession`: `SyncEngine` と取得元 `ReplayChatSource`（本番は `InnerTubeClient.fetchReplay`）をつなぐ。
  - `run()` はキャンセルまで 250ms ごとに tick し、`state: StateFlow<ReplayState>`（位置・再生状態・表示メッセージ・取得状態・終端）を更新する。
    単一スレッドのディスパッチャーで実行する（取得結果の反映も同じスレッドで行うため）。
  - 取り直し（初回・シーク）は `initialContinuation` と `playerOffsetMs = 推定位置 - 30 秒` で取得し、実行中の取得を取り消す。
    続きは直前の応答の継続トークンで、位置を指定せずに取得する。継続トークンが無ければ終端（`ended`）。
  - 取得済み範囲の終端は応答内の最大オフセット。空の応答は要求位置 + 10 秒を取得済みとみなす。
  - 通信量の抑制（K-04）: 続きの取得は最小 1 秒間隔。再試行しても失敗した後は 10 秒の冷却期間を置く（取り直しも含む）。
  - 取得状態 `FetchStatus`（Idle / Loading / Retrying(attempt) / Failed(failure)）を UI へ出す（F-CHAT-10）。
  - 間隔は `SessionTiming`（tick 250ms・冷却 10 秒・最小間隔 1 秒）でまとめて渡す。

## 非機能要件

- PLAN.md 3章（N-01〜N-11）に従う。
- バックアップ: 設定・履歴は端末内のみ（N-06）。`data_extraction_rules.xml` でクラウドバックアップと端末間移行から除外し、
  `allowBackup=false` とする。

## 実装制約

- ビルド環境のメモリ: 開発機（8GB）に合わせ `org.gradle.jvmargs=-Xmx2g`、`workers.max=2`、
  Kotlin コンパイラは Gradle デーモン内で実行する（`kotlin.compiler.execution.strategy=in-process`）。
- detekt の `ReturnCount` はガード節を数えず上限 3 とする（JSON 解析の `?: return null` が多いため）。
- テスト名は日本語で振る舞いを書く。ktlint の関数命名規則はテストソースのみ無効化する（`.editorconfig`）。
- 静的解析: ktlint（`ktlint_official`、`@Composable` 関数は命名規則の対象外）、detekt（既定設定＋`config/detekt/detekt.yml` の差分）、
  Android lint（`warningsAsErrors = true`。依存の新版警告のみ `lint.xml` で無効化）。
- 署名鍵（`*.jks` / `*.keystore` / `keystore.properties`）は `.gitignore` で除外する。
- アプリ名・アイコンに YouTube のロゴ・名称を使わない（PLAN 5.5）。

## エージェント実装指示

- 品質ゲートは `CLAUDE.md`「本リポジトリの品質ゲート定義」のコマンドを使う。
- 外部 API（YouTube・GitHub）への実通信はテストで行わない。MockWebServer と保存済み JSON fixture を使う（guardrails 12.5）。
- 参考にした外部実装は `docs/REFERENCES.md` へ記録し（現状は chat-downloader・pytchat・yt-dlp の方式のみ）、GPL・ライセンス無しのコードは流用しない（PLAN 5章）。
- 要件トレーサビリティ: 実装時は PLAN.md の要件ID（F-*/N-*）をコミット・EXECUTE.md の変更内容へ記載する。

<!-- COPILOT_RECORDS:END -->
