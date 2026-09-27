"""YouTube の実応答から core:chat の単体テスト用 fixture を作る（BL-022）。

アプリと同じ InnerTube のリクエスト（WEB クライアント）で次の応答を取得し、個人に関わる情報を置き換えてから
``core/chat/src/test/resources/fixtures/real/`` へ保存する。

| 出力ファイル | 取得元 |
| --- | --- |
| next_replay.json | ``next``（--replay の動画） |
| replay_chunk.json | ``live_chat/get_live_chat_replay``（チャット欄本体の continuation、再生位置 60 秒） |
| search_results.json | ``search``（--replay の動画の「タイトル チャンネル名」） |
| next_live.json | ``next``（--live の動画。省略時は検索の絞り込み「ライブ」の先頭） |
| live_chunk.json | ``live_chat/get_live_chat``（チャット欄本体の continuation） |

置き換える情報（チャットの投稿者に関わるもの）:

- 投稿者名（``authorName``）→「視聴者N」。応答内の他の文字列に現れた同じ名前も置き換える
- 投稿者のチャンネルID（``authorExternalChannelId``）→ 連番の仮ID
- 画像の URL（アイコン・スタンプ・絵文字等の ``thumbnails``）→ ``https://example.invalid/image.png``
- コメント本文（``message`` の文字列部分）→「コメントN」（絵文字の部分は残す）
- 追跡用の値（``trackingParams`` 等）と、解析に使わない大きな部分木は削除する

保存前に、元の投稿者名・チャンネルIDが出力に残っていないかを検査し、残っていれば保存を中止する。
元の応答はファイルへ保存しない（--keep-raw を指定した場合のみ、指定したリポジトリ外のディレクトリへ保存する）。

使い方:
    python scripts/fetch-real-fixtures.py --replay <アーカイブの動画ID> [--live <配信中の動画ID>]
    python scripts/fetch-real-fixtures.py --raw-dir <元の応答のディレクトリ>   # 通信せずに置き換えだけ行う

実行には YouTube への通信を伴う（--raw-dir を除く）。自律ループ実行モードのエージェントは通信するモードを実行しない
（guardrails 12.5）。
"""

from __future__ import annotations

import argparse
import copy
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CLIENT_SOURCE = REPO_ROOT / "core/chat/src/main/kotlin/io/github/filderschoice/romcha/core/chat/InnerTubeClient.kt"
DEFAULT_OUT_DIR = REPO_ROOT / "core/chat/src/test/resources/fixtures/real"
BASE_URL = "https://www.youtube.com/youtubei/v1/"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
)
REPLAY_OFFSET_MS = "60000"
MAX_CHAT_ACTIONS = 40
MAX_SEARCH_VIDEOS = 5
PLACEHOLDER_IMAGE = "https://example.invalid/image.png"

# 解析に使わず、追跡用の値や大きな部分木を含むキー（出力から削除する）
DROP_KEYS = {
    "trackingParams",
    "clickTrackingParams",
    "loggingDirectives",
    "contextMenuEndpoint",
    "contextMenuAccessibility",
    "menu",
    "richThumbnail",
    "channelThumbnailSupportedRenderers",
    "detailedMetadataSnippets",
    "expandableMetadata",
    "avatar",
    "inlinePlaybackEndpoint",
    "searchVideoResultEntityKey",
    "serviceEndpoint",
}

FILES = ["next_replay.json", "replay_chunk.json", "search_results.json", "next_live.json", "live_chunk.json"]


def client_version() -> str:
    """アプリと同じ clientVersion を InnerTubeClient.kt から読む（版数のずれを防ぐ）。"""
    match = re.search(r'DEFAULT_CLIENT_VERSION = "([^"]+)"', CLIENT_SOURCE.read_text(encoding="utf-8"))
    if match is None:
        sys.exit(f"clientVersion を読めません: {CLIENT_SOURCE}")
    return match.group(1)


def post(path: str, payload: dict) -> dict:
    body = {"context": {"client": {"clientName": "WEB", "clientVersion": client_version(), "hl": "ja", "gl": "JP"}}}
    body.update(payload)
    request = urllib.request.Request(
        f"{BASE_URL}{path}?prettyPrint=false",
        data=json.dumps(body).encode("utf-8"),
        headers={"User-Agent": USER_AGENT, "Content-Type": "application/json", "Accept-Language": "ja"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read())
    except urllib.error.HTTPError as e:
        sys.exit(f"{path} が HTTP {e.code} を返しました")


def find_all(node, key: str) -> list:
    found = []
    if isinstance(node, dict):
        for k, v in node.items():
            if k == key:
                found.append(v)
            found.extend(find_all(v, key))
    elif isinstance(node, list):
        for v in node:
            found.extend(find_all(v, key))
    return found


def chat_token(next_response: dict) -> str:
    """チャット欄本体の continuation（アプリの WatchInfoParser と同じ位置）。"""
    chat = (
        next_response.get("contents", {})
        .get("twoColumnWatchNextResults", {})
        .get("conversationBar", {})
        .get("liveChatRenderer")
    )
    if chat is None:
        sys.exit("この動画にはチャットがありません（チャットのある動画IDを指定してください）")
    for item in chat.get("continuations", []):
        token = item.get("reloadContinuationData", {}).get("continuation")
        if token:
            return token
    sys.exit("チャット欄の continuation が見つかりません")


def text_of(node) -> str:
    if not isinstance(node, dict):
        return ""
    if "simpleText" in node:
        return node["simpleText"]
    return "".join(run.get("text", "") for run in node.get("runs", []))


def fetch(replay_id: str, live_id: str | None) -> dict[str, dict]:
    raw = {}
    raw["next_replay.json"] = post("next", {"videoId": replay_id})
    raw["replay_chunk.json"] = post(
        "live_chat/get_live_chat_replay",
        {
            "continuation": chat_token(raw["next_replay.json"]),
            "currentPlayerState": {"playerOffsetMs": REPLAY_OFFSET_MS},
        },
    )
    watch = raw["next_replay.json"]["contents"]["twoColumnWatchNextResults"]["results"]["results"]["contents"]
    title = next((text_of(c["videoPrimaryInfoRenderer"]["title"]) for c in watch if "videoPrimaryInfoRenderer" in c), "")
    owner = next(
        (
            text_of(c["videoSecondaryInfoRenderer"]["owner"]["videoOwnerRenderer"]["title"])
            for c in watch
            if "videoSecondaryInfoRenderer" in c
        ),
        "",
    )
    raw["search_results.json"] = post("search", {"query": f"{title} {owner}".strip()})
    if live_id is None:
        live_search = post("search", {"query": "ライブ", "params": "EgJAAQ=="})
        ids = [v["videoId"] for v in find_all(live_search, "videoRenderer") if "videoId" in v]
        if not ids:
            sys.exit("配信中の動画が見つかりません（--live で動画IDを指定してください）")
        live_id = ids[0]
    raw["next_live.json"] = post("next", {"videoId": live_id})
    raw["live_chunk.json"] = post("live_chat/get_live_chat", {"continuation": chat_token(raw["next_live.json"])})
    return raw


# ---- 不要部分の削除 -------------------------------------------------------------


def prune_next(response: dict) -> dict:
    watch = response.get("contents", {}).get("twoColumnWatchNextResults", {})
    contents = []
    for item in watch.get("results", {}).get("results", {}).get("contents", []):
        if "videoPrimaryInfoRenderer" in item:
            primary = item["videoPrimaryInfoRenderer"]
            contents.append({"videoPrimaryInfoRenderer": {"title": primary.get("title")}})
        elif "videoSecondaryInfoRenderer" in item:
            owner = item["videoSecondaryInfoRenderer"].get("owner", {}).get("videoOwnerRenderer", {})
            contents.append({"videoSecondaryInfoRenderer": {"owner": {"videoOwnerRenderer": {"title": owner.get("title")}}}})
    return {
        "contents": {
            "twoColumnWatchNextResults": {
                "results": {"results": {"contents": contents}},
                "conversationBar": watch.get("conversationBar", {}),
            }
        }
    }


def prune_chat(response: dict) -> dict:
    chat = response.get("continuationContents", {}).get("liveChatContinuation", {})
    kept = {key: chat[key] for key in ("continuations", "header", "isReplay") if key in chat}
    kept["actions"] = chat.get("actions", [])[:MAX_CHAT_ACTIONS]
    return {"continuationContents": {"liveChatContinuation": kept}}


def prune_search(response: dict) -> dict:
    videos = []
    for video in find_all(response, "videoRenderer"):
        if isinstance(video, dict) and "videoId" in video:
            videos.append({"videoRenderer": video})
        if len(videos) >= MAX_SEARCH_VIDEOS:
            break
    return {
        "contents": {
            "twoColumnSearchResultsRenderer": {
                "primaryContents": {"sectionListRenderer": {"contents": [{"itemSectionRenderer": {"contents": videos}}]}}
            }
        }
    }


PRUNERS = {
    "next_replay.json": prune_next,
    "replay_chunk.json": prune_chat,
    "search_results.json": prune_search,
    "next_live.json": prune_next,
    "live_chunk.json": prune_chat,
}


# ---- 個人に関わる情報の置き換え ---------------------------------------------------


class Anonymizer:
    """投稿者名・チャンネルIDを全ファイルで一貫した仮の値へ置き換える。"""

    def __init__(self) -> None:
        self.names: dict[str, str] = {}
        self.channel_ids: dict[str, str] = {}
        self.comment_count = 0

    def collect(self, node) -> None:
        for name_node in find_all(node, "authorName"):
            name = text_of(name_node)
            if name and name not in self.names:
                self.names[name] = f"視聴者{len(self.names) + 1}"
        for channel_id in find_all(node, "authorExternalChannelId"):
            if isinstance(channel_id, str) and channel_id not in self.channel_ids:
                self.channel_ids[channel_id] = f"UCanonymous{len(self.channel_ids) + 1:013d}"

    def replace_text(self, text: str) -> str:
        # 長い名前から置き換える（短い名前が長い名前の一部である場合の取りこぼしを防ぐ）
        for original in sorted(self.names, key=len, reverse=True):
            text = text.replace(original, self.names[original])
        for original, fake in self.channel_ids.items():
            text = text.replace(original, fake)
        return text

    def anonymize(self, node):
        if isinstance(node, list):
            return [self.anonymize(v) for v in node]
        if isinstance(node, str):
            return self.replace_text(node)
        if not isinstance(node, dict):
            return node
        result = {}
        for key, value in node.items():
            if key in DROP_KEYS:
                continue
            if key == "authorName":
                name = text_of(value)
                result[key] = {"simpleText": self.names.get(name, "視聴者")}
            elif key == "authorExternalChannelId" and isinstance(value, str):
                result[key] = self.channel_ids.get(value, "UCanonymous0000000000000")
            elif key == "thumbnails" and isinstance(value, list):
                result[key] = [{**{k: v for k, v in t.items() if k != "url"}, "url": PLACEHOLDER_IMAGE} for t in value]
            elif key == "message" and isinstance(value, dict):
                result[key] = self.anonymize_message(value)
            else:
                result[key] = self.anonymize(value)
        return result

    def anonymize_message(self, message: dict) -> dict:
        message = copy.deepcopy(message)
        if "simpleText" in message:
            self.comment_count += 1
            message["simpleText"] = f"コメント{self.comment_count}"
            return message
        runs = []
        replaced = False
        for run in message.get("runs", []):
            if "text" in run:
                if not replaced:
                    self.comment_count += 1
                    runs.append({"text": f"コメント{self.comment_count}"})
                    replaced = True
            else:
                runs.append(self.anonymize(run))
        message["runs"] = runs
        return message

    def leaks(self, text: str) -> list[str]:
        return [value for value in [*self.names, *self.channel_ids] if len(value) >= 2 and value in text]


def build(raw: dict[str, dict]) -> dict[str, str]:
    pruned = {name: PRUNERS[name](raw[name]) for name in FILES}
    anonymizer = Anonymizer()
    for response in pruned.values():
        anonymizer.collect(response)
    outputs = {}
    for name, response in pruned.items():
        text = json.dumps(anonymizer.anonymize(response), ensure_ascii=False, indent=2) + "\n"
        leaked = anonymizer.leaks(text)
        if leaked:
            sys.exit(f"{name}: 置き換え後も投稿者の情報が {len(leaked)} 件残っているため保存を中止しました")
        outputs[name] = text
    print(f"投稿者 {len(anonymizer.names)} 人、コメント {anonymizer.comment_count} 件を置き換えました")
    return outputs


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--replay", help="チャットリプレイのあるアーカイブの動画ID（YouTube へ通信する）")
    source.add_argument("--raw-dir", type=Path, help="元の応答（出力ファイルと同名）のディレクトリ。通信しない")
    parser.add_argument("--live", help="配信中の動画ID（省略時は検索の絞り込み「ライブ」の先頭）")
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_DIR, help="出力先")
    parser.add_argument("--keep-raw", type=Path, help="元の応答を保存するディレクトリ（リポジトリの外を指定する）")
    args = parser.parse_args()

    if args.raw_dir is not None:
        raw = {name: json.loads((args.raw_dir / name).read_text(encoding="utf-8")) for name in FILES}
    else:
        raw = fetch(args.replay, args.live)
        if args.keep_raw is not None:
            keep = args.keep_raw.resolve()
            if REPO_ROOT in keep.parents or keep == REPO_ROOT:
                sys.exit("--keep-raw にはリポジトリの外のディレクトリを指定してください（元の応答は個人に関わる情報を含む）")
            keep.mkdir(parents=True, exist_ok=True)
            for name, response in raw.items():
                (keep / name).write_text(json.dumps(response, ensure_ascii=False), encoding="utf-8")

    outputs = build(raw)
    args.out_dir.mkdir(parents=True, exist_ok=True)
    for name, text in outputs.items():
        (args.out_dir / name).write_text(text, encoding="utf-8", newline="\n")
    print(f"{len(outputs)} ファイルを {args.out_dir} へ保存しました")


if __name__ == "__main__":
    main()
