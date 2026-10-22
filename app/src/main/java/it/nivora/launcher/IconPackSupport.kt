package it.nivora.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

internal data class InstalledIconPack(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/**
 * Reads the long-established ADW/Nova icon-pack format.
 * App-filter XML and icon resources are loaded locally from the installed pack;
 * Nivora never downloads icons or submits app inventory to a service.
 */
internal object IconPackSupport {
    private val actionNames = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME"
    )
    private val contexts = ConcurrentHashMap<String, Context>()
    private val mappings = ConcurrentHashMap<String, Map<String, String>>()
    private val drawableIds = ConcurrentHashMap<String, Int>()

    fun discover(context: Context): List<InstalledIconPack> {
        val pm = context.packageManager
        val found = linkedMapOf<String, InstalledIconPack>()
        actionNames.forEach { action ->
            val intent = Intent(action)
            val resolved = runCatching {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }.getOrDefault(emptyList())
            resolved.forEach { info ->
                val activity = info.activityInfo ?: return@forEach
                val packageName = activity.packageName
                if (packageName == context.packageName || found.containsKey(packageName)) return@forEach
                val appInfo = activity.applicationInfo
                val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                    .getOrDefault(packageName)
                val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull() ?: return@forEach
                found[packageName] = InstalledIconPack(packageName, label, icon)
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    fun loadIcon(context: Context, packPackage: String, targetPackage: String): Drawable? {
        if (packPackage.isBlank()) return null
        val drawableName = mappings.getOrPut(packPackage) { readMappings(context, packPackage) }[targetPackage]
            ?: return null
        val packContext = packageContext(context, packPackage) ?: return null
        val cacheKey = "$packPackage|$drawableName"
        val id = drawableIds.getOrPut(cacheKey) {
            val resources = packContext.resources
            resources.getIdentifier(drawableName, "drawable", packPackage)
                .takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", packPackage)
        }
        if (id == 0) return null
        return runCatching {
            val drawable = if (Build.VERSION.SDK_INT >= 21) {
                packContext.resources.getDrawable(id, packContext.theme)
            } else {
                @Suppress("DEPRECATION")
                packContext.resources.getDrawable(id)
            }
            drawable.constantState?.newDrawable(packContext.resources)?.mutate() ?: drawable.mutate()
        }.getOrNull()
    }

    private fun packageContext(context: Context, packPackage: String): Context? {
        contexts[packPackage]?.let { return it }
        val created = runCatching {
            context.createPackageContext(packPackage, Context.CONTEXT_IGNORE_SECURITY)
        }.getOrNull() ?: return null
        contexts.putIfAbsent(packPackage, created)
        return contexts[packPackage] ?: created
    }

    private fun readMappings(context: Context, packPackage: String): Map<String, String> {
        val packContext = packageContext(context, packPackage) ?: return emptyMap()
        val resourceId = packContext.resources.getIdentifier("appfilter", "xml", packPackage)
        if (resourceId != 0) {
            val parsed = runCatching {
                packContext.resources.getXml(resourceId).use { parser -> parse(parser) }
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }

        for (assetName in listOf("appfilter.xml", "appfilter")) {
            val parsed = runCatching {
                packContext.assets.open(assetName).use { stream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(stream, "UTF-8")
                    parse(parser)
                }
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }
        return emptyMap()
    }

    private fun parse(parser: XmlPullParser): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    val value = component.substringAfter('{', component).substringBefore('}')
                    val targetPackage = value.substringBefore('/').trim()
                    if (targetPackage.isNotBlank() && targetPackage.contains('.')) {
                        result.putIfAbsent(targetPackage, drawable)
                    }
                }
            }
            event = parser.next()
        }
        return result
    }
}
