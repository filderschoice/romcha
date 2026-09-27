package io.github.filderschoice.romcha.core.chat.internal

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

// InnerTube 応答を必要な箇所だけ辿るための補助（PLAN 4.4。全体を型定義しない）。
// いずれも型が合わない・キーが無い場合は null を返し、例外を投げない（N-08）。

internal fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

internal fun JsonElement?.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray

internal fun JsonElement?.str(key: String): String? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

/** 数値または数値文字列（`"83000"` のように文字列で返るものがある）を Long として読む。 */
internal fun JsonElement?.long(key: String): Long? {
    val primitive = (this as? JsonObject)?.get(key) as? JsonPrimitive ?: return null
    return if (primitive.isString) primitive.content.toLongOrNull() else primitive.longOrNull
}

internal fun JsonElement?.bool(key: String): Boolean? {
    val primitive = (this as? JsonObject)?.get(key) as? JsonPrimitive ?: return null
    return primitive.content.toBooleanStrictOrNull()
}

/** `{"simpleText": "..."}` または `{"runs": [{"text": "..."}]}` 形式の文字列を読む。 */
internal fun JsonElement?.text(key: String): String? {
    val node = obj(key) ?: return null
    node.str("simpleText")?.let { return it }
    val runs = node.arr("runs") ?: return null
    return runs.joinToString("") { it.str("text").orEmpty() }
}

/** `{"thumbnails": [{"url": ...}, ...]}` から最後（最大解像度）の URL を読む。`//` 始まりは https を補う。 */
internal fun JsonElement?.thumbnailUrl(key: String): String? {
    val url = obj(key).arr("thumbnails")?.lastOrNull().str("url") ?: return null
    return if (url.startsWith("//")) "https:$url" else url
}
