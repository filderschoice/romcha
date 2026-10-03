# アプリ紹介ポートフォリオ（site/）

Romcha を紹介する静的な 1 ページです。ほかのアプリでも同じ構成で作れるよう、テンプレートを兼ねています。
ビルドは不要で、`index.html` をブラウザーで開くだけで表示できます。

## 構成

| ファイル | 内容 |
| --- | --- |
| `index.html` | ページ本体。差し替える箇所に `TEMPLATE:` のコメントを付けている |
| `style.css` | 共通のスタイル。アプリごとに変えるのは先頭の `:root` のアクセント色（3 つ）だけ |
| `app.json` | アプリの概要（名前・一言説明・版・タグ・リンク）。複数アプリのポートフォリオ（一覧ページ）から読むためのもの |
| `assets/` | アイコン・画面イメージ・構成図（現在はすべて SVG の図解） |

## 方針

- **HTML と CSS だけ**で作る。JavaScript・外部フォント・外部 CDN を読み込まない（閲覧者の情報を第三者へ送らず、
  公開先を問わず動くようにするため）。
- ライト／ダークは閲覧者の OS の設定（`prefers-color-scheme`）に従う。スマホ幅（760px 以下）では 1 列になる。
- リンクはすべて相対パスで書き、公開先（GitHub Pages・別のホスティング・一覧ページからの参照）を選ばない。
- 画像には内容が分かる `alt` を付ける。図解を実画面と誤解されないよう、図解であることを `alt` に書く。

## 他のアプリへ流用する手順

1. `site/` をそのまま対象のリポジトリへコピーする。
2. `style.css` の `:root` の `--accent`・`--accent-strong`・`--accent-soft` を、ライト・ダークそれぞれアプリの色へ変える。
3. `index.html` の `TEMPLATE:` の箇所（タイトル・説明・ヒーロー・機能カード・構成図・手順・プライバシー・入手・フッター）を
   書き換え、当てはまらないセクションは `<section>` ごと削除する（ヘッダーのナビゲーションのリンクも合わせて消す）。
4. `assets/` の画像を差し替える。スクリーンショットは縦長（9:19〜9:20 程度）を想定している。
5. `app.json` を書き換える（キーは下記）。

## app.json のキー（`schema: app-portfolio.v1`）

| キー | 内容 |
| --- | --- |
| `id` | 一覧ページでの識別子（英小文字・数字・ハイフン） |
| `name` / `nameJa` | 表示名と日本語の読み |
| `tagline` | 一言説明（一覧のカードに出す想定） |
| `platforms` / `tags` | 対象プラットフォームと技術・分野のタグ |
| `status` | `released`（公開済み）/ `beta` / `development` |
| `version` / `license` | 現在の版とライセンス |
| `icon` / `page` | アイコンとページの、`app.json` からの相対パス |
| `links` | `homepage`（公開URL）・`repository`・`download` などの外部リンク |
| `updated` | 最終更新日（`YYYY-MM-DD`） |

一覧ページは、各アプリの `app.json` を集めてカードを並べ、`page` へリンクする想定です（一覧ページ自体は未作成）。

## Romcha での運用

- GitHub Pages で公開します（2026-10-04 ユーザー判断。sesami-wear と同じ `gh-pages` ブランチ方式）。公開URLは
  <https://filderschoice.github.io/romcha/> です。公開の手順は下記「GitHub Pages での公開」を参照してください。
- `assets/screen.svg` は、架空の動画とダミーのチャットで描いた図解です（実機のスクリーンショットではない）。
  実機では YouTube の動画とチャットの時刻を同期した画面を撮れないため、実画面への差し替えはしません（2026-10-04 ユーザー判断）。
  投稿者名・チャット本文は架空のもの（「視聴者A」等）で、第三者の情報を含みません。
- 機能・版・動作環境を変えたときは、`index.html` と `app.json` も README と合わせて更新します。

## GitHub Pages での公開

`site/` の中身だけを `gh-pages` ブランチへ切り出し、GitHub Pages の「Deploy from a branch」で配信します。
GitHub Actions のワークフローは使いません（本リポジトリには CI が無く、Pages の公開元フォルダには
`/ (root)` か `/docs` しか選べないため）。公開される範囲は `gh-pages` に載る `site/` の中身だけです。
`site/.nojekyll` は、Jekyll の処理を止めてそのまま配信するための空ファイルです（消さないでください）。
`git push` はユーザーが実行します（エージェントは実行しません）。

### 初回の設定（1 回だけ）

1. `site/` の変更を `main` へマージしたあと、`main` を最新にした状態で `gh-pages` ブランチを作って push する。

   ```bash
   git checkout main
   git pull
   git subtree push --prefix site origin gh-pages
   ```

2. GitHub のリポジトリの Settings → Pages で、Source を「Deploy from a branch」、Branch を `gh-pages` の
   `/ (root)` にして保存する（`gh api -X POST repos/filderschoice/romcha/pages -f "source[branch]=gh-pages" -f "source[path]=/"`
   でも設定できる。エージェントへ依頼してよい）。
3. 数分後に <https://filderschoice.github.io/romcha/> を開き、画像とリンクが表示されることを確かめる。

### 更新するとき

`site/` の変更を `main` へマージしたあと、`main` で同じコマンドを実行します。`gh-pages` の履歴は
`site/` の履歴から毎回同じ形で作られるため、通常は早送りで反映されます。

```bash
git checkout main
git pull
git subtree push --prefix site origin gh-pages
```

`gh-pages` を直接編集しないでください（次の push が早送りにならず失敗します）。失敗した場合は、
`gh-pages` を `site/` の履歴で作り直します（公開中の内容は `site/` と同じなので失われるものはありません）。

```bash
git push origin "$(git subtree split --prefix site main)":refs/heads/gh-pages --force
```

アプリのリリースで機能・版・画面が変わったときは、`index.html` と `app.json` を更新して上記の手順で反映します。
