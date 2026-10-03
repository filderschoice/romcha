# リリース手順書

署名済みリリース APK を GitHub Releases へ公開する手順です（PLAN 6章）。方針の正本は
[`PLAN.md`](PLAN.md)「6. 配布」で、本書はその手順だけを扱います。

鍵の作成・保管、署名ビルド（鍵とパスワードを使う）、タグの push は**人が行います**。エージェントは鍵とパスワードを扱わず、
`git push` も実行しません（`rules/guardrails-unified.v1.md` 5.3・12.2・12.5）。
Releases の公開（6章の `gh release create`）は、**ユーザーが明示的に指示した場合に限り**、エージェントが実行してよいものとします
（2026-10-04 ユーザー判断。`--verify-tag` で push 済みのタグを指定し、タグは作らせない）。自律ループ実行モード内では、
外部への発信にあたるため実行しません（12.2）。ループ外の対話で、版・リリースノートを示したうえで指示を受けて行います。

| 項目 | 内容 |
| --- | --- |
| 配布物 | `romcha-vX.Y.Z.apk`（署名済み）と `romcha-vX.Y.Z.apk.sha256` |
| タグ | `vX.Y.Z`（注釈付きタグ） |
| 版 | `app/build.gradle.kts` の `versionName`（SemVer）と `versionCode` |
| 更新通知 | アプリの「更新を確認」が GitHub の最新リリース（`releases/latest`）のタグと `versionName` を比べる |

## 1. リリース用キーストアの作成（初回のみ）

リポジトリの外（例 `%USERPROFILE%\keys\romcha-release.jks`）に作成します。**紛失すると同じアプリとして上書き
更新できなくなり、漏洩すると第三者が更新版を装えます**（PLAN K-08）。

```sh
keytool -genkeypair -v -keystore <リポジトリ外のパス>/romcha-release.jks -alias romcha \
  -keyalg RSA -keysize 4096 -validity 10000
```

- パスワードはパスワードマネージャー等へ保管し、チャット・Issue・コミットへ貼らないでください。
- キーストアとパスワードは別の場所（暗号化した外部ストレージ等）へバックアップします。

次に、リポジトリのルートの `local.properties`（`.gitignore` で除外済み。無ければ作る）へ署名情報を追記します。

```properties
RELEASE_STORE_FILE=C\:/Users/<ユーザー名>/keys/romcha-release.jks
RELEASE_STORE_PASSWORD=<キーストアのパスワード>
RELEASE_KEY_ALIAS=romcha
RELEASE_KEY_PASSWORD=<鍵のパスワード>
```

- パスの区切りは `/` を使います（`\` は properties 形式ではエスケープ文字のため、使うなら `\\` と重ねる）。
  ドライブ文字の `:` は `C\:` とエスケープします（`C:/` のように書くと Android lint の PropertyEscape がエラーにし、
  品質ゲートの `lintDebug` が通りません。小文字の `c:/` は指摘されないことを確認していますが、エスケープしておけば確実です）。
  相対パスはリポジトリのルートから解決します。
- 作成後に `git status --short` で `local.properties` と `*.jks` が表示されない（追跡対象外）ことを確認します。
- Android Studio が `sdk.dir` を書き換えることがあるため、追記した行が残っているかを署名ビルドの前に確かめます
  （消えていると未署名の `-unsigned` になります）。

## 2. 版を決める

`app/build.gradle.kts` の `defaultConfig` を更新します。

- `versionName`: SemVer（`MAJOR.MINOR.PATCH`）。タグ `vX.Y.Z` と一致させる。
- `versionCode`: `MAJOR × 10000 + MINOR × 100 + PATCH`（v1.0.0 は `10000`、v1.0.1 は `10001`）。単調増加でなければ上書き
  インストールできないため、公開済みの版より必ず大きくする。MINOR・PATCH は 0〜99 の範囲で運用する。

版の変更は通常の作業ブランチでコミットし、Pull Request でマージします。タグは規定ブランチへマージした後の
コミットに打ちます。

## 3. 署名済み APK を作る

### 3.1 スクリプトで作る（推奨）

2章の版の更新から、この章の署名の確認までを 1 コマンドで行います（Windows）。

```bat
rem 現在の版（app/build.gradle.kts）でビルドする
scripts\release-build.bat

rem 版を上げてビルドする（versionName と versionCode を書き換える。成功したら変更をコミットする）
scripts\release-build.bat -VersionName 1.0.1
```

| 順 | スクリプトが行うこと | 止まる条件 |
| --- | --- | --- |
| 1 | `local.properties` の署名情報 4 つとキーストアのファイルを確認する（値は表示しない） | 不足・ファイルが無い |
| 2 | `-VersionName` の指定時だけ、`versionName` と `versionCode`（2章の式）を書き換える | 形式違い（`MAJOR.MINOR.PATCH`、MINOR・PATCH は 0〜99） |
| 3 | 品質ゲートの Gradle 分（静的解析・型検査・単体テスト）を実行する | 失敗（版の書き換えは元に戻す） |
| 4 | `:app:releaseDist` で APK と `.sha256` を出力する | 失敗（版の書き換えは元に戻す） |
| 5 | `apksigner` で署名を確かめ、証明書の DN と SHA-256 を表示する | 検証の失敗 |
| 6 | 実機への導入・タグ・公開のコマンド例を表示する（実行はしない） | － |

- 既存の最新タグ以下の版を指定した場合や、コミットしていない変更がある場合は警告を出します（止めません）。
- オプション: `-SkipChecks`（品質ゲートを省略）、`-AllowUnsigned`（署名情報が無くても未署名でビルドする。動作確認用で、公開しない）。
- `local.properties` を書き換えた後も lint の結果がビルドキャッシュから復元されて古いままになることがあるため、
  スクリプトは app の lint の解析（`:app:lintAnalyzeDebug`）を毎回やり直します。
- 実体は `scripts/release-build.ps1` で、`.bat` は pwsh（PowerShell 7）があればそれで、無ければ Windows PowerShell で実行します。

### 3.2 手動で作る

```sh
# 品質ゲート（CLAUDE.md「本リポジトリの品質ゲート定義」）を通してから
./gradlew :app:releaseDist
```

`app/build/dist/` に `romcha-vX.Y.Z.apk` と `romcha-vX.Y.Z.apk.sha256` ができます。ファイル名に `-unsigned` が
付いている場合は `local.properties` の `RELEASE_STORE_FILE` を読めていません（未署名の APK は公開しないでください）。

署名を確かめます（`apksigner` は Android SDK の `build-tools/<版>/` にあります）。

```sh
apksigner verify --print-certs app/build/dist/romcha-vX.Y.Z.apk
```

`Signer #1 certificate DN` が作成した鍵の名前になっていること、初回以降は証明書の SHA-256 が前回の版と
同じであることを確認します（違う場合は鍵を取り違えています。公開しないでください）。

## 4. 実機で確認する

デバッグ版とリリース版は署名が違うため、デバッグ版が入っている端末へはそのまま上書きできません。
デバッグ版を削除してからインストールします（Romcha の設定と特定のキャッシュは消えます）。

```sh
adb uninstall io.github.filderschoice.romcha   # デバッグ版が入っている場合のみ
adb install app/build/dist/romcha-vX.Y.Z.apk
```

| 番号 | 確認 |
| --- | --- |
| R1 | 起動し、権限の案内・フローティング表示・アーカイブのチャット同期が動く |
| R2 | 「アップデート」欄の現在の版が `X.Y.Z` になっている。「更新を確認」を押すと、初回の公開前（Release が1件も無い間）は「公開されている版はまだありません。」、2回目以降は「最新の版です。」になる（公開前の版は既存の版より新しいため） |
| R3 | 2回目以降の版: 前の版を入れた端末へ `adb install -r` で上書きでき、設定が残る |

## 5. タグを作って push する

```sh
git switch main
git pull
git tag -a vX.Y.Z -m "Romcha vX.Y.Z"
git push origin vX.Y.Z
```

## 6. GitHub Releases に公開する

リリースノートには、利用者に見える変更点・既知の制約（広告中の同期のずれ等。README「既知の制約」）・
SHA-256 の値を書きます。

```sh
gh release create vX.Y.Z app/build/dist/romcha-vX.Y.Z.apk app/build/dist/romcha-vX.Y.Z.apk.sha256 \
  --title "Romcha vX.Y.Z" --notes-file <リリースノートのファイル>
```

- アセット名（`romcha-vX.Y.Z.apk`）は変えないでください（Obtainium 等の追従インストーラが名前で取得します）。
- 下書き（`--draft`）・プレリリース（`--prerelease`）は `releases/latest` の対象外のため、アプリの更新通知に出ません。
- リポジトリが非公開（private）の間は、アプリの「更新を確認」（認証なしの `releases/latest`）が 404 を受けて
  「公開されている版はまだありません。」になり、利用者は Releases のページも開けません。リポジトリは 2026-10-04 に公開済みで、
  以後は公開した Release が `releases/latest` に反映されます。

## 7. 公開後の確認

| 番号 | 確認 |
| --- | --- |
| P1 | Releases のページから APK をダウンロードし、`.sha256` の値と一致する（Windows は `certutil -hashfile <APK> SHA256`） |
| P2 | 公開した版を入れた端末で「更新を確認」が「最新の版です。」になる |
| P3 | 1つ前の版を入れた端末（2回目以降）で「新しい版 X.Y.Z が公開されています。」と「ダウンロードページを開く」が出て、リリースのページが開く |

## 8. ロールバック

- 旧版の APK は Releases から削除せずに残します。
- 不具合のある版を取り下げる場合は、その Release を削除するか下書きへ戻します（`releases/latest` が1つ前の版に戻り、
  更新通知も止まります）。修正版は必ず新しい版番号（`versionCode` を上げる）で公開します。
- 端末を旧版へ戻すには `versionCode` が下がるためアンインストールが必要で、設定は消えます（PLAN 6章）。
