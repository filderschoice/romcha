package io.github.filderschoice.romcha.update

/**
 * アプリの版（SemVer の `MAJOR.MINOR.PATCH[-プレリリース][+ビルド]`）。更新通知（F-APP-02）の版の比較に使う。
 *
 * タグ形式の先頭の `v` / `V` は読み飛ばす。ビルドメタデータ（`+` 以降）は比較に使わない。
 * プレリリース付きは同じ番号の正式版より古いとみなし、プレリリース同士は文字列で比べる（本アプリの運用ではこれで足りる）。
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)
            .takeIf { it != 0 }
            ?: comparePreRelease(preRelease, other.preRelease)

    override fun toString(): String = "$major.$minor.$patch" + (preRelease?.let { "-$it" } ?: "")

    companion object {
        private val PATTERN =
            Regex(
                """[vV]?(?<major>\d{1,6})\.(?<minor>\d{1,6})\.(?<patch>\d{1,6})""" +
                    """(?:-(?<pre>[0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?""",
            )

        /** 版の文字列を読む。形式が違えば null。 */
        fun parse(text: String): AppVersion? {
            val match = PATTERN.matchEntire(text.trim()) ?: return null
            val groups = match.groups

            fun number(name: String) = requireNotNull(groups[name]).value.toInt()
            return AppVersion(number("major"), number("minor"), number("patch"), groups["pre"]?.value)
        }

        private fun comparePreRelease(
            a: String?,
            b: String?,
        ): Int =
            when {
                a == b -> 0
                a == null -> 1
                b == null -> -1
                else -> a.compareTo(b)
            }
    }
}
