package `in`.firm.consultancy.bayaan.cardfit.data

import android.content.Context
import `in`.firm.consultancy.bayaan.cardfit.R

/** One bundled open-source library and its license text (or license URL). */
data class OssLicenseEntry(val name: String, val text: String)

/**
 * Load the bundled open-source license attributions.
 *
 * The data is the play-services OSS-licenses plugin's *release* output, committed as raw resources so
 * the same content is shown in both debug and release (the plugin only populates real data for release
 * builds — debug otherwise shows a blank "Debug License Info" placeholder). `oss_license_metadata`
 * holds `byteOffset:byteLength Name` lines pointing into `oss_licenses`; each slice is either a full
 * license text or a license URL. Fully offline — nothing is fetched.
 */
fun loadOssLicenses(context: Context): List<OssLicenseEntry> {
    val res = context.resources
    val blob = res.openRawResource(R.raw.oss_licenses).use { it.readBytes() }
    val meta = res.openRawResource(R.raw.oss_license_metadata).use { it.readBytes() }.toString(Charsets.UTF_8)
    return meta.lineSequence().mapNotNull { line ->
        if (line.isBlank()) return@mapNotNull null
        val space = line.indexOf(' ')
        if (space <= 0) return@mapNotNull null
        val range = line.substring(0, space)
        val name = line.substring(space + 1).trim()
        val colon = range.indexOf(':')
        if (colon <= 0) return@mapNotNull null
        val offset = range.substring(0, colon).toIntOrNull() ?: return@mapNotNull null
        val length = range.substring(colon + 1).toIntOrNull() ?: return@mapNotNull null
        if (offset < 0 || length <= 0 || offset + length > blob.size) return@mapNotNull null
        OssLicenseEntry(name, String(blob, offset, length, Charsets.UTF_8).trim())
    }.distinctBy { it.name }.sortedBy { it.name.lowercase() }.toList()
}
