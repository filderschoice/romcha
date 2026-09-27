<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->

```yaml
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
