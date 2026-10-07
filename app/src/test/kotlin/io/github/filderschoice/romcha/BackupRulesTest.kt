package io.github.filderschoice.romcha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** 再インストール時に設定を復元するバックアップ規則（BL-095）が、設定だけを対象にしていることを確かめる。 */
class BackupRulesTest {
    private val rules = File("src/main/res/xml/data_extraction_rules.xml")

    private fun includes(section: String): List<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(rules)
        val node = doc.getElementsByTagName(section).item(0)
        val items = (node as org.w3c.dom.Element).getElementsByTagName("include")
        return (0 until items.length).map {
            val e = items.item(it) as org.w3c.dom.Element
            e.getAttribute("domain") to e.getAttribute("path")
        }
    }

    @Test
    fun クラウドバックアップと端末間移行は設定のファイルだけを対象にする() {
        val expected =
            listOf("overlay.xml", "display.xml", "crash_reporting.xml").map { "sharedpref" to it }
        assertEquals(expected, includes("cloud-backup"))
        assertEquals(expected, includes("device-transfer"))
    }

    @Test
    fun マニフェストはバックアップを許可している() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"true\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
    }
}
