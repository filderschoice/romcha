<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
- date: 2026-10-09 10:00
  summary: 設定のメニュー構成を見直し、歯車と三点メニューを備えた設定画面へ整理した
  details:
    変更内容: >-
      ホームを操作だけに絞り、トップバーに歯車（設定）と三点メニュー（診断情報・ライセンス）を追加。バックアップ・クラッシュ情報・
      キャッシュ消去・設定の初期化・アップデートを新しい SettingsScreen にグループ化したリスト形式で移し、診断情報は専用画面にした。
      表示設定は「表示する項目」「チャットの絞り込み」「フローティングウィンドウ」「テーマ」に分け、説明文を短くした。
    変更ファイル:
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/SettingsScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DiagnosticsScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/RomchaApp.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/ResetSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/UpdateSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/BackupSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/CrashReportingSection.kt
      - app/src/main/res/values/strings.xml
      - README.md
      - docs/VERIFICATION.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、npx markdownlint-cli2、
      python scripts/validate-records.py
    検証結果: >-
      成功 - 品質ゲートがすべて成功。画面の見た目の実機確認は行っていない
    関連ID:
      - BL-101
- date: 2026-10-08 20:00
  summary: 退避したつまみを内側へのスワイプで戻せない不具合を、システムのジェスチャー除外で直した
  details:
    変更内容: >-
      実機で、縦・横の画面の左右の端への退避は動くが、内側への adb swipe では復帰せず（タップと長押しドラッグでは復帰）、
      画面端の OS の戻るジェスチャーにスワイプを奪われていると推定した。StashTab に systemGestureExclusion を付けると、
      縦画面の左右・横画面の左のいずれでも内側へのスワイプで復帰した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/StashTab.kt
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、npx markdownlint-cli2、
      python scripts/validate-records.py、実機（Pixel 8 Pro）で縦横・左右の退避と復帰
    検証結果: >-
      成功 - 縦の左右、横の左右（右は先の確認）で退避したつまみが見え、スワイプとタップで復帰した
- date: 2026-10-08 18:00
  summary: 設定のバックアップが書き出されない不具合を、バックアップを全体方式に固定して直した
  details:
    変更内容: >-
      実機の bmgr で、バックアップが KeyValueBackupTask（キー値方式）で呼ばれ、RomchaBackupAgent の onFullBackup が使われず
      設定ファイルが書き出されないと分かった。マニフェストへ android:fullBackupOnly=true を付け、BackupRulesTest でも固定した。
      修正後は FullBackup で overlay.xml と display.xml が書き出され、復元で戻ること、キャッシュ（files）は戻らないこと、
      スイッチをオフにすると書き出されず（Transport rejected）復元もされないことを確認した。
    変更ファイル:
      - app/src/main/AndroidManifest.xml
      - app/src/test/kotlin/io/github/filderschoice/romcha/BackupRulesTest.kt
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、npx markdownlint-cli2、
      python scripts/validate-records.py、adb shell bmgr backupnow / restore（ローカル転送先）
    検証結果: >-
      成功 - Google アカウントへの実際のバックアップと、アンインストール・再インストールでの自動復元は BL-096（人手検証）で確認する
- date: 2026-10-08 16:00
  summary: 長い検索語で YouTube の検索が 0 件になり動画を特定できない不具合を、タイトルだけの再検索で直した
  details:
    変更内容: >-
      実機の診断情報ではタイトル・チャンネル・長さ（9342000 ms）は取れており、logcat で検索応答が backgroundPromoRenderer のみ
      （検索結果なし）と分かった。タイトルとチャンネル名をつなげた検索語では 0 件になるため、VideoResolver は関連する候補
      （点数が 0 より大きいもの）が無い時にタイトルだけで検索し直す。再検索が失敗しても最初の結果を使う。調査用に SearchResultParser と
      VideoResolver へ java.util.logging のログ（件数・点数・応答の項目種別のみ）を足した。再検索のテストを追加した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolver.kt
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/SearchResultParser.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolverTest.kt
      - CHANGELOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py、実機（Pixel 8 Pro）で再現と修正後の確認
    検証結果: >-
      成功 - 修正前は候補 0 件、修正後は候補 19 件・最高 100 点で自動確定
- date: 2026-10-08 14:00
  summary: 設定のバックアップを HOME 画面のスイッチで切り替えられるようにし、設定の初期化を追加した
  details:
    変更内容: >-
      BackupSettings（既定オン。保存先 backup_control は復元で上書きされないようバックアップ規則に含めない）と、オフの間は
      onFullBackup で何も書き出さない RomchaBackupAgent を追加し、マニフェストの backupAgent に指定した。HOME 画面へ
      BackupSection（スイッチ）と ResetSection（確認ダイアログ付きの初期化）を追加した。初期化はオーバーレイを止めてから
      OverlaySettingsReset で overlay の保存値を消し、表示設定を初期値へ戻す。BackupSettingsTest を追加し BackupRulesTest を更新した。
    変更ファイル:
      - app/src/main/AndroidManifest.xml
      - app/src/main/kotlin/io/github/filderschoice/romcha/backup/BackupSettings.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/backup/RomchaBackupAgent.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/BackupSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/ResetSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeActions.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/res/values/strings.xml
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlaySettingsReset.kt
      - app/src/test/kotlin/io/github/filderschoice/romcha/backup/BackupSettingsTest.kt
      - app/src/test/kotlin/io/github/filderschoice/romcha/BackupRulesTest.kt
      - README.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - スイッチ・初期化の実機での動作は BL-096（人手検証）に含めた
- date: 2026-10-08 12:00
  summary: アプリの設定を自動バックアップの対象にし、再インストール時に復元できるようにした
  details:
    変更内容: >-
      allowBackup を true にし、data_extraction_rules.xml を除外式から設定の SharedPreferences（overlay・display・crash_reporting）
      だけを include する式に改めた（クラウドバックアップと端末間移行の両方）。動画の特定結果のキャッシュは対象外のまま。
      対象を固定する BackupRulesTest を追加し、README・site のプライバシー欄・DESIGN・CHANGELOG を合わせた。
    変更ファイル:
      - app/src/main/AndroidManifest.xml
      - app/src/main/res/xml/data_extraction_rules.xml
      - app/src/test/kotlin/io/github/filderschoice/romcha/BackupRulesTest.kt
      - README.md
      - site/index.html
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 実機での復元の確認は BL-096（人手検証）
- date: 2026-10-08 10:00
  summary: 横画面で画面端へ退避したつまみが見えなくなる問題に対し、オーバーレイ窓を切り欠き領域にも描画するようにした
  details:
    変更内容: >-
      原因は、横画面では切り欠きやナビゲーションバーが画面の左右に来て、オーバーレイ窓の既定（layoutInDisplayCutoutMode=DEFAULT、
      fitInsetsTypes=システムバー）ではその領域を避けて配置・クリップされるためと推定した（実機では未確認）。
      OverlayWindow の LayoutParams に layoutInDisplayCutoutMode=ALWAYS と fitInsetsTypes=0 を設定した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      Gradle のゲートは成功。横画面での表示は実機でないと確認できず、BL-094 で人手検証とする
- date: 2026-10-05 11:00
  summary: v1.0.3 の公開に向けて版を上げ、site・README・DESIGN をクラッシュ情報の送信の内容へ更新した
  details:
    変更内容: >-
      versionName を 1.0.3、versionCode を 10003 へ上げた（署名ビルドの scripts/release-build が書き換えた値をそのまま取り込んだ）。
      site/index.html の版表記とプライバシー欄を、クラッシュ情報の送信（送信先 Firebase Crashlytics・収集項目・既定オン・
      HOME 画面のスイッチでオフ）を載せる内容へ改め、site/app.json の version と updated、README の状態、DESIGN の配布と版を合わせた。
      タグと Release の作成・push は人が行う。
    変更ファイル:
      - app/build.gradle.kts
      - site/index.html
      - site/app.json
      - README.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      scripts/release-build（ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test と署名ビルド。
      ユーザー実行）、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 署名ビルドの実機（Pixel 8 Pro）で v1.0.3 の動作を確認済み。site の表示はデプロイ後に人が確認する
    関連ID:
      - BL-092
- date: 2026-10-05 10:00
  summary: Gradle の依存グラフを GitHub へ提出するワークフローを追加した（dependency submission。ユーザー承認済み）
  details:
    変更内容: >-
      .github/workflows/dependency-submission.yml を新設した。main への push と手動実行で gradle/actions/dependency-submission を実行し、
      Dependabot alerts が Gradle の依存を検出できるようにする。ビルド・テスト・署名はせず、秘密情報も使わない（権限は contents write のみ）。
      CI 定義の変更は guardrails 12.2 の禁止範囲だが、BL-076 の確認質問でユーザーが導入を承認した。取り込みと表示の確認は BL-093 へ切り出した。
    変更ファイル:
      - .github/workflows/dependency-submission.yml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - ワークフローの実行確認は push 後に人が行う（BL-093）
    関連ID:
      - BL-076
      - BL-093
- date: 2026-10-04 19:10
  summary: クラッシュ情報の送信を既定オン・設定でオプトアウトにした（Crashlytics。json が無いビルドでは何もしない）
  details:
    変更内容: >-
      RomchaApplication を追加し、起動時に CrashReporting.init で保存済みの設定を Crashlytics の収集の有効・無効へ反映する。
      Manifest で firebase_crashlytics_collection_enabled=false として自動収集を止め、設定がオンの時だけ有効にする。
      設定値は CrashReportingSettings（BooleanStore 経由で SharedPreferences crash_reporting の enabled に永続化。既定 true）で扱い、
      HOME 画面に「クラッシュ情報の送信」のスイッチと送信内容の説明を追加した（BuildConfig.FIREBASE_ENABLED が false なら出さない）。
      CrashReportingSettingsTest で既定値・読み込み・永続化と反映を検証した。detekt の TooManyFunctions を避けるため
      セクションを CrashReportingSection.kt へ分けた。
    変更ファイル:
      - app/src/main/AndroidManifest.xml
      - app/src/main/kotlin/io/github/filderschoice/romcha/RomchaApplication.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/crash/CrashReporting.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/crash/CrashReportingSettings.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/CrashReportingSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/res/values/strings.xml
      - app/src/test/kotlin/io/github/filderschoice/romcha/crash/CrashReportingSettingsTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew --no-daemon ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test :app:assembleRelease、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 全件成功。Firebase 有効時の実際の送信・オフ時に送らないことは BL-091（人手検証）で確認する
    関連ID:
      - BL-089
- date: 2026-10-04 18:30
  summary: Firebase Crashlytics を Gradle へ組み込んだ（google-services.json が無いときは無効でビルドできる構成）
  details:
    変更内容: >-
      libs.versions.toml へ Firebase BoM（33.7.0）・crashlytics・google-services プラグイン（4.4.2）・crashlytics プラグイン（3.0.2）と
      androidx.fragment（1.8.5）を追加した。app/build.gradle.kts は app/google-services.json があるときだけ両プラグインを適用し、
      BuildConfig.FIREBASE_ENABLED で有無を実行時に判別できるようにした。google-services.json は .gitignore へ追加した。
      Analytics の依存は入れていない。Firebase が推移的に古い Fragment を引き込み lint の InvalidFragmentVersionForActivityResult が
      出たため、Fragment を明示した。
    変更ファイル:
      - .gitignore
      - build.gradle.kts
      - app/build.gradle.kts
      - gradle/libs.versions.toml
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew --no-daemon ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      ./gradlew --no-daemon :app:assembleRelease、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - google-services.json が無い状態で全ゲートと assembleRelease が成功。json ありの動作は BL-091（人手検証）で確認する
    関連ID:
      - BL-088
- date: 2026-10-04 17:30
  summary: HOME 画面を整理し、権限のたたみ表示とボタン名の区別を行った
  details:
    変更内容: >-
      権限がすべて許可済み（PermissionStatus.nextStep が null）なら、権限カード 3 枚を「すべて許可済みです」の 1 行にたたみ、
      見出しの横の「詳細を表示」「たたむ」で開閉する（開閉状態は rememberSaveable。未許可があれば従来どおり常に 3 項目を出す）。
      HOME の「終了」を「フローティング表示を終了」にし、省略せずに収まるよう開始・終了のボタンを全幅で縦に並べた。
      フローティングの歯車ボタンの名前（contentDescription）を「表示設定」から「ウィンドウの設定」にし、アプリの「表示設定」画面と区別した。
    変更ファイル:
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/res/values/strings.xml
      - feature/overlay/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 全件成功。たたみ表示の判定は既存の PermissionStatus.nextStep（PermissionStatusTest で検証済み）を使う。
      実機での表示は BL-082（人手検証）
    関連ID:
      - BL-080
- date: 2026-10-04 17:20
  summary: フローティングのヘッダーのボタンの大きさを 36dp と 48dp から表示設定で選べるようにした
  details:
    変更内容: >-
      DisplaySettings に largeHeaderButtons（既定 false。SharedPreferences display の largeHeaderButtons）を追加し、
      ヘッダーの設定・最小化・隠すのボタンの大きさを headerButtonSize（false で従来の 36dp、true で推奨の 48dp）で決めるようにした。
      アプリの表示設定画面に「フローティングウィンドウ」の見出しと「ヘッダーのボタンを大きくする」のスイッチを追加し、
      文字サイズ・背景の案内文をその見出しの下へ移した。既定値は従来の見た目を維持する（ユーザー判断で設定による切り替えとした）。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/DisplaySettings.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/HeaderButtonSizeTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 追加した HeaderButtonSizeTest を含め全件成功。実機での見た目は BL-082（人手検証）
    関連ID:
      - BL-081
- date: 2026-10-04 17:05
  summary: 表示設定画面のスイッチ・テーマの行を、行全体のタップで切り替えられるようにした
  details:
    変更内容: >-
      SwitchRow の行に toggleable（Role.Switch）を付けて Switch の onCheckedChange を null にし、テーマの行に selectable
      （Role.RadioButton。親に selectableGroup）を付けて RadioButton の onClick を null にした。行の高さの下限を 48dp にした。
      設定の値・保存先・既定値は変えていない。
    変更ファイル:
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/DisplaySettingsScreen.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 全件成功。Compose の UI テストは無いため、タップ領域と TalkBack の読み上げは BL-082（人手検証）で確かめる
    関連ID:
      - BL-079
- date: 2026-10-04 16:55
  summary: フローティングのチャット一覧が空の時に案内文を出すようにした
  details:
    変更内容: >-
      EmptyHint（NO_MESSAGES・FILTERED_OUT）と判定 EmptyHint.of を追加した。表示するメッセージが空で、動画を開いており（タイトルあり）
      お知らせ帯が無い時に、取得済みのメッセージがあれば「絞り込みの条件に合うチャットはありません」、無ければ
      「まだ表示するチャットはありません」を一覧の中央に副文字色で出す。判定は表示直前の絞り込み・件数の切り詰め後の一覧で行う。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormat.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/res/values/strings.xml
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayFormatTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 1 回目は detekt の TooManyFunctions（OverlayFormat の関数が上限 11 に達した）で失敗し、判定を EmptyHint の
      companion へ移して解消した。追加したテスト 3 件を含め全件成功。実機での見た目は BL-082（人手検証）
    関連ID:
      - BL-078
- date: 2026-10-04 16:40
  summary: フローティングのお知らせ帯を案内（中立色）と失敗（赤）で色分けした
  details:
    変更内容: >-
      お知らせ文を OverlayNotice（文と NoticeLevel の INFO・ERROR）にし、SessionMessages で種類を決めるようにした。
      読み込み中・特定中・未検出・候補の選択・画面オフで停止中・リプレイの準備待ち・再接続中は INFO、通信失敗・HTTP エラー・解析失敗・
      特定できない・チャット無効・リプレイ無しは ERROR。INFO の帯は OverlayColors.infoBand（暗色 #78909C・明色 #90A4AE、40%）、
      ERROR は従来の赤帯（#B71C1C、40%）。チャット無効の文は応答の文言を優先する処理を SessionMessages.chatUnavailable へ移した。
      文言そのものは変えていない。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayUiState.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionMessages.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/ChatPlayer.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayColors.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/SessionMessagesTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 追加した SessionMessagesTest 4 件と既存の WatchCoordinatorTest を含め全件成功。
      実機での見た目は未確認（人手検証。他の UI 改善とまとめて確認する）
    関連ID:
      - BL-077
- date: 2026-10-04 12:00
  summary: 実データの fixture から配信者・動画の情報を除き、作成スクリプトにも同じ置き換えを加えた
  details:
    変更内容: >-
      リポジトリの公開に向けた点検で、fixtures/real/ に配信者名・動画タイトル（ハッシュタグを含む）・チャンネルID・ハンドル・動画IDと、
      それらを内部に符号化した継続トークン等が残っていることを確認した（視聴者の情報は置き換え済み）。ユーザー判断により
      scripts/fetch-real-fixtures.py に OwnerAnonymizer を追加し、視聴者の置き換えの後に配信者名を「配信者N」、動画タイトルを
      「動画タイトルN」、チャンネルIDを UCchannel＋連番、ハンドルを /@channelN、動画IDを video＋連番、continuation・params・メッセージの id 等の
      不透明な値を「tokenN」へ置き換え、URL の pp クエリを削除する。保存前の検査に配信者の情報を加え、置き換え済みの値を元の値と
      みなさないようにして、置き換え済みの fixture を --raw-dir に指定しても同じ結果になるようにした。
      既存の fixture へは --raw-dir で通信せずに適用した。過去の履歴には元の値が残る（履歴は書き換えない）。
    変更ファイル:
      - scripts/fetch-real-fixtures.py
      - core/chat/src/test/resources/fixtures/real/live_chunk.json
      - core/chat/src/test/resources/fixtures/real/next_live.json
      - core/chat/src/test/resources/fixtures/real/next_replay.json
      - core/chat/src/test/resources/fixtures/real/replay_chunk.json
      - core/chat/src/test/resources/fixtures/real/search_results.json
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/RealResponseFixtureTest.kt
      - README.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      python scripts/fetch-real-fixtures.py --raw-dir core/chat/src/test/resources/fixtures/real（2回実行して結果が変わらないこと）、
      元の配信者名・タイトル・チャンネルID・動画ID と、それらの base64 符号化が出力に無いことの検索、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 元の値の残存は 0 件、2 回目の実行で差分なし。RealResponseFixtureTest 5 件を含め全件成功（スキップ 0 件）
    関連ID:
      - BL-075
- date: 2026-10-02 14:00
  summary: キャッシュを消した直後に、見ている動画の特定を自動でやり直すようにした
  details:
    変更内容: >-
      ユーザー指示により、「キャッシュを消す」を押した直後に見ている動画の特定をやり直すようにした。OverlayEvent に
      ResolutionCacheCleared を追加し、MainActivity がキャッシュを消した後に送る。WatchCoordinator はこれを受けて、手動で指定した動画
      （キャッシュを使わない）と再生を検出していない間を除き、resolveAndOpen をやり直す（実行中の処理は取り消す）。
      Toast と説明文の文言を「停止して開始し直す」案内から「見ている動画を特定し直す」へ改めた。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayUiState.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinator.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/session/WatchCoordinatorTest.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 消去後に検索で特定し直すテストと、手動指定の動画は変えないテストを含め全件成功。実機での確認は BL-073
    関連ID:
      - BL-073
- date: 2026-10-02 13:00
  summary: HOME 画面に動画特定のキャッシュを消すボタンを追加した
  details:
    変更内容: >-
      ユーザー指示により、誤特定が固定された時にユーザー自身がキャッシュを消せる契機を設けた。ResolutionCache に clear() を追加し
      （InMemoryResolutionCache はメモリを消す。PersistentResolutionCache はメモリとファイルを消す）、PersistentResolutionCache を
      プロセス内で共有する shared(context) 経由に変えて OverlayService と HOME 画面が同じメモリ上の内容を扱うようにした
      （ファイルだけ消しても動作中のサービスのメモリに残るため）。HOME 画面に「キャッシュを消す」ボタンと説明を追加し、
      押すと Toast で結果と「表示中の動画を直すにはオーバーレイを停止して開始し直す」ことを案内する。
      表示中の動画の自動やり直しは、既定値として行わない（識別キーが変わった時だけ特定する既存の仕様を維持）。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolver.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolverTest.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/session/PersistentResolutionCache.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeActions.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/res/values/strings.xml
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - キャッシュを消すと以後は検索して特定し直すテストを含め全件成功。実機での確認は BL-073 として人手検証へ登録した
    関連ID:
      - BL-073
- date: 2026-10-02 12:00
  summary: 長さが不明な（ライブ・プレミア中の）動画は特定のキャッシュを読み書きしないようにした
  details:
    変更内容: >-
      YouTube の同期で別の動画のチャットを取得することがある不具合の調査と修正。MediaSession に動画 ID は無く（Q-02）、特定は
      キャッシュ → 検索で決まる。キャッシュのキー（タイトル・チャンネル名・長さ）は、長さが 0 のライブ中は「タイトル・チャンネル名」だけになり、
      同名の過去・次回の配信と衝突する。キャッシュは永続化され期限が無いため、一度保存されると検索せずに別の動画で確定し続ける。
      VideoResolver で、長さが 0 以下の動画はキャッシュの読み出し・保存（自動確定・ユーザー選択・MediaSession の動画 ID 由来のすべて）を行わないようにした。
      ライブ中は毎回、手順3（配信中の動画との照合）か検索で特定する。既に保存済みの長さ 0 のキーは読まれなくなる（削除はしない）。
      実機での再現確認と、切り替え直後の識別キーの時間差は BL-071・BL-072 として人手検証へ登録した。
    変更ファイル:
      - core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolver.kt
      - core/chat/src/test/kotlin/io/github/filderschoice/romcha/core/chat/resolve/VideoResolverTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/PLAN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 追加した 2 件のテスト（キャッシュに同名の過去配信があっても今のライブ動画を特定する、長さ不明は保存しない）を含め全件成功
    関連ID:
      - BL-071
      - BL-072
- date: 2026-09-28 00:40
  summary: リリースビルドを 1 コマンドで行うスクリプト（scripts/release-build.bat・.ps1）を追加した
  details:
    変更内容: >-
      ユーザー指示により、sesami-wear の release-build.bat / .ps1 を参考に、Romcha のリリースビルド用スクリプトを追加した。
      .bat は pwsh を優先して .ps1 を呼ぶ。.ps1 は local.properties の署名情報とキーストアの事前確認（値は表示しない）、-VersionName 指定時の versionName と versionCode（MAJOR × 10000 + MINOR × 100 + PATCH）の
      書き換え（失敗時は元に戻す）、品質ゲートの Gradle 分、:app:releaseDist、apksigner による署名の確認、タグ・公開のコマンド例の
      表示を行う。push・タグ・公開は行わない。検証中に、local.properties を書き換えても lint の解析結果がビルドキャッシュから
      復元されて古い PropertyEscape の判定が残ることが分かったため、スクリプトでは :app:lintAnalyzeDebug を --rerun で実行する。
      PropertyEscape は大文字の C:/ を指摘し、小文字の c:/ は指摘しなかったため、ドライブ文字のエスケープはスクリプトでは確かめず lint に任せる。
      docs/RELEASE.md の記入例のドライブ文字を C\: にエスケープし、3.1 にスクリプトの使い方を追加した。README・DESIGN を更新した。
      参考の version.properties 方式は、版の正本を app/build.gradle.kts の 1 か所に保つため採らなかった。
    変更ファイル:
      - scripts/release-build.bat
      - scripts/release-build.ps1
      - docs/RELEASE.md
      - README.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      git worktree 上で scripts\release-build.bat を次の条件で実行（使い捨ての検証用キーストアと仮の local.properties。
      ユーザーの local.properties と実鍵は使っていない）。署名情報なし、版の形式違い（1.0）、-SkipChecks -AllowUnsigned、
      誤ったパスワードで -VersionName 1.0.2、エスケープ済みのパスで -VersionName 1.0.1（品質ゲート込み）、
      Windows PowerShell 5.1 での -SkipChecks。PowerShell の構文解析（pwsh・5.1）。
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 署名情報なし・形式違いは説明付きで終了コード1。未署名は romcha-v1.0.0-unsigned.apk を出力。誤ったパスワードでは
      Gradle が失敗し、版の書き換えが元に戻ることを確認。エスケープ済みのパスでは品質ゲートを通って署名済みの romcha-v1.0.1.apk を出力し、
      versionCode 10001・apksigner の証明書表示・次の手順の表示を確認。5.1 でも同じく成功。worktree と検証用キーストアは削除した
    関連ID:
      - BL-063
- date: 2026-09-27 23:40
  summary: リリース署名の情報を local.properties から読むようにした
  details:
    変更内容: >-
      ユーザー指示により、app/build.gradle.kts の署名情報の読み込み元を keystore.properties から local.properties（.gitignore 済み）の
      RELEASE_STORE_FILE・RELEASE_STORE_PASSWORD・RELEASE_KEY_ALIAS・RELEASE_KEY_PASSWORD へ変えた。RELEASE_STORE_FILE が無い・空なら
      未署名でビルドする（-unsigned）。相対パスはリポジトリのルート基準。docs/RELEASE.md（記入例、パス区切りの注意、Android Studio が
      local.properties を書き換えた場合の確認）・README・DESIGN を合わせた。.gitignore の keystore.properties は誤コミット防止のため残した。
    変更ファイル:
      - app/build.gradle.kts
      - README.md
      - docs/RELEASE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      git worktree 上で ./gradlew :app:releaseDist（local.properties 無し、および使い捨ての検証用キーストアを指す仮の local.properties の2通り）、
      apksigner verify --print-certs、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 無しでは romcha-v1.0.0-unsigned.apk、仮の設定では署名済みの romcha-v1.0.0.apk（apksigner で CN=test を確認）が出力された。
      ユーザーの local.properties（本番の署名情報）は値を読まず、ビルドにも使っていない（guardrails 12.5。実鍵での署名は BL-058）。
      worktree と検証用キーストアは削除した。品質ゲートは終了コード0
    関連ID:
      - BL-062
- date: 2026-09-27 23:00
  summary: アプリ紹介ポートフォリオ（site/）を追加した
  details:
    変更内容: >-
      ユーザー指示により、Romcha を紹介する静的な 1 ページを site/ に追加した（HTML と CSS のみ。JavaScript・外部フォント・
      外部 CDN を読み込まない）。機能・仕組み・使い方・プライバシーと免責・入手を載せ、画像は SVG の図解（アイコン・画面イメージ・
      構成図）で用意した。他アプリのテンプレートを兼ね、差し替え箇所に TEMPLATE コメントを付け、アプリごとの色を CSS 変数 3 つに
      集約した。repo 横断の一覧ページ向けに概要を site/app.json（app-portfolio.v1）に置き、使い方とキーを site/README.md に記載した。
      README からのリンクと DESIGN の設計を追加した。公開方法は未定（リポジトリ内に置くだけ）。
    変更ファイル:
      - site/index.html
      - site/style.css
      - site/app.json
      - site/README.md
      - site/assets/icon.svg
      - site/assets/screen.svg
      - site/assets/how-it-works.svg
      - README.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      Edge のヘッドレス表示によるスクリーンショット（1280px 幅のダーク・ライト、390px 幅の iframe）、
      python による app.json の JSON 検証、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - ライト・ダークとも表示が崩れず、390px 幅で 1 列になり横にはみ出さないことを確認した。
      品質ゲートは終了コード0。実機のスクリーンショットへの差し替えと公開方法の決定は BL-061 で人が行う
    関連ID:
      - BL-060
- date: 2026-09-27 22:05
  summary: v1.0.0 リリースの準備（版の更新、README のインストール手順、リリース手順書）
  details:
    変更内容: >-
      versionName を 1.0.0、versionCode を 10000（MAJOR × 10000 + MINOR × 100 + PATCH）へ上げた。
      docs/RELEASE.md を追加し、キーストアの作成とバックアップ、keystore.properties、版の決め方、:app:releaseDist と apksigner による
      署名の確認、実機での確認（デバッグ版は署名が違うためアンインストールが必要）、注釈付きタグと push、gh release create による公開、
      公開後の確認、ロールバックを人の作業として記載した。README に状態（v1.0.0）・インストール（Releases・SHA-256・Obtainium）・
      手順書へのリンクを追加し、VERIFICATION の対象アプリの版と DESIGN の実装制約を更新した。
    変更ファイル:
      - app/build.gradle.kts
      - docs/RELEASE.md
      - README.md
      - docs/VERIFICATION.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew :app:releaseDist、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - romcha-v1.0.0-unsigned.apk と .sha256 が出力された（署名鍵が無い環境のため未署名）。品質ゲートは終了コード0、
      Markdown は 26 ファイル（git 管理対象の件数と一致）で 0 件。署名ビルド・タグ・公開は BL-058 で人が行う
    関連ID:
      - BL-056
- date: 2026-09-27 21:45
  summary: アプリ画面に更新の確認（F-APP-02）を追加した
  details:
    変更内容: >-
      HOME に「アップデート」欄を追加し、「更新を確認」を押した時だけ GitHub Releases API（repos/filderschoice/romcha/releases/latest。
      認証なし）へ問い合わせるようにした（起動時の自動確認はしない。ユーザー判断）。tag_name を SemVer（AppVersion）で読み、
      現在の版より新しければ「ダウンロードページを開く」で固定のリリースページをブラウザーで開く（応答内の URL は開かない。
      自動インストールはしない）。未公開（404）・HTTP エラー・通信断・形式違いはそれぞれ説明文を出す。
      品質ゲートの単体テストに含めるため app モジュールの update パッケージに置き、app へ kotlinx-serialization-json と
      テスト用の coroutines-test・mockwebserver を追加した。README の使い方・プライバシー（通信先）と DESIGN を更新した。
    変更ファイル:
      - app/build.gradle.kts
      - app/src/main/kotlin/io/github/filderschoice/romcha/MainActivity.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/update/AppVersion.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/update/UpdateChecker.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/UpdateSection.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeActions.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/HomeScreen.kt
      - app/src/main/kotlin/io/github/filderschoice/romcha/ui/RomchaApp.kt
      - app/src/main/res/values/strings.xml
      - app/src/test/kotlin/io/github/filderschoice/romcha/update/AppVersionTest.kt
      - app/src/test/kotlin/io/github/filderschoice/romcha/update/UpdateCheckerTest.kt
      - README.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0（AppVersionTest 5 件・UpdateCheckerTest 8 件を含む。1〜2回目は detekt の
      DestructuringDeclarationWithTooManyEntries・MagicNumber、3回目は ktlint の空行で失敗し、名前付きグループと ktlintFormat で解消）。
      GitHub への実通信はしていない。実機での確認は BL-058 で行う
    関連ID:
      - BL-055
- date: 2026-09-27 21:10
  summary: リリース署名の設定と配布物（romcha-vX.Y.Z.apk と SHA-256）の出力タスクを追加した
  details:
    変更内容: >-
      PLAN 6章（R-08）の配布方針に合わせ、app/build.gradle.kts がルートの keystore.properties（.gitignore 済み）を読んで
      release を署名するようにした。ファイルが無ければ未署名でビルドする。:app:releaseDist を追加し、リリース APK を
      app/build/dist/romcha-vX.Y.Z.apk（未署名なら -unsigned を付ける）へ置き、SHA-256 を sha256sum 形式の .sha256 へ書き出す。
      README の開発手順と DESIGN の実装制約へ追記した。
    変更ファイル:
      - app/build.gradle.kts
      - README.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew :app:releaseDist（keystore.properties 無し、および使い捨ての検証用キーストアを置いた状態の2通り）、
      apksigner verify --print-certs、sha256sum、
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 未署名時は romcha-v0.1.0-unsigned.apk、署名時は romcha-v0.1.0.apk が出力され、署名時は apksigner で署名を確認した。
      .sha256 の値は sha256sum と一致した。検証用キーストアと keystore.properties は検証後に削除し、git の追跡対象外であることを確認した。
      品質ゲートは終了コード0
    関連ID:
      - BL-054
- date: 2026-09-27 20:38
  summary: フローティングの設定メニューからアプリ本体を開けるようにした
  details:
    変更内容: >-
      設定パネルの下部に「アプリを開く」を追加し、押すとアプリ本体の起動用インテント（FLAG_ACTIVITY_NEW_TASK）でアプリの画面を
      前面に出すようにした。フローティングウィンドウは表示したまま残す。オーバーレイを表示中のサービスからの起動のため、
      バックグラウンドからのアクティビティ起動制限の例外に当たる。配置はヘッダーではなく設定メニューを選んだ（BACKLOG の根拠に記録）。
      detekt の関数数上限のため、OverlayActions の onTouchThrough・onHide と新しい操作を onCommand(OverlayCommand) に統合し、
      起動処理はサービスの操作オブジェクト内に置いた。README の使い方と docs/VERIFICATION.md（G1）を更新した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayService.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/OverlayActions.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/ChatOverlay.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/ui/SettingsPanel.kt
      - feature/overlay/src/main/res/values/strings.xml
      - README.md
      - docs/VERIFICATION.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - 終了コード0（1回目は detekt TooManyFunctions で失敗し、起動処理を操作オブジェクト内へ移して解消）。
      実機での起動は BL-053 で確認する
    関連ID:
      - BL-052
- date: 2026-09-27 18:19
  summary: 画面端へ退避中のつまみの横幅を 20dp から 28dp に広げた
  details:
    変更内容: >-
      実機確認での指摘を受け、退避中のつまみ（WindowPlacement.TAB_WIDTH_DP）の横幅を 20dp から 28dp に広げた。高さ 72dp は維持した。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowPlacement.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実機での見え方は BL-048（F15）で確認する
    関連ID:
      - BL-051
- date: 2026-09-27 18:11
  summary: 退避のドラッグでウィンドウを画面の左右の外へも運べるようにした
  details:
    変更内容: >-
      実機確認で、外へ押し出すドラッグが画面幅で止まり退避している感じがしないとの指摘を受けた。
      ウィンドウに FLAG_LAYOUT_NO_LIMITS を付け、ヘッダーのドラッグ中は画面の左右の外へのはみ出しを許すようにした
      （画面内に 48dp は残す。縦は従来どおり画面内に収める）。離した時に「48dp とウィンドウ幅の 3 分の 1 の大きい方」以上
      はみ出していればその側へ退避し、そうでなければ画面内へ戻す。
      従来の「画面端で止まった後の押し込み量」による判定（移動中の未収め位置の保持）はやめ、はみ出し量で判定する
      （StashRule.dragX・overshoot・threshold を追加し単体テストを追加）。
    変更ファイル:
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowMode.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/WindowPlacement.kt
      - feature/overlay/src/main/kotlin/io/github/filderschoice/romcha/feature/overlay/OverlayWindow.kt
      - feature/overlay/src/test/kotlin/io/github/filderschoice/romcha/feature/overlay/StashRuleTest.kt
      - README.md
      - docs/VERIFICATION.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug compileDebugKotlin testDebugUnitTest :core:chat:test :core:sync:test、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - 終了コード0。実機での操作感は BL-048（F15）で確認する
    関連ID:
      - BL-050
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
