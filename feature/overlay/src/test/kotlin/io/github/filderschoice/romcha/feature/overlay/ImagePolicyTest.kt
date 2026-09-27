package io.github.filderschoice.romcha.feature.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePolicyTest {
    @Test
    fun YouTubeの画像配信元のHTTPSだけを許可する() {
        assertTrue(ImagePolicy.isAllowed("https://yt3.ggpht.com/abc=w24-h24-c-k-nd"))
        assertTrue(ImagePolicy.isAllowed("https://i.ytimg.com/vi/xyz/default.jpg"))
        assertTrue(ImagePolicy.isAllowed("https://lh3.googleusercontent.com/abc"))
    }

    @Test
    fun それ以外の配信元やHTTPは許可しない() {
        assertFalse(ImagePolicy.isAllowed("http://yt3.ggpht.com/abc"))
        assertFalse(ImagePolicy.isAllowed("https://example.com/a.png"))
        assertFalse(ImagePolicy.isAllowed("https://ggpht.com.example.com/a.png"))
        assertFalse(ImagePolicy.isAllowed("https://evilggpht.com/a.png"))
        assertFalse(ImagePolicy.isAllowed("not a url"))
        assertFalse(ImagePolicy.isAllowed("file:///sdcard/a.png"))
    }
}
