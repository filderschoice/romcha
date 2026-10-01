package io.github.filderschoice.romcha.feature.overlay.session

import android.content.Context
import android.util.AtomicFile
import io.github.filderschoice.romcha.core.chat.resolve.InMemoryResolutionCache
import io.github.filderschoice.romcha.core.chat.resolve.ResolutionCache
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * 動画特定のキャッシュ（PLAN 4.3 手順2）をアプリ専用領域のファイルへ保存する（端末内のみ。N-06）。
 *
 * 書き込みは一時ファイルからの置き換え（AtomicFile）で行い、壊れたファイルは読み飛ばして空から始める。
 */
class PersistentResolutionCache private constructor(
    context: Context,
) : ResolutionCache {
    private val file = AtomicFile(File(context.filesDir, FILE_NAME))
    private val memory = InMemoryResolutionCache()

    init {
        load()
    }

    @Synchronized
    override fun get(identity: String): String? = memory.get(identity)

    @Synchronized
    override fun put(
        identity: String,
        videoId: String,
    ) {
        if (memory.get(identity) == videoId) return
        memory.put(identity, videoId)
        save()
    }

    /** メモリとファイルの両方を消す。 */
    @Synchronized
    override fun clear() {
        memory.clear()
        file.delete()
    }

    private fun load() {
        val text =
            try {
                file.readFully().toString(Charsets.UTF_8)
            } catch (ignored: IOException) {
                return
            }
        try {
            val entries = JSONArray(text)
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                memory.put(entry.getString(KEY_IDENTITY), entry.getString(KEY_VIDEO_ID))
            }
        } catch (ignored: JSONException) {
            // 壊れている場合は空から始める
        }
    }

    private fun save() {
        val entries = JSONArray()
        memory.snapshot().forEach { (identity, videoId) ->
            entries.put(JSONObject().put(KEY_IDENTITY, identity).put(KEY_VIDEO_ID, videoId))
        }
        val stream =
            try {
                file.startWrite()
            } catch (ignored: IOException) {
                return
            }
        try {
            stream.write(entries.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (ignored: IOException) {
            file.failWrite(stream)
        }
    }

    companion object {
        private const val FILE_NAME = "resolution-cache.json"
        private const val KEY_IDENTITY = "identity"
        private const val KEY_VIDEO_ID = "videoId"

        @Volatile
        private var instance: PersistentResolutionCache? = null

        /**
         * プロセス内で 1 つを共有する。オーバーレイ（OverlayService）が使っているメモリ上の内容を、
         * アプリ画面からの消去（[clear]）で同時に消すため。
         */
        fun shared(context: Context): PersistentResolutionCache =
            instance ?: synchronized(this) {
                instance ?: PersistentResolutionCache(context.applicationContext).also { instance = it }
            }
    }
}
