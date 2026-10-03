# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## 2026-10-04

- `docs/VERIFICATION.md` に BL-082（UI/UX 改善の実機確認）の 3 章（U1〜U9）を追加し、結果の記録を 4 章へ繰り下げた（BL-084）。
  BL-082 のタスク内容は観点を手順書へ寄せて要約にした。BL-073 の完了条件が指していた章番号（3 章 → 1 章）の古い記述を直した。

- README の「使い方」を UI/UX 改善（BL-077〜BL-081）に合わせた（BL-083）。権限がすべて許可済みなら 1 行にまとまること、
  ウィンドウ上部のお知らせ帯の色の意味（灰色は案内、赤は失敗）と空の一覧の案内、表示設定の「ヘッダーのボタンを大きくする」を追記した。
  「状態」欄の版は v1.0.1 のまま。

- UI/UX 改善（BL-077〜BL-081）に対するドキュメントと site の反映漏れを点検し、`docs/records/managed/BACKLOG.md` へ登録した
  （自律ループ第1イテレーション）。README の使い方（BL-083）、VERIFICATION への BL-082 の章の追加（BL-084）、
  site の機能カードへの反映（BL-085。配布中の v1.0.1 に無い機能を載せないため、次の版の公開時まで保留。ユーザー判断）。
  site の画面イメージ（`site/assets/screen.svg`）はお知らせ帯・ヘッダーのボタンの大きさを描いていないため変更不要とした。

- アプリの UI/UX レビューを行い、改善施策を `docs/records/managed/BACKLOG.md` へ登録した（自律ループ第1イテレーション）。
  お知らせ帯の色分け（BL-077）、空の一覧の案内文（BL-078）、表示設定画面の行全体のタップ（BL-079）、HOME 画面の整理（BL-080）、
  ヘッダーのボタンの大きさの切り替え（BL-081。36dp と 48dp。ユーザー判断）。

- ドキュメントを最新化した（自律ループ）。アプリの作成背景（YouTube 公式アプリの小窓ではチャットが見えなくなるため、
  フローティングに対応したチャットビューワーが欲しかった）を、`README.md` の「作成の背景」、`site/index.html` の「作った理由」節と
  ヒーロー文・description、`site/README.md`、DESIGN の「作成の背景」へ追記した。`site/app.json` の `updated` を更新した。
  「作った理由」には、小窓ではチャットが見えない状態と Romcha で浮かべた状態を比べる図解
  `site/assets/floating.svg`（架空の名前・チャットのダミー）も載せた。
  他のドキュメントは実装・公開状況と整合していることを機械検査（リンク到達性・時点情報の抽出）で確認した。

- ポートフォリオ（`site/`）を GitHub Pages で公開した（<https://filderschoice.github.io/romcha/>）。`gh-pages` ブランチ（`/ (root)`）から配信され、
  トップ・CSS・`app.json`・画像の取得と、ページ内のリンクを確認した。BL-061 を完了として BACKLOG から削除し、
  `docs/VERIFICATION.md` から端末を使わない作業の章を除いた。
- BL-059 を閉じた。Dependabot alerts は有効だが、Dependency graph に Gradle の依存は検出されなかったため、
  dependency submission（GitHub Actions）を導入するかの判断を BL-076（要確認）へ切り出した。
- ポートフォリオ（`site/`）を GitHub Pages で公開する準備をした（sesami-wear と同じ `gh-pages` ブランチ方式。ユーザー判断）。
  `site/.nojekyll` の追加、`site/app.json` の `links.homepage`、`site/index.html` の `og:url`・`canonical`、
  `site/README.md` の公開手順、README・DESIGN・VERIFICATION を更新した。公開（`gh-pages` の push と Pages の設定）は
  `main` へのマージ後にユーザーが行う（BL-061）。
- Releases の公開の運用方針を改めた（ユーザー判断）。ユーザーが明示的に指示した場合に限り、エージェントが `gh release create --verify-tag` で
  公開してよい。鍵・署名ビルド・タグの push は引き続き人が行い、自律ループ実行モード内では公開しない。
  `docs/RELEASE.md` と DESIGN の記述を合わせ、BL-065 を完了として BACKLOG から削除した。
- ポートフォリオ（`site/`）の画面イメージ `site/assets/screen.svg` を、架空の動画とダミーのチャット（上位チャットを含む）による図解へ
  描き直した。実機では YouTube の動画とチャットの時刻を同期した画面を撮れないため、スクリーンショットへの差し替えはしない
  （ユーザー判断）。`site/index.html` の `alt` と `site/README.md` を合わせた。BL-061 は公開方法の決定だけに絞った。
  BL-058 は P2・P4・K1 の確認結果（OK）を反映し、残る P3（次の版の公開後）を `docs/VERIFICATION.md` に残した。
- リポジトリを公開（public）にし、公開時の設定を行った。About の説明・topics を設定し、main に ruleset（ブランチの削除・force push の禁止、
  Pull Request 必須。Repository admin は bypass）を設定し、Dependabot alerts を有効にした。BL-069 を完了として BACKLOG から削除し、
  BL-059（Dependency graph が Gradle の依存を検出するかは再確認待ち）と BL-058 の記述を更新した。
  README・RELEASE・DESIGN・VERIFICATION の「リポジトリが非公開」の記述を公開済みの内容へ改めた。
- 脆弱性の報告方法を示す `SECURITY.md` を追加した（窓口は GitHub Issues。悪用手順・個人情報を書かないお願いと対象外を記載）。
  `README.md` の「問題の報告」節から参照した（BL-074）。
- リポジトリの公開（public 化）前の再点検を行った。履歴全体に秘密情報（鍵・トークン・パスワード）が無いことを確認した。
  コミットに記録された作者のメールアドレスは履歴を書き換えずにそのまま公開することとした（ユーザー判断）。
  指摘を `docs/records/managed/BACKLOG.md` へ登録し（BL-074 SECURITY.md の追加、BL-075 実データの fixture に残る配信者の情報の置き換え）、
  BL-069 へ main の ruleset 設定を加えた（自律ループ第1イテレーション）。

## 2026-10-03

- 実機検証（`docs/VERIFICATION.md`）の結果を BACKLOG・DESIGN へ反映した。BL-071（同名のライブ配信でも今の配信のチャットが出る）、
  BL-072（動画切り替え直後に識別キーが混ざる状態は見られず、特定の待ち（デバウンス）は入れない）、BL-024（D6 終了後のリプレイ切り替え）を
  完了として BACKLOG から削除した。BL-073 は C1〜C3 を確認し、誤特定からの回復（C5）だけが未確認のため進行中で残した。
  VERIFICATION から確認済みの項目を除き、残りを BL-073（C5）・BL-058・BL-069・BL-059・BL-061 へ絞った。

## 2026-10-02

- v1.0.1 の GitHub Release を公開した（ユーザーの明示指示でエージェントが `gh release create --verify-tag` で公開。タグ `v1.0.1` は `main` の
  8fa99f3、署名済み APK と `.sha256`。公開した APK の SHA-256 の一致を確認）。ライブ中の誤特定の修正とキャッシュを消す操作を含む。
  `README.md` の状態表・`site/app.json`・`site/index.html`・`docs/RELEASE.md`・DESIGN・BL-058 へ反映した。

## 2026-09-29

- Google Play での公開を検討したが、非公式 API を使う非公式アプリであり Play の審査・ポリシーにそぐわないため、GitHub Releases のみで
  公開する方針を維持した（ユーザー判断）。判断理由を `docs/PLAN.md` 6章に追記した。

## 2026-09-28

- 履歴に残る検証端末のシリアル番号は、履歴を書き換えずにそのまま公開することとした（ユーザー判断）。BL-070 を閉じ、BL-069 の依存から外した。
- `README.md` に「問題の報告」節（GitHub Issues の窓口、報告に書く情報、個人情報を書かないお願い）を追加した（BL-068）。
- `.github/CODEOWNERS` の配布テンプレートのプレースホルダ（`@your-org/...`）を、リポジトリ所有者（`@filderschoice`）へ置き換えた（BL-067）。
- `docs/VERIFICATION.md` から検証端末のシリアル番号を除き、`<シリアル>`（`adb devices -l` で確認する値）へ置き換えた（BL-066）。
- リポジトリの公開（public 化）に向けた点検の指摘を `docs/records/managed/BACKLOG.md` へ登録した（BL-066〜BL-070。
  検証端末のシリアル番号・CODEOWNERS のプレースホルダ・README の問い合わせ窓口・公開の操作・履歴に残るシリアルの扱い。自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、v1.0.0 の GitHub Releases 公開をドキュメントへ反映するタスク（BL-064）と、
  Releases の公開を誰が行うかの規定の見直し（BL-065。要確認）を登録した（自律ループ第1イテレーション）。
- v1.0.0 の GitHub Release 作成を `README.md` の状態表・DESIGN・BACKLOG（BL-058 を残りの確認へ絞った）・`site/app.json`・`docs/RELEASE.md` 6章へ反映した（BL-064）。
  リポジトリが非公開の間は「更新を確認」が「公開されている版はまだありません。」になること（API が 404 を返す）を記載した。

## 2026-09-27

- `docs/records/managed/BACKLOG.md` へ、リリースビルドを簡易化するスクリプトの作成（BL-063。ユーザー指示）を登録した
  （自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、リリース署名の情報を `local.properties` から読むよう変える修正（BL-062。ユーザー指示）を
  登録した（自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、ユーザー指示のアプリ紹介ポートフォリオ（BL-060）と、その実機画像の差し替え・公開方法の
  決定（人手検証 BL-061）を登録した（自律ループ第1イテレーション）。
- 脆弱性チェックの代替手段を GitHub の Dependabot アラートとした（ユーザー判断）。`CLAUDE.md`「本リポジトリの品質ゲート定義」の
  説明と PLAN 9章を更新し、BL-057 を閉じて、リポジトリ設定での有効化と検出範囲の確認を人手検証 BL-059 として登録した。
- `docs/records/managed/BACKLOG.md` の BL-027（M5 配布）を、ユーザー回答（コード整備までを範囲とし、更新確認は手動のみ）に
  基づいて BL-054〜BL-058 へ分割した（自律ループ第1イテレーション）。
- 実機検証 BL-053（`docs/VERIFICATION.md` の G1。フローティングからアプリ本体を開く）が OK だったため完了とし、
  BACKLOG と `docs/VERIFICATION.md` から削除した。DESIGN に確認済みを反映した。
- `docs/records/managed/BACKLOG.md` へ、フローティングの設定メニューからアプリ本体を起動する機能（BL-052）と、その人手検証（BL-053）を
  登録した（自律ループ第1イテレーション）。
- 実機検証 BL-048（`docs/VERIFICATION.md` の F1〜F15。M3・M4 と BL-034〜BL-051 の表示・操作）がすべて OK だったため完了とし、
  BACKLOG と `docs/VERIFICATION.md` から削除した。README の状態表から確認済みの「実機確認待ち」を外し、DESIGN に確認済みを反映した。
- `docs/records/managed/BACKLOG.md` へ、実機確認での指摘（退避中のつまみの横幅を広げる）を BL-051 として登録した
  （自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、実機確認での指摘（退避のドラッグが画面幅で止まる）を BL-050 として登録した
  （自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ、ユーザー指示のフローティングウィンドウの画面端への退避と復帰（BL-049）を登録し、
  人手検証 BL-048 の確認対象に加えた（自律ループ第1イテレーション）。
- `docs/VERIFICATION.md` から確認済みの項目（BL-021・BL-022・BL-023 の全項目、BL-024 の D1〜D5）を削除し、未確認の
  BL-024 の D6 と BL-048 だけを残した（ユーザー指示）。章番号を詰め、BACKLOG・PLAN の参照を合わせた。
- `README.md` の状態表と使い方を M3・M4 の実装（BL-034〜BL-047）に合わせて更新し、`docs/VERIFICATION.md` に
  BL-048（表示の調整と操作）の確認表（5 章）を追加した。以降の章番号を 1 つずつ繰り下げた。
- `docs/records/managed/BACKLOG.md` へ、ユーザー指示の2件（チャットの文字サイズ、ヘッダーの配色）と M3・M4 を要件単位に分解して登録した
  （BL-034〜BL-048。BL-025 / BL-026 は分割により削除。自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` へ PLAN.md（M0〜M5）をタスク分解して登録した（自律ループ第1イテレーション）。
- `docs/records/managed/BACKLOG.md` の先頭へ `markdownlint-disable-file MD041` を追加し、DESIGN.md / EXECUTE.md と体裁を揃えた（ユーザー承認済み）。
- `CLAUDE.md`「本リポジトリの品質ゲート定義」の `replace-me` を Gradle のコマンド（ktlint・detekt・Android lint、
  compileDebugKotlin、単体テスト）へ置き換えた。脆弱性チェックはコストに見合わないため導入しない（ユーザー判断）。
  導入完了に伴い、同節の記入方法コメントを削除した。
- `README.md` を追加した（概要・免責・使い方と権限の用途・動作環境・ビルドと品質ゲート・構成・プライバシー・ライセンス）。
- `README.md` の状態表と使い方を M2（ライブ・プレミア対応）の実装に合わせて更新した。
- 実機検証手順書 `docs/VERIFICATION.md`（BL-021〜BL-024）を追加し、README からリンクした。各確認表に結果欄と記入値の凡例を設け、
  最新の結果だけを上書きで保持する（履歴は残さない）運用とした（BL-030）。
- 実機検証（`docs/VERIFICATION.md`、Premium 有り／無しの両環境）の結果を反映した。BL-021（MediaSession）と BL-023（リプレイ同期）は
  完了し BACKLOG から削除した。BL-022 は実応答からの fixture の作り直し、BL-024 は D6（終了後のリプレイ切り替え）を残して進行中とした。
  PLAN の Q-01 / Q-02 を決定済みにし、4.2・4.3・K-01 / K-02 / K-05 を検証結果で更新した。
  広告中の扱いを確認する人手検証 BL-031 を起票した。DESIGN に検証結果を追記した。
- 広告中の同期ずれを既知の制約として `README.md` に追記し、動作環境とテストの説明を検証結果に合わせて更新した（BL-031 の決定）。
- 常駐通知の動画タイトルが動画の切り替えに追従することを実機で確認し、BL-033 を完了とした。
