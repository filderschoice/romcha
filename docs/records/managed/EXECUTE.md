<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- date: 2026-09-27 13:59
  summary: 実応答から fixture を作るスクリプトと、実応答 fixture で Parser を検証する単体テストを追加
  details:
    変更内容: >-
      BL-022 の fixture の作り直しに向けて、scripts/fetch-real-fixtures.py を追加した。アプリと同じ InnerTube のリクエストで
      next・get_live_chat_replay・get_live_chat・search の応答を取得し、解析に使わない部分木と追跡用の値を削り、投稿者名・
      投稿者のチャンネルID・画像 URL・コメント本文を仮の値へ置き換え、元の値が残っていないことを検査してから fixtures/real へ保存する。
      --raw-dir で通信せずに置き換えだけを行える。RealResponseFixtureTest（5 件）を追加し、fixtures/real が無い場合はスキップする。
      自律ループ内では実通信を行わず（guardrails 12.5）、既存の合成 fixture を --raw-dir の入力にして検証した。
    変更ファイル:
      - scripts/fetch-real-fixtures.py
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/RealResponseFixtureTest.kt
      - docs/VERIFICATION.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      python scripts/fetch-real-fixtures.py --raw-dir（合成 fixture を入力）、./gradlew :core:chat:test（fixtures/real あり・なし）、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 合成 fixture の投稿者 7 人・コメント 6 件が置き換わり元の名前が残らないことを確認。fixtures/real ありで 5 件成功、
      なしで 5 件スキップ。品質ゲートは終了コード0。実応答での実行は未実施（BL-022 の人手検証）
    関連ID:
      - BL-032
- date: 2026-09-27 12:45
  summary: チャット取得が HTTP 400 で失敗し続ける問題を修正（next 応答の雛形トークンを使わない）
  details:
    変更内容: >-
      WatchInfoParser が next 応答の見出しの切り替えメニュー（上位／すべてのチャット）の continuation を優先していたが、
      このトークンは動画IDを含まない雛形で、get_live_chat_replay・get_live_chat とも HTTP 400 を返していた（実機と PC からの再現で確認）。
      チャット欄本体の reloadContinuationData（上位チャット）を topChatToken として使い、VideoChatInfo から allChatToken を削除した。
      ChatResponseParser が取得応答の見出しから「すべてのチャット」の continuation（allChatToken）を読み、
      ChatPlayer は表示の開始時に topChatToken で 1 回取得して allChatToken へ切り替える（画面オン中のみ取得。N-03）。
      ReplaySwitcher は topChatToken を返す。単体テストの期待値を更新し、見出しの読み取りのテストとフィクスチャを追加した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/ChatResponseParser.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/WatchInfoParser.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/ChatResponseParserTest.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClientTest.kt
      - core/chat/src/test/resources/fixtures/replay_chunk.json
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySwitcher.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySwitcherTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py、実機（Pixel 8 Pro）でアーカイブ再生中にフローティング表示を確認
    検証結果: 成功 - 終了コード0、実機で HTTP 400 が解消し、公式アプリの再生位置に同期したリプレイチャットの表示を確認
    関連ID:
      - BL-029
- date: 2026-09-27 12:34
  summary: MediaMetadata の読み取りで Bundle の型不一致警告が logcat へ大量出力される問題を修正
  details:
    変更内容: >-
      PlaybackMonitor が MediaMetadata の全キーへ getText と getLong を順に試していたため、画像（Bitmap）・数値のキーで
      Android の Bundle が ClassCastException のスタックトレースを警告として再生状態の変化ごとに出力していた。
      MediaMapping に標準キーと公式アプリ独自キー（MEDIA_METADATA_VIDEO_WIDTH_PX / HEIGHT_PX）の型表を追加し、
      型に合った取得メソッドだけで読むようにした。画像・評価のキーは従来どおり文字列化しない。
      型の分からないキーは文字列、数値の順に試し、読めた型を覚えて次回からはその型だけで読む。キーの型判定の単体テストを追加した。
    変更ファイル:
      - core/media/src/main/kotlin/io/github/filderschoice/romcha/core/media/MediaMapping.kt
      - core/media/src/main/kotlin/io/github/filderschoice/romcha/core/media/PlaybackMonitor.kt
      - core/media/src/test/kotlin/io/github/filderschoice/romcha/core/media/MediaMappingTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py、実機（Pixel 8 Pro）で公式アプリの再生と一時停止を 5 回切り替えて
      adb logcat のアプリのプロセスの Bundle 警告の件数を数える
    検証結果: 成功 - 終了コード0、実機の Bundle 警告は修正前の 66 行から 0 行になった
    関連ID:
      - BL-028
- date: 2026-09-27 23:40
  summary: ライブ・プレミアのセッション統合（状態判定、最新追従表示、終了時のリプレイ切り替え）を実装
  details:
    変更内容: >-
      F-CHAT-04〜06 と F-SYNC-08 に基づき、動画の状態に応じてリプレイ／ライブの取得経路を選ぶ ChatPlayer を追加し、
      WatchCoordinator から動画の再生処理を分離した。ライブは LiveChatSession と LiveTimeline（表示遅延の設定を反映）で最新追従表示し、
      終了後は ReplaySwitcher でリプレイの準備を待って切り替える。セッションの入出力と端末側の状態を SessionIo・SessionEnvironment にまとめ、
      ChatBackend に live を追加し、SessionTiming にリプレイ準備の確認間隔を追加した。統合の単体テストを 3 件追加した。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySession.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatBackend.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionEnvironment.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionMessages.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinator.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      ./gradlew :app:assembleDebug、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、feature:overlay の単体テスト 17 件成功、デバッグ APK の生成を確認
    関連ID:
      - BL-018
- date: 2026-09-27 22:45
  summary: 動画特定パイプラインの手順3（配信中・プレミア公開中の動画とのタイトル照合）を実装
  details:
    変更内容: >-
      PLAN 4.3 手順3 に基づき、再生中の動画の長さが不明（0 以下）の場合にチャンネル名で「ライブ」に絞った検索を行い、
      ライブ表示のある候補をタイトル・チャンネル名で採点して確定する処理を VideoResolver に追加した。確定しなければ通常の検索（手順4）へ進む。
      InnerTubeClient.search に liveOnly（検索の絞り込み params）を追加し、検索元のインターフェースを変更した。単体テスト 4 件を追加した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClient.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolver.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClientTest.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolverTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatBackend.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:chat の単体テスト 38 件成功
    関連ID:
      - BL-017
- date: 2026-09-27 22:15
  summary: ライブ・プレミア中の最新追従表示（LiveTimeline）と表示遅延の設定を追加
  details:
    変更内容: >-
      F-SYNC-08 に基づき、受信時刻から表示遅延だけ経ったメッセージを表示する LiveTimeline を core:sync に追加し、
      オーバーレイの設定パネルに LIVE 時だけ表示する表示遅延のスライダー（0〜30 秒）と、その端末内保存を追加した。
      単体テスト 4 件を追加した。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/LiveTimeline.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/LiveTimelineTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayPrefs.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/res/values/strings.xml
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:sync の単体テスト 32 件成功
    関連ID:
      - BL-016
- date: 2026-09-27 21:55
  summary: ライブ・プレミア終了後にリプレイの準備を待って切り替える ReplaySwitcher を core:sync に実装
  details:
    変更内容: >-
      F-CHAT-06 に基づき、ライブの終了（継続トークン無し。LiveChatSession.ended で通知）後、30 秒〜10 分の間隔で動画の情報を取り直し、
      リプレイが使えるようになったら continuation を返す ReplaySwitcher を追加した。まだ配信中ならライブへ戻り、
      合計約 18 分待っても準備されなければ諦める。仮想時間を使う単体テスト 4 件を追加した。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySwitcher.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySwitcherTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:sync の単体テスト 28 件成功
    関連ID:
      - BL-015
- date: 2026-09-27 21:30
  summary: ライブ・プレミアのチャットをポーリングで追従する LiveChatSession を core:sync に実装
  details:
    変更内容: >-
      F-CHAT-04/05 に基づき、応答の継続トークンを更新しながら推奨間隔（1〜10 秒に制限）でライブチャットを取得する LiveChatSession を追加した。
      受信時刻付きで重複を除いて保持し、継続トークンが無くなったら終了とする。公式アプリの一時停止中は取得せず（N-03）、
      プレミア待機中を考慮して一時停止以外の状態では取得を続ける。MockWebServer と InnerTubeClient を使う単体テスト 6 件を追加した。
    変更ファイル:
      - core/sync/build.gradle.kts
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/LiveChatSession.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/LiveChatSessionTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:sync の単体テスト 24 件成功
    関連ID:
      - BL-014
- date: 2026-09-27 20:50
  summary: 再生検出から動画特定・リプレイ取得・同期・オーバーレイ表示までをつなぐセッション統合を実装
  details:
    変更内容: >-
      PLAN 4.1 と F-VID-02/03/04/05、N-03 に基づき、WatchCoordinator（再生中の動画の変化で再特定、手動指定の優先と指定中の自動特定抑止、
      候補選択の記憶、チャット無効・ライブ中の表示、画面オフ中の取得停止）、ChatBackend、特定キャッシュを端末内に保存する
      PersistentResolutionCache を追加し、OverlayService から PlaybackMonitor・画面オン／オフの監視とともに起動するようにした。
      常駐通知の組み立てを OverlayNotifications に分け、core:chat の OkHttp 依存を api にした。統合の単体テスト 7 件を追加した。
    変更ファイル:
      - core/chat/build.gradle.kts
      - feature/overlay/build.gradle.kts
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayNotifications.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatBackend.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionMessages.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinator.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/PersistentResolutionCache.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      ./gradlew :app:assembleDebug、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、feature:overlay の単体テスト 14 件成功、デバッグ APK の生成を確認
    関連ID:
      - BL-012
- date: 2026-09-27 19:55
  summary: アプリ画面（権限案内・共有受信・URL 入力・免責・OSS ライセンス・診断表示）を app に実装
  details:
    変更内容: >-
      F-APP-01/03/04 と F-VID-04/05 に基づき、権限を順に案内する画面、公式アプリの共有（ACTION_SEND）からの動画指定、URL 入力、
      免責表示、AboutLibraries による OSS ライセンス一覧、M0 確認用の MediaSession 診断表示を追加した。
      指定された動画はセッション開始前でも失われないよう OverlayChannel の StateFlow で保持する。INTERNET 権限を宣言した。
      権限の案内順と共有テキストからの ID 抽出の単体テスト 3 件を追加し、デバッグ APK のビルドを確認した。
    変更ファイル:
      - app/build.gradle.kts
      - app/src/main/AndroidManifest.xml
      - app/src/main/res/values/strings.xml
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/PermissionStatus.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/RomchaApp.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeActions.kt
      - app/src/test/kotlin/io/github/filderschoice/romcha/PermissionStatusTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayUiState.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      ./gradlew :app:assembleDebug、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、app の単体テスト 3 件成功、デバッグ APK の生成を確認
    関連ID:
      - BL-011
- date: 2026-09-27 19:20
  summary: フローティングウィンドウ（フォアグラウンドサービスと Compose の表示）を feature:overlay に実装
  details:
    変更内容: >-
      F-OVL-01/02/03/07/08、F-VIEW-02、F-VID-07 と PLAN 4.6 に基づき、TYPE_APPLICATION_OVERLAY のウィンドウへ Compose の画面を載せる
      OverlayService（specialUse のフォアグラウンドサービス、常駐通知から表示切り替え・終了）、ドラッグ移動・サイズ変更・不透明度変更、
      新着の自動スクロールと「最新へ」ボタン、スーパーチャットの色帯表示、表示内容を受け渡す OverlayChannel を追加した。
      表示値の計算（位置表記・文字色の選択・ウィンドウの収め方・自動スクロール判断）の単体テスト 7 件を追加した。
    変更ファイル:
      - feature/overlay/src/main/AndroidManifest.xml
      - feature/overlay/src/main/res/values/strings.xml
      - feature/overlay/src/main/res/drawable/ic_overlay_notification.xml
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayPrefs.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayUiState.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatItems.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、feature:overlay の単体テスト 7 件成功、Android lint 指摘0件
    関連ID:
      - BL-010
- date: 2026-09-27 18:30
  summary: 動画の自動特定パイプライン（MediaSession の ID、端末内キャッシュ、検索照合と採点）を core:chat に実装
  details:
    変更内容: >-
      F-VID-01/02 と PLAN 4.3 手順1・2・4 に基づき、VideoResolver・InMemoryResolutionCache・SearchResultParser を追加し、
      InnerTubeClient に search を追加した。検索結果をタイトル・チャンネル名・長さで採点し、閾値と上位2件の差で自動確定か候補提示かを判定する。
      タイトル比較は NFKC 正規化・小文字化・空白除去で表記揺れを吸収する。単体テスト 11 件と検索応答の合成 fixture を追加した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClient.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/SearchResultParser.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolver.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolverTest.kt
      - core/chat/src/test/resources/fixtures/search_results.json
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:chat の単体テスト 34 件成功
    関連ID:
      - BL-009
- date: 2026-09-27 17:55
  summary: 公式アプリの MediaSession から再生状態を取得する PlaybackMonitor を core:media に実装
  details:
    変更内容: >-
      F-SYNC-01/02 と PLAN 4.2 に基づき、通知へのアクセス許可を受ける MediaListenerService、MediaSession を追跡する PlaybackMonitor、
      値を Android 非依存モデルへ変換する MediaMapping を追加した。4.3 手順1 のためメタデータの全キーから動画IDの候補を探し、
      M0（Q-02）の実機確認用に全キーの一覧を公開する（画面表示のみ）。TrackMetadata を core:chat の resolve パッケージに追加した。
    変更ファイル:
      - core/media/src/main/AndroidManifest.xml
      - core/media/src/main/res/values/strings.xml
      - core/media/src/main/kotlin/io/github/filderschoice/romcha/core/media/MediaListenerService.kt
      - core/media/src/main/kotlin/io/github/filderschoice/romcha/core/media/MediaMapping.kt
      - core/media/src/main/kotlin/io/github/filderschoice/romcha/core/media/PlaybackMonitor.kt
      - core/media/src/test/kotlin/io/github/filderschoice/romcha/core/media/MediaMappingTest.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/TrackMetadata.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:media の単体テスト 7 件成功
    関連ID:
      - BL-008
- date: 2026-09-27 17:30
  summary: YouTube URL から動画IDを取り出す VideoUrlParser を core:chat に実装
  details:
    変更内容: >-
      F-VID-04/05 に基づき、youtu.be / watch?v= / live/ / shorts/ 形式（www. / m. 付き、スキーム省略可）から動画IDを取り出す
      VideoUrlParser を追加した。共有テキスト中の URL にも対応し、ホストの完全一致と ID 形式で不正入力を除外する。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/VideoUrlParser.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/VideoUrlParserTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:chat の単体テスト 23 件成功
    関連ID:
      - BL-007
- date: 2026-09-27 17:15
  summary: リプレイの先読み取得とシーク時の再取得を行う ReplaySession を core:sync に実装
  details:
    変更内容: >-
      F-CHAT-02/03 と PLAN 4.5 に基づき、SyncEngine の取得要求を ReplayChatSource へ中継する ReplaySession を追加した。
      初回・シーク時は初期トークンと推定位置の 30 秒前から取り直し、続きは継続トークンで取得する。
      続きの取得は最小 1 秒間隔、失敗後は 10 秒の冷却期間を置く（K-04）。取得状態を StateFlow で公開する（F-CHAT-10）。
      仮想時間を使う単体テスト 7 件を追加した。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySession.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/ReplaySessionTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:sync の単体テスト 18 件成功
    関連ID:
      - BL-006
- date: 2026-09-27 16:45
  summary: InnerTube クライアント（continuation 取得、リプレイ／ライブ取得、指数バックオフ）を core:chat に実装
  details:
    変更内容: >-
      PLAN 4.4、F-CHAT-10、F-VID-07 に基づき、next 応答からタイトル・チャンネル名・チャットの continuation を読む WatchInfoParser と、
      get_live_chat_replay / get_live_chat を呼ぶ InnerTubeClient を追加した。通信断・429・5xx は指数バックオフ（1 秒から最大 30 秒）で再試行する。
      チャット無効・チャット欄の無い動画は Unavailable を返す。MockWebServer を使う単体テスト 10 件と next 応答の合成 fixture 4 件を追加した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClient.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/WatchInfoParser.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClientTest.kt
      - core/chat/src/test/resources/fixtures/next_replay.json
      - core/chat/src/test/resources/fixtures/next_live.json
      - core/chat/src/test/resources/fixtures/next_chat_disabled.json
      - core/chat/src/test/resources/fixtures/next_no_chat.json
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:chat の単体テスト 20 件成功
    関連ID:
      - BL-005
- date: 2026-09-27 16:10
  summary: チャットのメッセージモデルとリプレイ／ライブ応答の解析を core:chat に実装
  details:
    変更内容: >-
      F-CHAT-01・F-CHAT-08・N-08 に基づき、ChatMessage モデルと ChatResponseParser を追加した。
      通常・スーパーチャット・スーパーステッカー・メンバー加入・ギフトを解析し、未知の種別は読み飛ばして件数を返す。
      壊れた応答では例外を投げず Failure を返す。合成 fixture 3 件と単体テスト 10 件を追加した。
      detekt の ReturnCount をガード節除外・上限3へ調整し、参考にした方式の出典を docs/REFERENCES.md へ記録した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/ChatMessage.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/ChatResponseParser.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/internal/JsonNav.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/ChatResponseParserTest.kt
      - core/chat/src/test/resources/fixtures/replay_chunk.json
      - core/chat/src/test/resources/fixtures/live_chunk.json
      - core/chat/src/test/resources/fixtures/live_ended.json
      - config/detekt/detekt.yml
      - docs/REFERENCES.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:chat の単体テスト 10 件成功
    関連ID:
      - BL-004
- date: 2026-09-27 15:40
  summary: SyncEngine（位置推定・一時停止・シーク判定・速度追従・表示バッファ）を core:sync に実装
  details:
    変更内容: >-
      F-SYNC-03/04/05 と PLAN 4.5 に基づき、PlaybackSnapshot・PositionEstimator・SeekDetector・SyncEngine を追加した。
      SyncEngine は通信せず取得要求（FetchRequest）を返し、世代番号でシーク前の応答を破棄する。
      開発機のメモリ不足でビルドが落ちたため gradle.properties のヒープと並列数を下げ、Kotlin コンパイラをデーモン内実行にした。
      日本語のテスト名を許容するため、ktlint の関数命名規則をテストソースのみ無効化した。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/PlaybackSnapshot.kt
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/SeekDetector.kt
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/SyncEngine.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/SyncEngineTest.kt
      - gradle.properties
      - .editorconfig
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、core:sync の単体テスト 11 件成功
    関連ID:
      - BL-003
- date: 2026-09-27 15:10
  summary: Android プロジェクトの雛形と静的解析（ktlint・detekt・Android lint）を導入
  details:
    変更内容: >-
      Gradle Wrapper 8.13、バージョンカタログ、app / core:chat / core:sync / core:media / feature:overlay の5モジュール、
      .gitignore（署名鍵を除外）、LICENSE（MIT）、.editorconfig、detekt 設定、lint.xml（依存の新版警告のみ無効化）、
      アダプティブアイコン、data_extraction_rules を追加した。Android lint は warningsAsErrors で指摘0件を強制する。
      markdownlint の対象からビルド生成物（build 配下）を除外した。
    変更ファイル:
      - settings.gradle.kts
      - build.gradle.kts
      - gradle.properties
      - gradle/libs.versions.toml
      - gradle/wrapper/gradle-wrapper.jar
      - gradle/wrapper/gradle-wrapper.properties
      - gradlew
      - gradlew.bat
      - .editorconfig
      - .gitignore
      - LICENSE
      - lint.xml
      - .markdownlint-cli2.yaml
      - config/detekt/detekt.yml
      - app/build.gradle.kts
      - app/proguard-rules.pro
      - app/src/main/AndroidManifest.xml
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/res/values/strings.xml
      - app/src/main/res/xml/data_extraction_rules.xml
      - app/src/main/res/drawable/ic_launcher_background.xml
      - app/src/main/res/drawable/ic_launcher_foreground.xml
      - app/src/main/res/mipmap-anydpi/ic_launcher.xml
      - core/chat/build.gradle.kts
      - core/sync/build.gradle.kts
      - core/media/build.gradle.kts
      - feature/overlay/build.gradle.kts
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0、Android lint は No issues found
    関連ID:
      - BL-001
```

<!-- COPILOT_RECORDS:END -->
