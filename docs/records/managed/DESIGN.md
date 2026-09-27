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
- 動作確認端末: Pixel 8 Pro（実機確認は人手検証。手順と最新の結果は `docs/VERIFICATION.md`）。MediaSession の取得（Q-01。Premium 有り／無し）、
  リプレイ同期（N-02）、ライブ・プレミアの最新追従は確認済み。ライブ終了後のリプレイ切り替えは未確認

## 実装済み機能要件

- プロジェクト雛形と品質ゲート（静的解析・型検査・単体テスト）
- F-CHAT-01/08、N-08: メッセージモデルとチャット応答の解析（`core:chat` の `ChatResponseParser`）
- F-CHAT-10、F-VID-07: InnerTube クライアント（`next` からの continuation 取得・チャット無効の判定、リプレイ／ライブ取得、指数バックオフ）
- F-VID-04/05: YouTube URL からの動画ID抽出（`core:chat` の `VideoUrlParser`）
- F-VID-01/02: 動画の自動特定パイプライン手順1〜4（`core:chat` の `resolve.VideoResolver`）
- F-SYNC-01/02: 公式アプリの MediaSession からの再生状態・メタデータ取得（`core:media` の `PlaybackMonitor`）
- F-OVL-01〜08、F-VIEW-02、F-VID-07: フローティングウィンドウ（`feature:overlay` の `OverlayService`）
- F-APP-01/03/04、F-VID-04/05: アプリ画面（権限案内・共有受信・URL 入力・免責・OSS ライセンス・MediaSession 診断表示）
- F-CHAT-04/05: ライブ・プレミア（公開中・待機中）のチャットのポーリング取得（`core:sync` の `LiveChatSession`）
- F-SYNC-08: ライブ・プレミア中の最新追従表示と表示遅延の設定（`core:sync` の `LiveTimeline`、オーバーレイの設定パネル）
- F-CHAT-06: ライブ・プレミア終了の検知とリプレイへの切り替え待ち（`core:sync` の `LiveChatSession.ended` と `ReplaySwitcher`）
- F-VID-03、N-03: セッション統合（再生検出 → 動画特定 → リプレイ／ライブ取得 → 同期 → オーバーレイ表示。`feature:overlay` の `session` パッケージ）
- F-SYNC-03/04/05: 位置推定・一時停止・シーク判定・速度追従（`core:sync` の `SyncEngine`）
- F-SYNC-06: 同期オフセットの手動補正（`core:sync` の `SyncOffset`。±10 秒・0.5 秒刻み。正の値でチャットを早く表示）。
  `ChatPlayer` がリプレイの同期に使う再生状態の位置へ補正を足す（PLAN 4.5 の「推定位置 + 手動補正」）。
  補正は SharedPreferences `overlay` の `syncOffsetMs` に保存して引き継ぐ。ライブ・プレミアには効かない（表示遅延を使う）
- F-SYNC-07、F-OVL-09: 手動タイマーモード（`core:sync` の `ManualTimer`。再生状態 `PlaybackSnapshot` を開始・停止・位置入力で作る。等速）。
  設定パネル（リプレイ時）の「手動タイマー」でオンにすると、表示中の位置（補正を除く）で停止した状態から始める。
  手動中は `SessionEnvironment.manualTimer` を公式アプリの再生状態の代わりにリプレイの同期へ使い、同期状態を「手動」と表示する。
  ライブ・プレミアは対象外。位置入力（`OverlayFormat.parsePosition`。`h:mm:ss`・`m:ss`・秒数、全角コロン可）の間だけ
  ウィンドウの `FLAG_NOT_FOCUSABLE` を外し（`OverlayWindow.focusable`）、確定・取消で戻す。手動タイマーの状態は保存しない
- F-CHAT-02/03: リプレイの先読み取得とシーク時の再取得（`core:sync` の `ReplaySession`）
- F-VIEW-01: 文字サイズ（フローティングの設定パネル）と、投稿者名・アイコン・時刻の表示有無（アプリの表示設定画面）
- F-VIEW-03: NG ワードと種別（スパチャ・メンバー・モデレーター／配信者）の絞り込み（`feature:overlay` の `ChatFilter`）
- F-CHAT-09: カスタム絵文字・メンバースタンプ・スーパーステッカーの画像表示（`feature:overlay` の `MessageText`・`ImagePolicy`）

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
- すべてのチャットの continuation（`allChatToken`）: 見出し `header.liveChatHeaderRenderer.viewSelector.sortFilterSubMenuRenderer`
  の `subMenuItems[1]`（0 = 上位チャット、1 = すべてのチャット）の `reloadContinuationData`。見出しは最初の取得応答にだけ含まれ、
  2 回目以降は null。
- JSON は `kotlinx.serialization` の `JsonElement` を必要箇所だけ辿る（`internal/JsonNav.kt`）。型不一致・欠落は null とし、
  `liveChatContinuation` が無い・JSON でない場合は `ChatParseResult.Failure` を返す（例外を投げない）。
- `InnerTubeClient`（通信先は `https://www.youtube.com/` のみ。Cookie を保持しない）:
  - 共通: `POST youtubei/v1/<path>?prettyPrint=false`、本文に `context.client`（`clientName=WEB`・`clientVersion`・`hl=ja`・`gl=JP`）。
    `clientVersion` の既定値は定数で持ち、実応答での有効性は人手検証で確認する。
  - `fetchVideoChatInfo(videoId)`: `next` 応答の `twoColumnWatchNextResults` から、タイトル（`videoPrimaryInfoRenderer.title`）、
    チャンネル名（`videoSecondaryInfoRenderer.owner.videoOwnerRenderer.title`）、`conversationBar.liveChatRenderer` の
    `isReplay` と、チャット欄本体の `continuations` の `reloadContinuationData`（上位チャットの表示。`topChatToken`）を読む。
    `next` 応答の見出しの切り替えメニューのトークンは動画IDを含まない雛形で、送ると HTTP 400 になるため使わない
    （2026-09-27 実機検証。アーカイブ・ライブとも同じ）。
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
  1. `videoIdHints` の先頭 → `Confirmed(METADATA)`（キャッシュにも登録）。公式アプリは動画IDを公開しないため（Q-02）、
     現状は常に手順2以降で特定する。公式アプリの変化に備えて残す
  2. `ResolutionCache`（キーは `TrackMetadata.identity`）→ `Confirmed(CACHE)`
  3. 長さが 0 以下（ライブ・プレミア中）でチャンネル名があれば、`search(チャンネル名, liveOnly = true)`（検索の絞り込み
     「ライブ」`params`）の結果のうちライブ表示のあるものを同じ規則で採点し、確定すれば `Confirmed(LIVE)`。
     確定しなければ（確度不足・0 件・通信失敗）手順4へ進む。
  4. `InnerTubeClient.search("タイトル チャンネル名")` の結果を採点。タイトル完全一致 50・部分一致 25、チャンネル名一致 30、
     長さ ±2 秒一致 20。比較は NFKC 正規化・小文字化・空白除去後。1 位が 80 点以上かつ 2 位との差 10 点以上なら
     `Confirmed(SEARCH, alternatives=他の候補)`、それ以外で 1 点以上の候補があれば `Ambiguous`（上位 5 件）、無ければ `NotFound`。
     通信失敗は `Failed`。
  - ユーザーが候補を選んだら `remember()` でキャッシュへ登録する（`ResolutionSource.USER`）。
  - `InMemoryResolutionCache`: 最大 200 件の LRU。`snapshot()` で永続化用に取り出せる（端末内にのみ保存。N-06）。
- 検索元 `VideoSearchSource.search(query, liveOnly)`。本番は `InnerTubeClient.search`（`liveOnly` で `params = SEARCH_PARAMS_LIVE`。
  値の有効性は人手検証で確認する）。
- `resolve.SearchResultParser`: `twoColumnSearchResultsRenderer…sectionListRenderer.contents[].itemSectionRenderer.contents[].videoRenderer`
  から videoId・タイトル・チャンネル名（`ownerText` → `longBylineText` → `shortBylineText`）・長さ（`lengthText` の `h:mm:ss`）・
  ライブ表示（`BADGE_STYLE_TYPE_LIVE_NOW`）を読む。
- テストの fixture（`core/chat/src/test/resources/fixtures/`）は既知の応答構造に基づく合成データで、値まで検証する。
  実応答由来の fixture は `fixtures/real/` に置き、`RealResponseFixtureTest` が構造（解析できること・必要な値があること）だけを検証する
  （無ければスキップ。現在はアーカイブ・配信中の動画・検索の実応答を置いている）。
  `fixtures/real/` は `scripts/fetch-real-fixtures.py` が作る。アプリと同じリクエスト（`clientVersion` は
  `InnerTubeClient.kt` から読む）で next・リプレイ・ライブ・検索の応答を取得し、解析に使わない部分木と追跡用の値を削り、
  投稿者名・投稿者のチャンネルID・画像 URL・コメント本文を仮の値へ置き換え、元の値と動画配信サーバーの URL（取得した端末の
  IP アドレスを含む）が残っていないことを検査してから保存する。
  元の応答は保存しない。`--raw-dir` で通信せずに置き換えだけを行える。実通信を伴う実行は人が行う（guardrails 12.5）。

### ライブチャットの取得（`core:sync` の `LiveChatSession`）

- 取得元 `LiveChatSource`（本番は `InnerTubeClient.fetchLive`）を、応答の継続トークンを更新しながら繰り返し呼ぶ。
  次の取得までの待ち時間は応答の `timeoutMs`（無ければ 5 秒）を 1〜10 秒に収めた値（`LivePolling`）。
- 受信したメッセージは ID で重複を除き、受信時刻（`clock` 基準）を付けて `LiveState.received` に受信順で保持する（上限 500 件。
  重複判定の ID は上限の 4 倍まで覚える）。受信時刻は表示遅延（F-SYNC-08）の起点に使う。
- 継続トークンが無い応答で終了（`ended = true`）とし、ループを抜ける（F-CHAT-06）。
- 公式アプリが一時停止（`PAUSED`）の間は取得せず、1 秒ごとに再開を確かめる（N-03）。プレミアの待機中は公式アプリが再生状態に
  ならないため、一時停止以外の状態（再生中・未検出・停止・バッファ中）では取得を続ける（F-CHAT-05）。
- 失敗（クライアントの再試行後）は `FetchStatus.Failed` とし、10 秒後に同じトークンで取り直す。

### ライブの最新追従表示（`core:sync` の `LiveTimeline`）

- `LiveTimeline.visible(received, now, delayMs)`: 受信時刻 + 表示遅延 ≦ 現在時刻 のメッセージを受信順に返す（上限 500 件、新しい方を残す）。
- 表示遅延は 0〜30 秒（既定 0 秒）。公式アプリの映像は配信より遅れて届くため、チャットが映像より先に流れる場合に利用者が遅らせる。
- 設定はオーバーレイの設定パネル（同期状態が LIVE の時だけ表示するスライダー、1 秒刻み）で変え、SharedPreferences `overlay` の
  `liveDelaySeconds` に保存する。`OverlayService` が `StateFlow` で保持し、セッションへ渡す。

### ライブ終了後のリプレイへの切り替え（`core:sync` の `ReplaySwitcher`）

- `LiveChatSession` が終了（継続トークン無し）したら、`ReplaySwitcher.await(videoId)` で動画の情報を取り直し、
  リプレイが使えるようになるのを待つ（終了直後はリプレイが準備されていないことがあるため）。
- 確認の間隔は 30 秒・1 分・2 分・5 分・10 分（合計約 18 分）。各待ちの前に `onWaiting(回数, 待ち時間)` で UI へ知らせる。
- 結果: リプレイになっていれば `Ready(continuation)`（`topChatToken`。すべてのチャットへの切り替えは `ChatPlayer` が行う）、まだライブなら `StillLive(continuation)`
  （終了判定が早すぎた場合。ライブの取得へ戻る）、最後まで準備されなければ `Unavailable`。
  チャット無効（`Unavailable`）と通信失敗は準備中の可能性があるため、諦めずに次の確認まで待つ。

### 再生状態の取得（`core:media`）

- `MediaListenerService`（`NotificationListenerService`）は「通知へのアクセス」の許可を受けるためだけに置き、通知内容は読まない。
  manifest は `exported=true`・`permission=BIND_NOTIFICATION_LISTENER_SERVICE`。
- `PlaybackMonitor`: `MediaSessionManager.getActiveSessions(リスナーのコンポーネント)` と `addOnActiveSessionsChangedListener` で
  パッケージ `com.google.android.youtube` の `MediaController` を追跡し、`MediaController.Callback` で状態・メタデータの変化を
  `state: StateFlow<NowPlaying>`（スナップショット・`TrackMetadata`・セッション有無・デバッグ用の全キー一覧）へ反映する。
  コールバックはメインスレッド。許可が無い・`SecurityException` の場合は開始しない。
  MediaMetadata の各キーは `MediaMapping.metadataValueType` の型表（標準キーと実機で確かめた公式アプリ独自キー）に従い、
  型に合った取得メソッドだけで読む（型違いの取得は Bundle が警告のスタックトレースを logcat へ出すため）。画像・評価は文字列化しない。
  型表に無いキーは文字列 → 数値の順に試し、読めた型を覚えて次回からはその型だけで読む。
- `MediaMapping`（純粋関数。JVM テスト可能）:
  - 状態: PLAYING / FAST_FORWARDING / REWINDING → PLAYING、PAUSED → PAUSED、BUFFERING / CONNECTING / SKIPPING_* → BUFFERING、
    STOPPED / ERROR → STOPPED、その他 → NONE。速度が 0 以下なら 1.0 とみなす。負の位置は 0。
  - `TrackMetadata`（`core:chat` の `resolve` パッケージ）: タイトル（無ければメタデータ無し）、ARTIST をチャンネル名、DURATION を長さ、
    `videoIdHints`。`identity`（タイトル・チャンネル名・長さ）の変化で動画の切り替えとみなす（F-VID-03）。
  - `videoIdHints`（PLAN 4.3 手順1）: MediaMetadata の全キー、MediaDescription の mediaId / mediaUri / extras、controller と
    PlaybackState の extras、キューの mediaId / mediaUri を集め、値に YouTube URL があればその ID、キー名の末尾要素に `id` を含み
    値が 11 桁 ID 形式ならその値を候補にする。実機（YouTube 21.38.130）では候補は常に空（Q-02）。
  - 広告再生中もタイトル・チャンネル名・長さは本編の値のままで、メタデータでは広告を区別できない（PLAN K-05。
    広告中の position は広告自体の再生位置で、状態・actions も本編と同じため区別できない。広告の検知はせず、
    既知の制約として README で案内する。広告明けは位置の変化をシークとして扱い、表示し直す）。
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

### フローティングウィンドウ（`feature:overlay`）

- `OverlayService`（`LifecycleService` + `SavedStateRegistryOwner`）: `foregroundServiceType="specialUse"`（用途を
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` に記載）で常駐し、`WindowManager.addView`（`TYPE_APPLICATION_OVERLAY`、
  `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL`、`TOP|START`）で `ComposeView` を表示する。
  `ComposeView` にはサービス自身を `ViewTreeLifecycleOwner` / `ViewTreeSavedStateRegistryOwner` として設定する。
  - 起動は `OverlayService.start(context)`（オーバーレイ権限が無ければ false）。前面のアクティビティから呼ぶ。
  - 常駐通知（チャンネル `overlay`、重要度 LOW）に「表示／非表示」「終了」の操作を付け、本文タップでアプリを開く（F-OVL-08）。
  - 常駐通知の本文は表示中の動画タイトル。`OverlayNotifications.titleChanges(OverlayChannel.state)`（タイトルの変化だけを流す）を
    購読し、変わったときだけ通知を出し直す（BL-033。状態は表示の更新ごとに発行されるため、同じタイトルでは出し直さない）。
  - ウィンドウの位置・大きさ・不透明度・文字サイズは SharedPreferences `overlay` に保存し、表示時に画面内へ収める（`WindowBounds.clampTo`）。
    既定 280×360dp、最小 160dp。ヘッダーのドラッグで移動、右下のハンドルのドラッグでサイズ変更（F-OVL-02）。
  - ウィンドウの追加・削除と `LayoutParams` の反映は `OverlayWindow`、位置・大きさの計算と保存は `WindowPlacement` に置く。
    位置・大きさは画面の向き（`ScreenOrientation`。
    幅＞高さで横）ごとに保存し、縦は従来のキー、横は `landscape.` を前置したキーを使う。`onConfigurationChanged` で向きの変化を
    検知したら、その向きの保存値へ切り替える（F-OVL-06）。
  - 不透明度は 0.2〜1.0（既定 0.6）。ヘッダーの設定ボタンでスライダーを出す（F-OVL-03）。
    ヘッダー（ドラッグ領域）は青みの灰色 #37474F、チャット欄は黒で塗り分け、不透明度は両方の背景に同じ値を掛ける（BL-035）。
  - タッチ透過モード（F-OVL-05）: 設定パネルから入り、`FLAG_NOT_TOUCHABLE` を付けてウィンドウの `LayoutParams.alpha` を 0.8 に
    下げる（0.8 を超える他アプリのオーバーレイ越しのタッチは OS に遮断される）。透過中はウィンドウを触れないため、解除は常駐通知の
    「タッチ透過を解除」で行い、ヘッダーに「タッチ透過中」と出す。透過モードは保存しない（起動ごとに解除状態から始める）。
  - 最小化（F-OVL-04）: ヘッダーの最小化ボタンで 48dp の丸いバブル（`ui/Bubble.kt`）にする。タップで復帰、ドラッグで移動。
    バブルは通常表示の左上に出し、バブル自身の大きさで画面内へ収める（移動は通常表示の位置にも反映し、復帰時に収め直す）。
    縁の色で同期状態を示し（同期中・ライブは緑）、不透明度には下限 0.7 を置く。最小化状態は保存しない。
  - 設定パネル（`ui/SettingsPanel.kt`。高さ 200dp を上限にスクロール）: 不透明度・文字サイズ・表示遅延（LIVE の時）または
    同期の補正と手動タイマー（それ以外）・タッチ透過。
    値は `OverlaySettings`（不透明度・文字サイズ・表示遅延・補正）にまとめ、`OverlayActions.onSettingsChange` で受け取り、
    操作の終わり（`onGestureEnd`）に `OverlayPrefs.settings` へ保存する。セッションへは表示遅延と補正を `StateFlow` で渡す。
  - チャットの文字サイズは倍率 0.8〜1.5（0.1 刻み、既定 1.0＝中）。設定パネルのスライダーで変え、`fontScale` に保存する。
    チャット欄だけ `LocalDensity` の `fontScale` に倍率を掛けて反映する（各 `Text` の sp は変えない。F-VIEW-01 の文字サイズ）。
- 表示内容の受け渡し: プロセス内オブジェクト `OverlayChannel` の `state: StateFlow<OverlayUiState>`（タイトル・メッセージ・
  再生位置・同期状態・お知らせ文・候補）へセッション側が書き込み、ウィンドウが購読する。利用者の操作は
  `events: SharedFlow<OverlayEvent>`（候補の選択・終了）でセッション側へ返す。
- 一覧（F-OVL-07）: `AutoScrollPolicy` で、利用者が遡ってドラッグを終えた時点で最下部でなければ追従を止めて「最新へ」ボタンを出し、
  最下部に戻るかボタン押下で追従を再開する。追従中は新着ごとに最下部へスクロールする。
- 表示: 通常メッセージは投稿者名（所有者＝黄・モデレーター＝青・メンバー＝緑）と本文。
  スーパーチャット・スーパーステッカーは見出し帯（投稿者名・金額）と本文帯を応答の色で塗り、輝度で黒／白文字を選ぶ（F-VIEW-02）。
  メンバー加入・ギフトは緑の帯。お知らせ文（チャット無効 F-VID-07・通信失敗など）は赤帯で表示する。
- 表示設定（F-VIEW-01）: `DisplaySettings`（投稿者名・アイコン・時刻の表示有無。既定は名前のみで従来と同じ）を
  `DisplaySettingsStore`（SharedPreferences `display`。`init(context)` 後に `state: StateFlow` を購読、`update` で保存）で共有する。
  アプリ画面の「表示設定」（`DisplaySettingsScreen`）で変え、表示中のウィンドウへすぐ反映する。時刻はリプレイなら動画内の位置、
  ライブ・プレミアなら投稿時刻 `H:mm`（`OverlayFormat.messageTime`）。アイコンは `ImagePolicy` を通した URL だけ 18dp の丸で出す。
  スーパーチャットの帯の投稿者名は設定によらず出す。
- 絞り込み（F-VIEW-03）: `ChatFilter.apply` でウィンドウの表示直前に絞る。「スパチャのみ」（`paid` あり）・「メンバーのみ」
  （MEMBER の役割、またはメンバー加入・ギフト）・「モデレーター・配信者のみ」（MODERATOR / OWNER）はオンにしたもののいずれかに
  当てはまれば出す（すべてオフなら絞らない）。NG ワードを本文（`plainText`）に含むものは大文字・小文字を区別せず常に除く。
  NG ワードは表示設定画面で 1 行 1 語で入力し「保存」で取り込む（`parseNgWords`。空行・重複を除き最大 100 語・1 語 50 字）。
- 画像（F-CHAT-09）: Coil（`coil-compose`）で読み込む。本文は `MessageText` で描き、カスタム絵文字・メンバースタンプを
  `InlineTextContent`（1.4em）で文中に差し込む。URL が無い・許可外・読み込み失敗なら代替テキスト、Unicode の絵文字は文字のまま。
  スーパーステッカーは 56dp の画像（失敗時は「（スーパーステッカー）」）。応答の URL は `ImagePolicy` で HTTPS かつ
  `*.ggpht.com`・`*.ytimg.com`・`*.googleusercontent.com` に限る（N-05）。

### アプリ画面（`app`）

- `MainActivity`（`singleTop`）1 画面構成。Compose で `HOME`・`DISPLAY`（表示設定）・`LICENSES` を切り替える（ナビゲーションライブラリは使わない）。
  テーマは端末の壁紙色（dynamic color）とシステムのライト／ダーク設定に従う。
- HOME の構成（上から）: アプリ名と副題、免責表示（F-APP-04）、権限案内（F-APP-01）、フローティング表示の開始／終了、
  URL 入力（F-VID-05）、表示設定へのボタン、診断情報（折りたたみ）、OSS ライセンスへのリンク。
- 権限案内: `PermissionStatus` で「オーバーレイ → 通知へのアクセス → 通知の表示」の順に次の未許可を強調し、各行の「設定を開く」で
  それぞれの設定画面（通知へのアクセスは `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` にコンポーネント名を付け、無ければ一覧画面）
  または実行時許可を出す。状態は `onResume` で読み直す。通知へのアクセスは「通知の内容を読まない」旨を説明文に書く（PLAN 4.9）。
  フローティング表示の開始にはオーバーレイ権限だけを必須とする。
- 動画の指定（F-VID-04/05）: 共有（`ACTION_SEND` / `text/plain`）の本文・件名、または URL 入力欄から `VideoUrlParser` で ID を取り出し、
  `OverlayChannel.requestVideo(id)` で保持してから `OverlayService.start` する（セッション開始前でも失われないよう StateFlow で保持）。
  共有から開いた場合は画面を閉じて公式アプリへ戻る。URL が無い場合はトーストで知らせる。
- 診断情報: アクティビティ表示中だけ `PlaybackMonitor` を動かし、状態・位置・速度・タイトル・チャンネル名・長さ・動画ID候補と
  MediaSession の全キーを等幅で表示する（選択してコピー可能。送信しない）。M0（Q-01 / Q-02）の実機確認に使う。
- OSS ライセンス（F-APP-03）: AboutLibraries（Gradle プラグインがビルド時に依存一覧を生成し、`LibrariesContainer` で表示）。
- 文言は日本語のみ（英語リソースは未対応。N-11 は SHOULD）。

### セッション統合（`feature:overlay` の `session` パッケージ）

- `OverlayService` が `onCreate` で `PlaybackMonitor` を開始し、`WatchCoordinator.run()` を `lifecycleScope`（メインスレッド）で動かす。
  画面のオン・オフは `ACTION_SCREEN_ON/OFF` のレシーバー（`RECEIVER_NOT_EXPORTED`）と `PowerManager.isInteractive` で追う。
  `onStartCommand` のたびに `PlaybackMonitor.start()` を試す（通知へのアクセスが後から許可された場合に備える）。
  終了時はモニター・レシーバーを止め、表示内容を初期化する。
- 構成: `WatchCoordinator`（入力の命令化・動画の特定）、`ChatPlayer`（1 本の動画の取得・同期・表示）、`OverlayPublisher`
  （表示の土台＝タイトル・候補を保持して `io.publish` する）、`SessionEnvironment(nowPlaying, screenOn, liveDelaySeconds, clock)`、
  `SessionIo(requestedVideo, takeRequestedVideo, events, publish)`、`ChatBackend`（videoInfo・replay・live・search）。
- `WatchCoordinator`: 入力（再生中の動画の識別キーの変化、共有・URL 入力の指定、候補の選択）を命令のキューへ入れ、命令ごとに
  実行中の処理を取り消して新しい処理を始める。
  - 再生中の動画の変化（F-VID-03）: 識別キー（タイトル・チャンネル名・長さ）が変わったら `VideoResolver` で特定し直す。
    再生を検出していなければ「公式アプリの再生を検出していません」。確定なら開く（他の候補は切り替え候補として表示）、
    候補ありなら候補を表示、見つからなければ共有・URL 入力を案内、通信失敗なら失敗の説明。
  - 手動指定（F-VID-04/05）: 最優先で開き、「指定中」とする。指定中は再生中の動画が変わるまで自動特定しない
    （指定時に再生を検出していなければ、最初に検出した動画を指定した動画とみなす）。
  - 候補の選択（F-VID-02）: `VideoResolver.remember` でキャッシュへ覚えてから開く。
- `ChatPlayer.open(videoId, alternatives)`: `next` で情報を取り、チャット無効なら説明文（応答の文言、無ければ既定文。F-VID-07）。
  チャットがあれば、アーカイブならリプレイ、ライブ・プレミア中（待機中を含む）ならライブを表示する。表示の開始時（画面オンのたび）に
  `topChatToken` で 1 回取得し、応答の `allChatToken` があれば「すべてのチャット」へ切り替える（取得失敗・見出し無しなら上位チャットのまま）。
  既定を「すべてのチャット」にしたのは、閲覧専用ビューワーとして取りこぼしの無い表示を優先するため（切り替え F-CHAT-07 は M4）。
  - リプレイ: `ReplaySession` の状態から、メッセージ・位置・同期状態（再生中＝同期中、一時停止・バッファ中＝一時停止、
    MediaSession 無し＝未検出）・お知らせ（再接続中 n 回目・失敗の説明・未検出）を表示する。
  - ライブ（F-CHAT-04/05、F-SYNC-08）: `LiveChatSession` を動かし、250ms ごとに `LiveTimeline.visible`（表示遅延は
    `liveDelaySeconds`）で表示を更新する。同期状態は LIVE（公式アプリが一時停止なら一時停止）。
  - ライブの終了（F-CHAT-06）: `ReplaySwitcher` でリプレイの準備を待ち（待機中は「配信は終了しました。リプレイの準備を待っています
    （n 回目）」）、準備できればリプレイへ、まだ配信中ならライブへ戻り、準備されなければ「リプレイは利用できません」。
  - 画面オフ（N-03）: 画面が消えたら取得を止めて「画面オフのため停止中」を表示し、点いたら取得をやり直す（`transformLatest`）。
  - 表示文は `SessionMessages` に集約する（日本語のみ）。
- `PersistentResolutionCache`: 特定のキャッシュを `filesDir/resolution-cache.json`（`[{identity, videoId}]`）へ `AtomicFile` で保存し、
  起動時に読み込む。壊れていれば空から始める（端末内のみ。N-06）。
- `core:chat` は `InnerTubeClient` のコンストラクターが OkHttp の型を公開するため、OkHttp を `api` 依存にする。
- `feature:overlay` は画像読み込みに Coil 2.7.0（`io.coil-kt:coil-compose`）を使う。既定の `ImageLoader`（シングルトン）で足りるため設定しない。

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
