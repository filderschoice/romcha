# 実機検証手順書（人手検証）

BACKLOG の人手検証タスクのうち、未確認の項目（BL-024 の D6）を実機で行うための手順です。各表の「結果」欄へ
確認結果を記入し、最後の「結果の記録」に沿ってエージェントへ伝えてください。結果は BACKLOG へ反映します。
確認済みの項目は本書から削除しています（結果は `docs/records/managed/EXECUTE.md`・`CHANGELOG.md` と PLAN 4.2 に反映済み）。

| 項目 | 内容 |
| --- | --- |
| 対象端末 | Pixel 8 Pro（Android 17）。USB 接続（シリアル `39181FDJG008MY`） |
| 対象アプリ | Romcha デバッグ版 1.0.0（`io.github.filderschoice.romcha`、versionCode 10000） |
| 公式アプリ | YouTube 21.38.130（2026-09-27 時点の端末の版） |
| 所要時間の目安 | BL-024（D6）: 配信の終了に合わせて最大 30 分 |
| 最終実施日 | 2026/09/27 |

「結果」欄の記入値:

| 記入値 | 意味 |
| --- | --- |
| `OK` | 確認内容を満たした |
| `NG：<内容>` | 満たさなかった。起きたこと・表示された文言・操作の直前の状態を書く |
| `未実施` | まだ行っていない（初期値） |
| `対象外：<理由>` | 条件が合わず行えない（例 終了まで見届けられる配信が無かったため D6 は対象外） |

本書は常に最新の状態だけを持ちます。確認し直した項目は結果欄を上書きし、過去の結果や経緯は残しません
（修正の経緯は `docs/records/managed/EXECUTE.md`、未解決の NG は `docs/records/managed/BACKLOG.md` で追います）。
アプリを修正して再インストールした場合、その修正に関わる項目は `未実施` に戻して確認し直します。

## 0. 準備

### 0.1 インストール（済みの場合は不要）

```sh
# 端末が複数見える場合（USB と Wi-Fi の二重表示を含む）はシリアルを必ず指定する
adb devices -l
# PowerShell
$env:ANDROID_SERIAL = "39181FDJG008MY"; .\gradlew.bat :app:installDebug
# 確認: lastUpdateTime がインストールした時刻になっていること
adb -s 39181FDJG008MY shell dumpsys package io.github.filderschoice.romcha | findstr "versionName lastUpdateTime"
```

### 0.2 権限の許可（許可済みの場合は不要）

1. Romcha を起動する。「必要な権限」に 3 項目が並び、未許可の最初の項目が強調されていることを確認する。
2. 「他のアプリの上に重ねて表示」の「設定を開く」→ Romcha を許可 → 戻る。
3. 「通知へのアクセス」の「設定を開く」→ Romcha を許可 → 戻る。
   - 許可のスイッチが押せず「制限付き設定」と表示された場合は、設定 → アプリ → Romcha → 右上のメニュー →
     「制限付き設定を許可」を行ってから再度許可する（adb で入れた場合は通常出ないが、APK を手動で入れた場合に出る）。
     出た場合は結果の記録に書く（配布時の README へ案内を追加するため）。
4. 「通知の表示」の「設定を開く」→ 許可する。
5. 3 項目すべてが「許可済み」になることを確認する。

確認コマンド（任意）:

```sh
# 通知へのアクセス: 出力に io.github.filderschoice.romcha/...MediaListenerService が含まれれば許可済み
adb -s 39181FDJG008MY shell settings get secure enabled_notification_listeners
# オーバーレイ: SYSTEM_ALERT_WINDOW: allow なら許可済み
adb -s 39181FDJG008MY shell appops get io.github.filderschoice.romcha SYSTEM_ALERT_WINDOW
```

### 0.3 記録の取り方

- 画面の記録: `adb -s 39181FDJG008MY shell screencap -p /sdcard/shot.png` の後に
  `adb -s 39181FDJG008MY pull /sdcard/shot.png shot-<番号>.png`（手元の端末でスクリーンショットでもよい。
  PowerShell で `exec-out ... >` とリダイレクトすると版によって画像が壊れるため使わない）
- 診断情報: Romcha の「診断情報を表示」の文字列は長押しで選択・コピーできる。メモアプリ等に貼って保存する。
- 動画のタイトル・チャンネル名・URL は、検証に使った動画を後から特定できるように控える。

## 1. BL-024: ライブ・プレミア終了時のリプレイへの切り替え

配信（またはプレミア公開）の終了時刻に合わせて行います。追従表示・遅延設定・一時停止・待機中（D1〜D5）は確認済みです。

| 番号 | 操作 | 確認 | 結果 |
| --- | --- | --- | --- |
| D6 | 配信（またはプレミア）の終了まで開いたままにする | 「配信は終了しました。チャットのリプレイの準備を待っています（n 回目）」が出て、準備でき次第リプレイの同期表示へ切り替わる（最大約 18 分待つ）（F-CHAT-06） | 未実施 |

## 2. 結果の記録

1 章の表の「結果」欄と、冒頭の「最終実施日」を記入して保存し、エージェントへ「VERIFICATION の結果を記入した」と
伝えてください（途中までの記入でも構いません。`未実施` の項目は次回に回します）。スクリーンショット・診断情報は
ファイル名または貼り付けで添えると判定が速くなります。表に収まらない気付き（操作しにくい点・見えにくい配色など）は、
次の「その他」へ書きます。

| その他の気付き |
| --- |
| （未記入） |

エージェントは記入内容を BACKLOG（BL-024 の完了判定、NG の起票）へ反映します。

## 3. 不具合時に取得すると調査が速い情報

アプリはチャットの内容や個人に関わる情報をログへ出しません（N-07）。落ちた場合は次で原因を取れます。

```sh
# 直前のクラッシュ（アプリが落ちた場合）
adb -s 39181FDJG008MY logcat -d -b crash | findstr /i romcha
# オーバーレイ・通知アクセスの状態
adb -s 39181FDJG008MY shell dumpsys activity services io.github.filderschoice.romcha | findstr /i "ServiceRecord isForeground"
```
