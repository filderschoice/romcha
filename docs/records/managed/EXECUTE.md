<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- date: 2026-09-27 17:13
  summary: フローティングウィンドウを画面の左右の外へスワイプして退避し、つまみから復帰できるようにした
  details:
    変更内容: >-
      YouTube 公式アプリの PiP と同様に、ヘッダーのドラッグでウィンドウを画面の左右の端でさらに 48dp 以上押し込んで離すと、
      その側の画面端へ退避し、20×72dp のつまみだけを残すようにした。つまみを画面の内側へ 24dp 以上スワイプするか、タップすると、
      退避した側の画面端に寄せた通常表示で復帰する。つまみは上下にドラッグして動かせる。
      表示状態（通常・最小化・退避）を WindowMode にまとめ、OverlayActions の onMinimizeChange を onWindowModeChange に置き換えた
      （detekt の関数数上限のため）。退避の判定（押し込み量・復帰のスワイプ量・つまみの位置）は StashRule（Android 非依存）に置き、
      単体テストを追加した。WindowPlacement は移動中の画面内へ収める前の横位置を保持して押し込み量を求める。
      退避の向きは左右のみ、退避状態は保存しない（BACKLOG の根拠に記録した既定値）。
      README の使い方と docs/VERIFICATION.md（F15）を更新した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowMode.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowPlacement.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/StashTab.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/Bubble.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/res/values/strings.xml
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/StashRuleTest.kt
      - README.md
      - docs/VERIFICATION.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0（1回目は detekt LongParameterList と MaxLineLength で失敗し修正）。
      実機での操作感は BL-048（F15）で確認する
    関連ID:
      - BL-049
- date: 2026-09-27 16:24
  summary: ライト／ダーク／システム追従のテーマを追加し、アプリ画面とフローティングウィンドウへ反映
  details:
    変更内容: >-
      DisplaySettings に theme（ThemeMode。SYSTEM・LIGHT・DARK、既定 SYSTEM）を追加し、表示設定画面にラジオボタンを置いた。
      ユーザー回答（2026-09-27）により、既定は現状維持とした。アプリ画面は SYSTEM ならシステムの設定に合わせ（従来どおり）、
      フローティングウィンドウは SYSTEM・DARK なら従来の暗色、LIGHT なら明るい配色にする。
      フローティングの配色を OverlayColors（Dark / Light）と CompositionLocal にまとめ、既存の OverlayTextColor などは
      CompositionLocal を読むプロパティに置き換えた（呼び出し側は変更なし）。投稿者の役割の色も配色ごとに持つ。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/DisplaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayColors.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatItems.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/ChatFilterTest.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayColorsTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/RomchaApp.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。ライト配色の見え方は BL-048 で確認する
    関連ID:
      - BL-047
- date: 2026-09-27 16:20
  summary: URL 入力欄に「クリップボードから貼り付け」を追加（押した時だけクリップボードを読む）
  details:
    変更内容: >-
      F-VID-06 の実現方法として、ユーザー回答（2026-09-27）により、アプリを開いた時の自動検出ではなく、URL 入力欄の
      「クリップボードから貼り付け」ボタンを押した時だけクリップボードを読む方式にした（Android 12 以降の貼り付け通知が毎回出ることと、
      無関係な内容を勝手に読まないため）。読んだ文字列を入力欄へ入れ、YouTube の動画 URL として読めなければ入力欄に誤りを表示する。
    変更ファイル:
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0
    関連ID:
      - BL-041
- date: 2026-09-27 15:44
  summary: 表示設定に「上位のチャットのみ」を追加し、「すべてのチャット」との切り替えに対応
  details:
    変更内容: >-
      DisplaySettings に topChatOnly（既定オフ＝従来どおり「すべてのチャット」）を追加し、表示設定画面にスイッチを置いた。
      オンの時は動画情報の「上位チャット」の continuation をそのまま使い、「すべてのチャット」への切り替え用の取得を行わない。
      ChatPlayer の whileScreenOn を画面のオン・オフと設定の組で transformLatest するように変え、設定が変わったら取得をやり直す。
      SessionSettings に topChatOnly を追加した。切り替えの単体テストを追加した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/DisplaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionEnvironment.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0（1回目は追加したテストの前提不足で失敗し、テストに再生位置を与えて解消）
    関連ID:
      - BL-046
- date: 2026-09-27 15:40
  summary: 表示保持件数の上限を表示設定で変えられるようにした
  details:
    変更内容: >-
      DisplaySettings に表示保持件数の上限 maxVisible（100〜1000、100 刻み、既定 500。N-04）を追加し、表示設定画面にスライダーを置いた。
      上限はセッションの開始時にリプレイの SyncConfig.maxVisible・ライブの LivePolling.maxMessages と LiveTimeline.visible へ渡し、
      上限を下げた時はウィンドウの表示直前でも切り詰めてすぐ反映する（増やした分は次に動画を開いた時から）。
      detekt の引数数上限に達したため、SessionEnvironment の設定値（表示遅延・同期の補正・表示保持件数）を SessionSettings にまとめた。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/DisplaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionEnvironment.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/ChatFilterTest.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0（1回目は detekt LongParameterList で失敗し、SessionSettings への統合で解消）
    関連ID:
      - BL-045
- date: 2026-09-27 15:36
  summary: 表示設定に NG ワードと「スパチャのみ」「メンバーのみ」「モデレーター・配信者のみ」の絞り込みを追加
  details:
    変更内容: >-
      ChatFilter（Android 非依存）を追加した。3 つの「のみ」はオンにしたもののいずれかに当てはまるメッセージだけを出し
      （どれもオフなら絞り込まない）、NG ワードを本文に含むメッセージは大文字・小文字を区別せず除く。
      メンバーはメンバーの投稿とメンバー加入・ギフト、モデレーター・配信者は MODERATOR / OWNER の役割で判定する。
      NG ワードは 1 行 1 語で入力し「保存」で取り込む（空行・重複を除き、最大 100 語・1 語 50 字）。
      DisplaySettings に項目を追加して display に保存し、フローティングウィンドウは表示直前に絞り込む。単体テストを追加した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ChatFilter.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/DisplaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/ChatFilterTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0
    関連ID:
      - BL-044
- date: 2026-09-27 15:32
  summary: アプリ画面に表示設定を追加し、投稿者名・アイコン・時刻の表示有無を切り替えられるようにした
  details:
    変更内容: >-
      feature:overlay に DisplaySettings（投稿者名・アイコン・時刻の表示有無）と DisplaySettingsStore（SharedPreferences display に保存し、
      同一プロセスのアプリ画面とフローティングウィンドウが同じ StateFlow を購読する）を追加した。既定値は従来の表示（名前のみ）。
      アプリのホームに「表示設定」ボタンと DisplaySettingsScreen（スイッチ）を追加し、変更は表示中のウィンドウへすぐ反映する。
      時刻はリプレイなら動画内の位置、ライブ・プレミアなら投稿時刻（H:mm）を出す（OverlayFormat.messageTime）。
      アイコンは ImagePolicy を通した URL だけ 18dp の丸で表示する。スーパーチャットの帯の投稿者名は設定によらず出す。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/DisplaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatItems.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/RomchaApp.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実機での見た目は BL-048 で確認する
    関連ID:
      - BL-043
- date: 2026-09-27 15:28
  summary: カスタム絵文字・メンバースタンプ・スーパーステッカーを画像で表示（Coil を導入）
  details:
    変更内容: >-
      画像読み込みに Coil 2.7.0（coil-compose。Apache-2.0、PLAN 4.7 の採用候補）を追加した。
      メッセージ本文を MessageText で描き、カスタム絵文字・メンバースタンプを文中へ画像（1.4em）で差し込む。
      画像の URL が無い・許可していない配信元・読み込み失敗の時は代替テキスト（:name: など）を出し、Unicode の絵文字は文字のまま出す。
      スーパーステッカーは 56dp の画像で表示し、表示できない時は従来の「（スーパーステッカー）」を出す。
      応答の URL は外部入力のため、ImagePolicy で HTTPS かつ YouTube の画像配信元（*.ggpht.com・*.ytimg.com・*.googleusercontent.com）に
      限って読み込む（N-05）。README のプライバシー節の通信先を更新した。
    変更ファイル:
      - gradle/libs.versions.toml
      - feature/overlay/build.gradle.kts
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ImagePolicy.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/MessageText.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatItems.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/ImagePolicyTest.kt
      - README.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実際の絵文字画像の表示は BL-048 で確認する
    関連ID:
      - BL-042
- date: 2026-09-27 15:22
  summary: 手動タイマーモード（開始・停止・位置入力）と同期状態「手動」を追加
  details:
    変更内容: >-
      core:sync に ManualTimer（再生状態 PlaybackSnapshot を開始・停止・位置入力で作る。等速）を追加した。
      設定パネル（リプレイ時）に「手動タイマー」を追加し、オンにすると表示中の位置（同期の補正を除く）で停止した状態から始め、
      「開始／停止」「位置を入力」（h:mm:ss・m:ss・秒数。全角コロン可）で操作する。手動中はリプレイの同期に公式アプリの再生状態の
      代わりに手動タイマーを使い、同期状態を「手動」（SyncIndicator.MANUAL）と表示し、「再生を検出していません」の表示を出さない。
      ライブ・プレミアは受信時刻で表示するため手動タイマーの対象外とした。
      ウィンドウは通常 FLAG_NOT_FOCUSABLE のため、位置の入力中だけフォーカスを取れるようにし（OverlayWindow.focusable）、
      確定・取消で元に戻す。設定パネルは項目が増えたため高さ 200dp を上限にスクロールさせる。
      手動タイマーの状態は保存しない。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/ManualTimer.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/ManualTimerTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ManualControl.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayUiState.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionEnvironment.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ManualPanel.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/SettingsPanel.kt
      - feature/overlay/src/main/res/values/strings.xml
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0（1回目は ktlint の行長と detekt ReturnCount で失敗し修正）。
      入力欄でキーボードが出るかは BL-048 で確認する
    関連ID:
      - BL-040
- date: 2026-09-27 15:16
  summary: リプレイの同期オフセットの手動補正を追加し、設定パネルの値を OverlaySettings にまとめた
  details:
    変更内容: >-
      core:sync に SyncOffset（±10 秒・0.5 秒刻み。PLAN 4.5 の「推定位置 + 手動補正」で、正の値でチャットを早く表示）を追加し、
      ChatPlayer がリプレイの同期に使う再生状態へ補正を足すようにした。補正値は SessionEnvironment.syncOffsetMs で渡す（既定 0）。
      設定パネルでは、ライブ・プレミア中は従来の表示遅延、それ以外は補正のスライダー（「補正 +1.5 秒」）を出す。
      補正は SharedPreferences overlay の syncOffsetMs に保存し、次回も引き継ぐ（端末の遅れは動画によらず一定のことが多いため）。
      detekt の関数数上限に達したため、設定パネルの値（不透明度・文字サイズ・表示遅延・補正）を OverlaySettings データクラスへまとめ、
      OverlayActions の個別の変更通知を onSettingsChange の1つに統合した。保存キーは従来のまま。
    変更ファイル:
      - core/sync/src/main/kotlin/io/github/filderschoice/romcha/core/sync/SyncOffset.kt
      - core/sync/src/test/kotlin/io/github/filderschoice/romcha/core/sync/SyncOffsetTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayPrefs.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionEnvironment.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/SettingsPanel.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/res/values/strings.xml
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlaySettingsTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0（1回目は detekt TooManyFunctions で失敗し、OverlaySettings への統合で解消）。
      実機での補正の効き方は BL-048 で確認する
    関連ID:
      - BL-039
- date: 2026-09-27 15:11
  summary: フローティングウィンドウの最小化（バブル）と復帰を追加
  details:
    変更内容: >-
      ヘッダーに最小化ボタンを追加した。最小化中はウィンドウを 48dp の丸いバブルにし、タップで元の大きさに戻し、ドラッグで移動する。
      バブルは通常表示の左上の位置に出し、バブル自身の大きさで画面内へ収める（移動は通常表示の位置にも反映し、復帰時に収め直す）。
      縁の色で同期状態を示す（同期中・ライブは緑）。背景の不透明度が低くても見失わないよう、バブルの不透明度には下限 0.7 を置く。
      最小化状態は保存しない。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowPlacement.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/Bubble.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実機での操作感は BL-048 で確認する
    関連ID:
      - BL-038
- date: 2026-09-27 15:07
  summary: タッチ透過モードを追加し、解除を常駐通知から行えるようにした
  details:
    変更内容: >-
      設定パネルに「タッチ透過にする」を追加した。透過中はウィンドウに FLAG_NOT_TOUCHABLE を付け、他アプリのオーバーレイ越しの
      タッチが遮断されないようウィンドウの不透明度（LayoutParams.alpha）を 0.8 に下げる（PLAN 4.6）。
      透過中はウィンドウを触れないため、常駐通知に「タッチ透過を解除」の操作を追加し、ヘッダーに「タッチ透過中」と表示する。
      透過モードは保存せず、サービスの起動ごとに解除した状態から始める。
      detekt の関数数上限に達したため、位置と大きさの計算を WindowPlacement へ、設定パネルの部品を ui/SettingsPanel.kt へ切り出した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowPlacement.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayNotifications.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/SettingsPanel.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0（1回目は detekt TooManyFunctions で失敗し、切り出しで解消）。
      下のアプリを実際に操作できるかは BL-048 で確認する
    関連ID:
      - BL-037
- date: 2026-09-27 15:03
  summary: フローティングウィンドウの位置・大きさを画面の縦横それぞれで記憶
  details:
    変更内容: >-
      OverlayPrefs の位置・大きさを画面の向き（ScreenOrientation。幅＞高さで横）ごとに保存するようにした。
      縦は従来のキーを使い、既存の保存値を引き継ぐ。横は landscape. を前置したキーに保存する。
      サービスの onConfigurationChanged で向きの変化を検知し、その向きの保存値へ切り替える。
      detekt の関数数上限に達したため、ウィンドウの追加・削除・位置と大きさの管理を OverlayWindow クラスへ切り出した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayPrefs.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0（1回目は detekt TooManyFunctions で失敗し、OverlayWindow への切り出しで解消）。回転時の動作は BL-048 で確認する
    関連ID:
      - BL-036
- date: 2026-09-27 15:00
  summary: フローティングのヘッダーの色味をチャット欄と分け、不透明度を両方に反映
  details:
    変更内容: >-
      ヘッダー（ドラッグで移動する領域）の背景を青みの灰色（#37474F）、チャット欄を黒にし、ドラッグできる範囲を見分けやすくした。
      不透明度はウィンドウ全体ではなくヘッダーとチャット欄それぞれの背景に掛け、スライダーが両方に効くようにした。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実機での見え方の確認は BL-048（人手検証）で行う
    関連ID:
      - BL-035
- date: 2026-09-27 14:57
  summary: フローティングの設定パネルにチャットの文字サイズのスライダーを追加
  details:
    変更内容: >-
      設定パネルに文字サイズのスライダー（80〜150%、10% 刻み、既定 100%＝従来の大きさ）を追加した。
      チャット欄だけを LocalDensity の fontScale に倍率を掛けて拡大・縮小する（端末の文字サイズ設定に掛け合わせる）。
      倍率は SharedPreferences overlay の fontScale に保存し、OverlayFormat.clampFontScale で範囲と刻みに揃える。単体テストを追加した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayPrefs.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/res/values/strings.xml
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実機での見た目の確認は BL-048（人手検証）で行う
    関連ID:
      - BL-034
- date: 2026-09-27 14:36
  summary: 常駐通知の本文の動画タイトルが動画の切り替えに追従しない問題を修正
  details:
    変更内容: >-
      OverlayService は常駐通知を onStartCommand と setVisible でしか作り直しておらず、動画を切り替えても通知の本文が
      最初の動画のタイトルのままだった。OverlayNotifications.titleChanges（OverlayUiState の title の変化だけを流す）を追加し、
      OverlayService が OverlayChannel.state から購読して、タイトルが変わったときだけ通知を出し直すようにした。
      通知の出し直しを updateNotification にまとめた。単体テスト OverlayNotificationsTest を追加した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayNotifications.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayNotificationsTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      ./gradlew :app:installDebug、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0、実機へのインストールを確認。実機で動画を切り替えたときの通知の追従は未確認
      （フローティング表示の開始を自動操作で行えなかったため、人手で確認する）
    関連ID:
      - BL-033
- date: 2026-09-27 14:14
  summary: 実応答由来の fixture を追加し、fixture 作成スクリプトの IP アドレス混入を修正
  details:
    変更内容: >-
      scripts/fetch-real-fixtures.py を実応答（アーカイブ 9mQ2ioeay4I と検索の絞り込み「ライブ」の先頭の配信）で実行し、
      core/chat/src/test/resources/fixtures/real に 5 ファイルを追加した（投稿者 35 人・コメント 72 件を置き換え済み）。
      実行時の点検で、検索結果の watchEndpointSupportedOnesieConfig に取得した端末の IP アドレスを含む動画配信サーバーの
      署名付き URL が残ることが分かったため、同キーを削除対象に加え、googlevideo.com が出力に残る場合は保存を中止する検査を追加した。
    変更ファイル:
      - scripts/fetch-real-fixtures.py
      - core/chat/src/test/resources/fixtures/real/next_replay.json
      - core/chat/src/test/resources/fixtures/real/replay_chunk.json
      - core/chat/src/test/resources/fixtures/real/search_results.json
      - core/chat/src/test/resources/fixtures/real/next_live.json
      - core/chat/src/test/resources/fixtures/real/live_chunk.json
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      python scripts/fetch-real-fixtures.py --replay 9mQ2ioeay4I、出力の URL ホスト・ip= パラメータ・投稿者名の点検、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - RealResponseFixtureTest 5 件が実行され成功（スキップ 0）。出力の URL は example.invalid と youtube.com 内のパスのみで、
      ip= パラメータ・元の投稿者名は残っていない。品質ゲートは終了コード0
    関連ID:
      - BL-022
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
