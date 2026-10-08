---
name: dependabot-alert-triage
description: GitHub の Dependabot アラートを取得し、出どころ（APK に入る実行時の依存か、ビルドツール側か）を切り分けて、対応できるものを gradle/security-patches.txt の修正版で解消する手順。「Dependabot の内容を確認して」「脆弱性アラートに対応して」「アラートが増えた」等の依頼で参照する。アラートの却下や GitHub 上の操作は行わず、判断が要るものは BACKLOG へ「要確認」で起票する
---

# Dependabot アラートの対応手順

本リポジトリの依存の脆弱性対応の手順です（初回の対応は BL-112。2026-10-09 に 61 件を確認し、すべてビルドツール側だった）。
方針の背景は `docs/records/managed/DESIGN.md`「依存の脆弱性」、ローカルに脆弱性チェックを入れない判断は `CLAUDE.md`
「本リポジトリの品質ゲート定義」にあります。

## 0. 前提と禁止（MUST）

- アラートの**却下・再開・コメントなど GitHub 上の変更は行わない**（外部への操作。ガードレール 12.2）。読み取り（`gh api` の GET）だけを行う。
- 依存の**削除・ダウングレードはしない**（12.2）。上げる場合も、AGP・Kotlin など**ビルド全体を左右する本体は上げず**、要確認として起票する。
- 自律ループ実行モードで行うなら、`autonomous-loop` の手順（作業ブランチ・1 タスク 1 コミット・品質ゲート・記録の更新）に従う。
- 署名鍵を使うリリースビルドは行わない（人が行う。`docs/RELEASE.md`）。署名に使う依存（bouncycastle）を上げたら、署名の検証を人へ依頼する。

## 1. アラートを取得して出どころごとにまとめる

```bash
gh api "repos/filderschoice/romcha/dependabot/alerts?state=open&per_page=100" --paginate \
  -q '.[] | [.number,.security_advisory.severity,.dependency.package.name,.dependency.manifest_path,(.security_vulnerability.first_patched_version.identifier // "-")] | @tsv' > alerts.tsv
```

- 出力は `gh` を Bash で実行する（`dangerouslyDisableSandbox: true` が要ることがある）。一時ファイルはスクラッチパッドへ置く。
- `manifest_path` は dependency submission のワークフローでは `settings.gradle.kts` になり、**出どころの判別には使えない**。
  ライブラリ単位に集計し、重大度の高い順・件数の多い順に並べて、修正版の最大値を控える。
- GitHub の push 時の表示（「n 件の脆弱性」）は集計の単位が違い、API の件数と一致しない。**API の件数を正とする**。

## 2. 出どころを切り分ける（最重要）

「APK に入る実行時の依存か」で対応の緊急度が変わる。

```bash
# APK に入る依存（これに該当すれば最優先）
for m in app feature:overlay core:chat core:sync core:media; do
  ./gradlew.bat :$m:dependencies --configuration releaseRuntimeClasspath -q | grep -iE "<ライブラリ名の正規表現>"
done
# ビルドツール側（AGP などのプラグインのクラスパス）
./gradlew.bat buildEnvironment -q | grep -iE "<正規表現>"
# ktlint・lint・detekt など各モジュールのツール側の設定
./gradlew.bat :app:dependencies -q | grep -iE "<正規表現>"
```

| 出どころ | 影響 | 対応 |
| --- | --- | --- |
| `releaseRuntimeClasspath`（APK に入る） | 利用者に届く | 最優先。`gradle/libs.versions.toml` の版を上げる（下げない）。アプリの動作確認と `docs/VERIFICATION.md` への反映が要る |
| `buildEnvironment`・ツール側の設定 | 開発・ビルド環境のみ | 第 3 節で修正版へ上げる |
| 本体（AGP・Kotlin・Compose など）の更新が要るもの | 影響が大きい | 上げずに BACKLOG へ「要確認」で起票する（初回は kotlin-gradle-plugin の修正版がベータ版のみで BL-113 とした） |

## 3. ビルドツール側を修正版へ上げる

1. `gradle/security-patches.txt` に `group:name:version` を 1 行ずつ追記する。同じ系統のライブラリ（netty の各モジュールなど）は**版をそろえる**。
2. ルートの `build.gradle.kts` が、buildscript のクラスパスには依存の制約（この版以上）として、全モジュールの設定には `eachDependency`
   （要求された版が修正版より古い時だけ上げる）として適用する。**新しいライブラリは `security-patches.txt` の追記だけで済む**（コードは変えない）。
3. 修正版が存在するかは解決で分かる。存在しない版・互換しない版は解決・ビルドで失敗するため、`buildEnvironment` を先に流す。
4. 確認する。
   - `./gradlew.bat buildEnvironment -q` と `:app:dependencies -q` で、対象が修正版に解決される（`旧版 -> 新版` と出る）。
   - **実行時の依存が変わらない**: 変更の前後の `:app:dependencies --configuration releaseRuntimeClasspath -q` を保存して `diff` する。
   - 品質ゲート（`CLAUDE.md`）をすべて実行し、`:app:assembleDebug` が成功する。
   - メジャーに近い更新（例 logback 1.3 → 1.5）は、それを使うツール（ktlint）が動くことまで確かめる（`ktlintCheck` が通る）。

## 4. 記録する

- BACKLOG（`FORMAT.md` に従う）: 解消したものは `EXECUTE.md` と `CHANGELOG.md` へ記録して削除する。
  保留（要確認）は理由とアラートの重大度を書いて残す。GitHub 上のマージ後の再評価の確認は**人手検証**（担当 ユーザー）として起票する
  （dependency submission のワークフローが `main` への push で実行され、アラートが閉じるのはその後のため）。
- DESIGN「依存の脆弱性」に、件数・出どころ・上げた版・保留したものを残す（時点情報は日付付きで）。

## 5. よくある落とし穴

- **`archives` 設定に依存の制約は付けられない**（`Dependency constraints can not be declared against the archives configuration`）。
  全設定へ一律に制約を付けようとせず、`resolutionStrategy.eachDependency` を使う。
- `plugins {}` より前に書けるのは `buildscript {}` だけ。`buildscript` の中では、ルートの変数を使えず、ファイルを読み直す。
- **ライブラリを上げると、その依存が新しいアラートとして現れることがある**（初回は commons-compress 1.21 → 1.26.0 で commons-lang3 3.14.0 が
  引き込まれ、マージ後に commons-lang3 の medium が 1 件出た）。マージ後にアラートを取り直して、残りと新規を `security-patches.txt` へ追記する
  （手順 1〜4 をもう一度流す）。
- Dependabot の件数は、ビルドツール側のライブラリが 1 つ上がっても**マージして dependency submission が再実行されるまで減らない**。
  ローカルで件数が変わらないことを失敗と判断しない。
- Windows の Git Bash では `/tmp` と Python から見える `/tmp` が違う。一時ファイルはスクラッチパッドへ置く。
- 前回の対応の根拠となった確認結果（実行時の依存に該当なし）を、**新しいアラートへ当てはめて省略しない**。毎回第 2 節で確かめる。
