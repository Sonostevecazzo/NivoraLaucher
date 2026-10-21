package it.nivora.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

private data class LaunchableApp(val label: String, val packageName: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NivoraApp() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NivoraApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nivora_preferences", 0) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var grid by remember { mutableStateOf(prefs.getBoolean("grid", true)) }
    var showLabels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact", false)) }
    var columns by remember { mutableIntStateOf(prefs.getInt("columns", 4).coerceIn(3, 5)) }
    var iconSize by remember { mutableIntStateOf(prefs.getInt("iconSize", 50).coerceIn(40, 64)) }
    var fontChoice by remember { mutableStateOf(prefs.getString("fontChoice", "Mono") ?: "Mono") }
    var wallpaper by remember { mutableStateOf(prefs.getString("wallpaper", "OLED") ?: "OLED") }
    var query by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLogo by remember { mutableStateOf(prefs.getBoolean("showLogo", true)) }
    var searchExpanded by remember { mutableStateOf(false) }
    val availableThemes = setOf("Mono Glass", "Frost", "Ember", "Moss", "Violet")
    var themePreset by remember {
        mutableStateOf((prefs.getString("themePreset", "Mono Glass") ?: "Mono Glass")
            .let { saved -> if (saved in availableThemes) saved else "Mono Glass" })
    }
    var monochromeIcons by remember { mutableStateOf(prefs.getBoolean("monochromeIcons", true)) }
    var customWallpaperUri by remember { mutableStateOf(prefs.getString("customWallpaperUri", null)) }
    var customWallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showArcade by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var favoritesOnly by remember { mutableStateOf(true) }
    var recentApps by remember { mutableStateOf(prefs.getString("recentApps", "")?.split("|")?.filter { it.isNotBlank() } ?: emptyList()) }
    var sortBy by remember { mutableStateOf((prefs.getString("sortBy", "Nome") ?: "Nome").let { if (it == "Name") "Nome" else it }) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var clock by remember { mutableStateOf(LocalDateTime.now()) }

    BackHandler(
        enabled = showArcade || showSettings || searchExpanded || showAllApps
    ) {
        when {
            showArcade -> showArcade = false
            showSettings -> showSettings = false
            searchExpanded -> { searchExpanded = false; query = "" }
            showAllApps -> { showAllApps = false; query = ""; favoritesOnly = false }
        }
    }

    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            customWallpaperUri = uri.toString()
            prefs.edit().putString("customWallpaperUri", uri.toString()).apply()
        }
    }

    LaunchedEffect(customWallpaperUri) {
        val savedUri = customWallpaperUri
        if (savedUri.isNullOrBlank()) {
            customWallpaperBitmap = null
        } else {
            val bitmap = withContext(Dispatchers.IO) { decodeSampledWallpaper(context, Uri.parse(savedUri)) }
            customWallpaperBitmap = bitmap
            if (bitmap == null) {
                // URI access can expire after a restore; clear the invalid selection gracefully.
                customWallpaperUri = null
                prefs.edit().remove("customWallpaperUri").apply()
            }
        }
    }

    suspend fun loadApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        activities.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            try {
                LaunchableApp(
                    label = info.loadLabel(pm).toString(),
                    packageName = activityInfo.packageName,
                    icon = info.loadIcon(pm)
                )
            } catch (_: Exception) { null }
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    suspend fun refreshLaunchableApps() {
        loading = true
        val loadedApps = loadApps()
        apps = loadedApps
        val installedPackages = loadedApps.map { it.packageName }.toSet()
        val cleanedFavorites = favorites.intersect(installedPackages)
        if (cleanedFavorites != favorites) {
            favorites = cleanedFavorites
            prefs.edit().putStringSet("favorites", cleanedFavorites).apply()
        }
        val cleanedRecents = recentApps.filter { it in installedPackages }.distinct().take(30)
        if (cleanedRecents != recentApps) {
            recentApps = cleanedRecents
            prefs.edit().putString("recentApps", cleanedRecents.joinToString("|")).apply()
        }
        loading = false
    }

    // Refresh the catalog when this Home activity resumes after another app, Settings or an install.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            refreshLaunchableApps()
        }
    }
    LaunchedEffect(Unit) {
        while (true) { clock = LocalDateTime.now(); delay(30_000) }
    }

    val trimmedQuery = query.trim()
    // Searches must always cover the full catalog, regardless of the favorites-only Home filter.
    val filteredApps = apps.filter { app ->
        val matchesQuery = trimmedQuery.isEmpty() ||
            app.label.contains(trimmedQuery, ignoreCase = true) ||
            app.packageName.contains(trimmedQuery, ignoreCase = true)
        val matchesFavoriteFilter = trimmedQuery.isNotEmpty() || !favoritesOnly || favorites.contains(app.packageName)
        matchesQuery && matchesFavoriteFilter
    }
    val visibleApps = when (sortBy) {
        "Recenti" -> filteredApps.sortedWith(compareBy<LaunchableApp> { val index = recentApps.indexOf(it.packageName); if (index < 0) Int.MAX_VALUE else index }.thenBy { it.label.lowercase() })
        "Preferiti" -> filteredApps.sortedWith(compareByDescending<LaunchableApp> { favorites.contains(it.packageName) }.thenBy { it.label.lowercase() })
        else -> filteredApps.sortedBy { it.label.lowercase() }
    }
    val accent = when (themePreset) {
        "Frost" -> Color(0xFF9FC8D8)
        "Ember" -> Color(0xFFD99A83)
        "Moss" -> Color(0xFF9EAF96)
        "Violet" -> Color(0xFFB5A5D5)
        else -> Color(0xFFB8C0C8)
    }
    val bg = when (wallpaper) {
        "Graphite" -> if (dark) Color(0xFF202326) else Color(0xFFE0E3E4)
        "Warm" -> if (dark) Color(0xFF2A211E) else Color(0xFFF0E9DE)
        "Paper" -> if (dark) Color(0xFF191A18) else Color(0xFFF7F7F2)
        "Gradient" -> if (dark) Color(0xFF191D27) else Color(0xFFE5E9F2)
        "Sunset" -> if (dark) Color(0xFF30202C) else Color(0xFFF4E5EA)
        else -> when (themePreset) {
            "Frost" -> if (dark) Color(0xFF111A20) else Color(0xFFEAF3F6)
            "Ember" -> if (dark) Color(0xFF211817) else Color(0xFFF5ECE8)
            "Moss" -> if (dark) Color(0xFF151B16) else Color(0xFFEEF2EB)
            "Violet" -> if (dark) Color(0xFF1A1721) else Color(0xFFF0ECF5)
            else -> if (dark) Color(0xFF101214) else Color(0xFFF1F2F2)
        }
    }
    val fg = if (dark) Color(0xFFF4F4F0) else Color(0xFF111212)
    val muted = if (dark) Color(0xFFB8BEBA) else Color(0xFF555955)
    val panel = if (dark) Color(0xFF30363A).copy(alpha = 0.62f) else Color(0xFFDDE2E4).copy(alpha = 0.72f)
    // Dialog content stays opaque; translucent glass is reserved for cards and the Home surface.
    val dialogSurface = if (dark) Color(0xFF171A1E) else Color(0xFFF7F8F9)
    val selectedFont = when (fontChoice) {
        "Mono iOS", "Mono" -> FontFamily.Monospace
        "Serif iOS", "Serif" -> FontFamily.Serif
        "Cursive" -> FontFamily.Cursive
        "Rounded iOS", "Rounded" -> FontFamily(Typeface.create("sans-serif-rounded", Typeface.NORMAL))
        "Condensed" -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
        "iOS Style" -> FontFamily(Typeface.create("sans-serif", Typeface.NORMAL))
        "iOS Light" -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
        "iOS Medium" -> FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        else -> FontFamily.Default
    }
    val customTypography = Typography().let { base -> base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = selectedFont), displayMedium = base.displayMedium.copy(fontFamily = selectedFont),
        displaySmall = base.displaySmall.copy(fontFamily = selectedFont), headlineLarge = base.headlineLarge.copy(fontFamily = selectedFont),
        headlineMedium = base.headlineMedium.copy(fontFamily = selectedFont), headlineSmall = base.headlineSmall.copy(fontFamily = selectedFont),
        titleLarge = base.titleLarge.copy(fontFamily = selectedFont), titleMedium = base.titleMedium.copy(fontFamily = selectedFont),
        titleSmall = base.titleSmall.copy(fontFamily = selectedFont), bodyLarge = base.bodyLarge.copy(fontFamily = selectedFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = selectedFont), bodySmall = base.bodySmall.copy(fontFamily = selectedFont),
        labelLarge = base.labelLarge.copy(fontFamily = selectedFont), labelMedium = base.labelMedium.copy(fontFamily = selectedFont),
        labelSmall = base.labelSmall.copy(fontFamily = selectedFont)
    ) }

    MaterialTheme(colorScheme = if (fg != Color(0xFF111212)) darkColorScheme(primary = accent, background = bg, surface = panel, onBackground = fg, onSurface = fg)
        else lightColorScheme(primary = accent, background = bg, surface = panel, onBackground = fg, onSurface = fg), typography = customTypography) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            val backdrop = when (wallpaper) {
                "Gradient" -> if (dark)
                    Brush.verticalGradient(listOf(Color(0xFF242833), bg, Color(0xFF090A0A)))
                else
                    Brush.verticalGradient(listOf(Color(0xFFF7F8FB), bg, Color(0xFFDCE2EF)))
                "Sunset" -> if (dark)
                    Brush.verticalGradient(listOf(Color(0xFF4B253F), Color(0xFF1A202C), Color(0xFF090A0A)))
                else
                    Brush.verticalGradient(listOf(Color(0xFFFFF0E9), bg, Color(0xFFE8D9E8)))
                else -> Brush.verticalGradient(listOf(bg, bg))
            }
            Box(Modifier.fillMaxSize()) {
                if (customWallpaperBitmap != null) {
                    Image(bitmap = customWallpaperBitmap!!.asImageBitmap(), contentDescription = "Sfondo personale", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(if (dark) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.10f)))
                } else {
                    Box(Modifier.fillMaxSize().background(backdrop))
                }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showLogo) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                        Text("N I V O R A", color = fg, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showArcade = true }) { Icon(Icons.Default.SportsEsports, contentDescription = "Giochi offline", tint = accent) }
                    if (!showAllApps) IconButton(onClick = { searchExpanded = !searchExpanded; if (!searchExpanded) query = "" }) { Icon(Icons.Default.Search, contentDescription = "Cerca app", tint = fg) }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = fg)
                    }
                }

                if (!showAllApps) {
                    Spacer(Modifier.height(28.dp))
                    Text(clock.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ITALIAN)).uppercase(Locale.ITALIAN),
                        color = muted, fontSize = 11.sp, letterSpacing = 2.sp)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp, bottom = 22.dp)) {
                        Text(clock.format(DateTimeFormatter.ofPattern("HH:mm")), color = fg, fontSize = 66.sp,
                            fontWeight = if (fontChoice == "iOS Style" || fontChoice == "Rounded iOS") FontWeight.Normal else FontWeight.Light,
                            fontFamily = if (fontChoice == "Mono iOS") FontFamily.Monospace else selectedFont,
                            letterSpacing = if (fontChoice == "iOS Style") (-2).sp else (-3).sp, lineHeight = 70.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("●", color = accent, fontSize = 9.sp, modifier = Modifier.padding(bottom = 15.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Text("PREFERITE", color = muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("${favorites.size} SALVATE", color = muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                } else {
                    Row(Modifier.padding(top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("APPLICAZIONI", color = fg, fontSize = 22.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Light)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { grid = !grid; prefs.edit().putBoolean("grid", grid).apply() }) {
                            Icon(if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView, contentDescription = "Cambia disposizione", tint = fg)
                        }
                    }
                }

                if (showAllApps || searchExpanded) PrismSurface(modifier = Modifier.fillMaxWidth(), tint = panel, shape = RoundedCornerShape(18.dp)) {
                  Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(value = query, onValueChange = { query = it; if (it.isNotBlank()) { showAllApps = true; favoritesOnly = false } },
                        singleLine = true, textStyle = TextStyle(color = fg, fontSize = 15.sp),
                        cursorBrush = SolidColor(accent), modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Cerca un'app o un pacchetto", color = muted, fontSize = 15.sp)
                            inner()
                        })
                    if (query.isNotEmpty()) Text("×", color = muted, fontSize = 22.sp,
                        modifier = Modifier.combinedClickable(onClick = { query = "" }, onLongClick = { query = "" }).padding(horizontal = 4.dp))
                  }
                }
                if (showAllApps || searchExpanded) Spacer(Modifier.height(12.dp))

                if (showAllApps) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false }, label = { Text("Tutte") })
                        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = true }, label = { Text("Preferite") })
                        Spacer(Modifier.weight(1f))
                        var sortExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { sortExpanded = true }) { Text(sortBy.uppercase(), fontSize = 10.sp, color = accent) }
                            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                                listOf("Nome", "Preferiti", "Recenti").forEach { choice ->
                                    DropdownMenuItem(text = { Text(choice) }, onClick = {
                                        sortBy = choice
                                        prefs.edit().putString("sortBy", choice).apply()
                                        sortExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }

                if (loading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                } else if (visibleApps.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (!showAllApps && favoritesOnly) "TIENI PREMUTA UN’APP PER SALVARLA"
                                else if (favoritesOnly) "NESSUNA APP PREFERITA" else "NESSUNA APP TROVATA",
                                color = muted, fontSize = 11.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center
                            )
                            if (!showAllApps) {
                                TextButton(onClick = { showAllApps = true; query = ""; favoritesOnly = false }) {
                                    Text("APRI TUTTE LE APP", color = accent)
                                }
                            } else if (favoritesOnly) {
                                TextButton(onClick = { favoritesOnly = false }) {
                                    Text("MOSTRA TUTTE", color = accent)
                                }
                            }
                        }
                    }
                } else if (grid) {
                    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp)) {
                        gridItems(visibleApps, key = { it.packageName }) { app ->
                            AppTile(app, favorites.contains(app.packageName), showLabels, fg, compact, iconSize, fontChoice, if (monochromeIcons) accent else null,
                                onClick = {
                                    launchApp(context, app) { packageName ->
                                        recentApps = (listOf(packageName) + recentApps.filterNot { it == packageName }).take(30)
                                        prefs.edit().putString("recentApps", recentApps.joinToString("|")).apply()
                                    }
                                },
                                onLongClick = {
                                    favorites = toggleFavorite(favorites, app.packageName, prefs)
                                    Toast.makeText(context, if (favorites.contains(app.packageName)) "Preferito aggiunto" else "Preferito rimosso", Toast.LENGTH_SHORT).show()
                                })
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppListRow(app, favorites.contains(app.packageName), fg, muted, if (monochromeIcons) accent else null,
                                onClick = { launchApp(context, app) { packageName ->
                                    recentApps = (listOf(packageName) + recentApps.filterNot { it == packageName }).take(30)
                                    prefs.edit().putString("recentApps", recentApps.joinToString("|")).apply()
                                } },
                                onLongClick = {
                                    favorites = toggleFavorite(favorites, app.packageName, prefs)
                                    Toast.makeText(context, if (favorites.contains(app.packageName)) "Preferito aggiunto" else "Preferito rimosso", Toast.LENGTH_SHORT).show()
                                })
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BottomAction("HOME", !showAllApps, accent, fg, panel) { showAllApps = false; query = ""; searchExpanded = false; favoritesOnly = true }
                    BottomAction("APP", showAllApps, accent, fg, panel) { showAllApps = true; query = ""; favoritesOnly = false }
                }
            }
            }
        }
    }

    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f).clip(RoundedCornerShape(28.dp)),
                color = dialogSurface
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("IMPOSTAZIONI", color = fg, fontSize = 19.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.SemiBold)
                            Text("Un’interfaccia essenziale, a modo tuo.", color = muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { showSettings = false }) { Text("×", color = fg, fontSize = 28.sp, fontWeight = FontWeight.Light) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSection("Aspetto e temi", "Palette curate e superfici ispirate al glass design.", accent) {
                            ThemePresetPicker(themePreset, accent) { selected ->
                                themePreset = selected
                                prefs.edit().putString("themePreset", selected).apply()
                            }
                            HorizontalDivider(color = muted.copy(alpha = 0.18f))
                            SettingSwitch("Modalità scura", dark) { dark = it; prefs.edit().putBoolean("dark", it).apply() }
                        }
                        SettingsSection("Schermata Home", "Organizza la schermata principale.", accent) {
                            SettingSwitch("Mostra logo Nivora", showLogo) { showLogo = it; prefs.edit().putBoolean("showLogo", it).apply() }
                            SettingSwitch("Mostra nomi delle app", showLabels) { showLabels = it; prefs.edit().putBoolean("labels", it).apply() }
                            SettingSwitch("Layout compatto", compact) { compact = it; prefs.edit().putBoolean("compact", it).apply() }
                            SettingChoice("Disposizione", if (grid) "Griglia" else "Lista", "Griglia", "Lista") {
                                grid = it == "Griglia"; prefs.edit().putBoolean("grid", grid).apply()
                            }
                            SettingChoice("Colonne griglia", "$columns", "3", "4", "5") {
                                columns = it.toInt(); prefs.edit().putInt("columns", columns).apply()
                            }
                        }
                        SettingsSection("Icone e applicazioni", "Uniformità visiva e accesso rapido.", accent) {
                            SettingSwitch("Icone monocromatiche Nivora", monochromeIcons) {
                                monochromeIcons = it; prefs.edit().putBoolean("monochromeIcons", it).apply()
                            }
                            SettingChoice("Dimensione icone", "$iconSize dp", "40 dp", "50 dp", "60 dp", "64 dp") {
                                iconSize = it.removeSuffix(" dp").toInt().coerceIn(40, 64)
                                prefs.edit().putInt("iconSize", iconSize).apply()
                            }
                            SettingChoice("Ordina applicazioni", sortBy, "Nome", "Preferiti", "Recenti") {
                                sortBy = it; prefs.edit().putString("sortBy", it).apply()
                            }
                        }
                        SettingsSection("Tipografia", "Scegli il carattere per orologio e nomi delle app.", accent) {
                            SettingChoice("Carattere", fontChoice,
                                "System", "iOS Style", "iOS Light", "iOS Medium", "Rounded iOS",
                                "Mono iOS", "Serif iOS", "Condensed", "Rounded", "Mono", "Serif", "Cursive") {
                                fontChoice = it; prefs.edit().putString("fontChoice", it).apply()
                            }
                        }
                        SettingsSection("Sfondi", "Scegli una superficie oppure usa una tua immagine.", accent) {
                            SettingChoice("Stile dello sfondo", wallpaper, "OLED", "Graphite", "Warm", "Paper", "Gradient", "Sunset") {
                                wallpaper = it; prefs.edit().putString("wallpaper", it).apply()
                            }
                            OutlinedButton(
                                onClick = { pickWallpaper.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                            ) { Text(if (customWallpaperUri == null) "Scegli una foto dalla galleria" else "Cambia foto personale") }
                            if (customWallpaperUri != null) {
                                OutlinedButton(
                                    onClick = { customWallpaperUri = null; customWallpaperBitmap = null; prefs.edit().remove("customWallpaperUri").apply() },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                                ) { Text("Rimuovi foto personale") }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { showSettings = false }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("FATTO", letterSpacing = 1.2.sp)
                    }
                }
            }
        }
    }

    if (showArcade) {
        Dialog(onDismissRequest = { showArcade = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.97f).fillMaxHeight(0.94f).clip(RoundedCornerShape(28.dp)), color = bg) {
                OfflineArcade(accent = accent, fg = fg, panel = panel, dark = dark, monochrome = themePreset == "Mono Glass", onClose = { showArcade = false })
            }
        }
    }


}

private fun decodeSampledWallpaper(context: android.content.Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, options) }
    }.getOrNull()

@Composable
private fun PrismSurface(modifier: Modifier = Modifier, tint: Color, shape: Shape, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "prism-flow")
    val sweep by transition.animateFloat(initialValue = -0.65f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 6200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "prism-sweep")
    Box(modifier = modifier.clip(shape).background(tint).drawBehind {
        val start = size.width * (sweep - 0.35f)
        drawRect(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.055f), tint.copy(alpha = 0.16f), Color.Transparent),
            start = Offset(start, 0f), end = Offset(start + size.width * 0.52f, size.height)))
    }.border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.23f), tint.copy(alpha = 0.14f), Color.White.copy(alpha = 0.035f))), shape), content = content)
}

private enum class ArcadeGame { MENU, SNAKE, BLOCKFALL }
internal enum class SnakeDirection(val dx: Int, val dy: Int) {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0)
}
internal data class SnakeState(
    val body: List<Pair<Int, Int>> = listOf(7 to 10, 6 to 10, 5 to 10),
    val food: Pair<Int, Int> = 11 to 10,
    val score: Int = 0,
    val over: Boolean = false
)
internal data class BlockState(
    val board: List<List<Int>> = List(18) { List(10) { 0 } },
    val pieceType: Int = Random.nextInt(1, 8),
    val pieceX: Int = 3,
    val pieceY: Int = 0,
    val rotation: Int = 0,
    val score: Int = 0,
    val over: Boolean = false
)

private data class ArcadePalette(
    val accent: Color,
    val food: Color,
    val board: Color,
    val grid: Color,
    val blocks: List<Color>
)

private fun createArcadePalette(accent: Color, foreground: Color, dark: Boolean, monochrome: Boolean): ArcadePalette {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    fun tone(shift: Float, value: Float): Color {
        val hue = (hsv[0] + shift + 360f) % 360f
        val saturation = if (monochrome) 0.025f else hsv[1].coerceAtLeast(0.42f).coerceAtMost(0.92f)
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }
    val blockColors = listOf(Color.Transparent) + if (monochrome) {
        listOf(Color(0xFFE7EAED), Color(0xFFD1D6DB), Color(0xFFBCC3CA), Color(0xFFA5AEB7),
            Color(0xFF909AA4), Color(0xFF7A858F), Color(0xFFF5F6F7))
    } else {
        listOf(-34f, 0f, 34f, 68f, 126f, 174f, 218f).map { tone(it, if (dark) 0.94f else 0.48f) }
    }
    return ArcadePalette(
        accent = if (dark) accent else tone(0f, 0.36f),
        food = if (monochrome) foreground else tone(155f, if (dark) 0.98f else 0.38f),
        board = if (dark) Color(0xFF080B0F) else Color(0xFFF7F9FB),
        grid = foreground.copy(alpha = if (dark) 0.085f else 0.11f),
        blocks = blockColors
    )
}

@Composable
private fun OfflineArcade(accent: Color, fg: Color, panel: Color, dark: Boolean, monochrome: Boolean, onClose: () -> Unit) {
    var selected by remember { mutableStateOf(ArcadeGame.MENU) }
    val gamePalette = remember(accent, fg, dark, monochrome) { createArcadePalette(accent, fg, dark, monochrome) }
    val arcadeBackdrop = if (dark) listOf(panel.copy(alpha = 0.7f), Color(0xFF0B0D10)) else listOf(panel.copy(alpha = 0.72f), Color(0xFFF5F7F8))
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(arcadeBackdrop)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("N I V O R A  /  ARCADE", color = accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text(if (selected == ArcadeGame.MENU) "Piccole pause, zero rete." else if (selected == ArcadeGame.SNAKE) "SNAKE" else "BLOCKFALL",
                    color = fg, fontSize = 23.sp, fontWeight = FontWeight.Light)
            }
            TextButton(onClick = { if (selected == ArcadeGame.MENU) onClose() else selected = ArcadeGame.MENU }) {
                Text(if (selected == ArcadeGame.MENU) "CHIUDI  ×" else "← GIOCHI", color = accent)
            }
        }
        when (selected) {
            ArcadeGame.MENU -> {
                Text("Tre regole semplici: entra, gioca, riprendi da dove eri. Tutto gira in locale.", color = fg.copy(alpha = 0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                PrismSurface(Modifier.fillMaxWidth(), panel, RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { selected = ArcadeGame.SNAKE }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("〰", color = accent, fontSize = 36.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("SNAKE", color = fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Raccogli i nuclei di luce. Evita pareti e coda.", color = fg.copy(alpha = 0.65f), fontSize = 12.sp)
                        }
                        Text("→", color = accent, fontSize = 24.sp)
                    }
                }
                PrismSurface(Modifier.fillMaxWidth(), panel, RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { selected = ArcadeGame.BLOCKFALL }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("▦", color = accent, fontSize = 34.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("BLOCKFALL", color = fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Blocchi in caduta, rotazione e righe da liberare.", color = fg.copy(alpha = 0.65f), fontSize = 12.sp)
                        }
                        Text("→", color = accent, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("PRISM ARCADE  ·  OFFLINE  ·  NESSUN ACCOUNT", color = fg.copy(alpha = 0.42f), fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
            ArcadeGame.SNAKE -> SnakeGame(gamePalette, fg, Modifier.weight(1f))
            ArcadeGame.BLOCKFALL -> BlockfallGame(gamePalette, fg, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SnakeGame(palette: ArcadePalette, fg: Color, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(SnakeState()) }
    var direction by remember { mutableStateOf(SnakeDirection.RIGHT) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running, state.over) {
        if (running && !state.over) {
            while (true) {
                delay(175)
                state = advanceSnake(state, direction)
                if (state.over) { running = false; break }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("PUNTEGGIO", color = fg.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text(state.score.toString(), color = palette.accent, fontSize = 25.sp, fontWeight = FontWeight.Light)
        }
        TextButton(onClick = { running = !running && !state.over }) { Text(if (running) "PAUSA" else if (state.over) "FINE PARTITA" else "GIOCA", color = palette.accent) }
        TextButton(onClick = { state = SnakeState(); direction = SnakeDirection.RIGHT; running = false }) { Text("RESET", color = fg) }
    }
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.board)) {
        val cell = minOf(size.width / 16f, size.height / 22f)
        val left = (size.width - cell * 16) / 2f
        val top = (size.height - cell * 22) / 2f
        for (x in 0..16) drawLine(palette.grid, Offset(left + x * cell, top), Offset(left + x * cell, top + 22 * cell), 1.dp.toPx())
        for (y in 0..22) drawLine(palette.grid, Offset(left, top + y * cell), Offset(left + 16 * cell, top + y * cell), 1.dp.toPx())
        val food = state.food
        drawCircle(color = palette.food, radius = cell * 0.34f, center = Offset(left + (food.first + 0.5f) * cell, top + (food.second + 0.5f) * cell))
        state.body.forEachIndexed { index, p ->
            drawRoundRect(color = if (index == 0) palette.accent else palette.accent.copy(alpha = 0.68f - (index.coerceAtMost(20) * 0.015f)),
                topLeft = Offset(left + p.first * cell + cell * 0.07f, top + p.second * cell + cell * 0.07f),
                size = androidx.compose.ui.geometry.Size(cell * 0.86f, cell * 0.86f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.2f))
        }
        if (state.over) drawRect(Color.Black.copy(alpha = 0.52f), topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cell * 16, cell * 22))
    }
    if (state.over) Text("Partita terminata — premi RESET per riprovare", color = fg.copy(alpha = 0.7f), fontSize = 12.sp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = { if (direction != SnakeDirection.DOWN) direction = SnakeDirection.UP }, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 5.dp)) { Text("↑", color = palette.accent, fontSize = 20.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (direction != SnakeDirection.RIGHT) direction = SnakeDirection.LEFT }, contentPadding = PaddingValues(horizontal = 23.dp, vertical = 5.dp)) { Text("←", color = palette.accent, fontSize = 20.sp) }
            OutlinedButton(onClick = { if (direction != SnakeDirection.UP) direction = SnakeDirection.DOWN }, contentPadding = PaddingValues(horizontal = 23.dp, vertical = 5.dp)) { Text("↓", color = palette.accent, fontSize = 20.sp) }
            OutlinedButton(onClick = { if (direction != SnakeDirection.LEFT) direction = SnakeDirection.RIGHT }, contentPadding = PaddingValues(horizontal = 23.dp, vertical = 5.dp)) { Text("→", color = palette.accent, fontSize = 20.sp) }
        }
    }
    Text("Comandi a schermo · nessuna connessione richiesta", color = fg.copy(alpha = 0.42f), fontSize = 10.sp)
}

internal fun advanceSnake(state: SnakeState, direction: SnakeDirection): SnakeState {
    if (state.over) return state
    val head = state.body.first()
    val next = (head.first + direction.dx) to (head.second + direction.dy)
    val eating = next == state.food
    val collisionBody = if (eating) state.body else state.body.dropLast(1)
    if (next.first !in 0 until 16 || next.second !in 0 until 22 || next in collisionBody) return state.copy(over = true)
    val newBody = listOf(next) + if (eating) state.body else state.body.dropLast(1)
    if (!eating) return state.copy(body = newBody)
    val free = (0 until 22).flatMap { y -> (0 until 16).map { x -> x to y } }.filterNot { it in newBody }
    if (free.isEmpty()) return state.copy(body = newBody, score = state.score + 10, over = true)
    return state.copy(body = newBody, food = free.random(), score = state.score + 10)
}

private val BLOCK_SHAPES: Map<Int, List<Pair<Int, Int>>> = mapOf(
    1 to listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1),
    2 to listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1),
    3 to listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1),
    4 to listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
    5 to listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
    6 to listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1),
    7 to listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1)
)
internal fun blockCells(type: Int, rotation: Int): List<Pair<Int, Int>> {
    var cells = BLOCK_SHAPES[type] ?: BLOCK_SHAPES.getValue(1)
    repeat(Math.floorMod(rotation, 4)) { cells = cells.map { (x, y) -> (3 - y) to x } }
    return cells
}

internal fun canPlaceBlock(state: BlockState, x: Int, y: Int, rotation: Int): Boolean =
    blockCells(state.pieceType, rotation).all { (dx, dy) ->
        val px = x + dx; val py = y + dy
        px in 0 until 10 && py < 18 && (py < 0 || state.board[py][px] == 0)
    }

internal fun moveBlock(state: BlockState, dx: Int = 0, dy: Int = 0, rotate: Boolean = false, hardDrop: Boolean = false): BlockState {
    if (state.over) return state
    var x = state.pieceX; var y = state.pieceY; var rotation = state.rotation
    if (rotate) {
        if (canPlaceBlock(state, x, y, rotation + 1)) rotation++
    } else if (dx != 0) {
        if (canPlaceBlock(state, x + dx, y, rotation)) x += dx
    } else if (hardDrop) {
        while (canPlaceBlock(state, x, y + 1, rotation)) y++
        return lockBlock(state.copy(pieceX = x, pieceY = y, rotation = rotation))
    } else if (canPlaceBlock(state, x, y + dy, rotation)) {
        y += dy
    } else {
        return lockBlock(state)
    }
    return state.copy(pieceX = x, pieceY = y, rotation = rotation)
}

internal fun lockBlock(state: BlockState): BlockState {
    val mutable = state.board.map { it.toMutableList() }
    blockCells(state.pieceType, state.rotation).forEach { (dx, dy) ->
        val x = state.pieceX + dx; val y = state.pieceY + dy
        if (x in 0 until 10 && y in 0 until 18) mutable[y][x] = state.pieceType
    }
    val fullRows = mutable.count { row -> row.all { it != 0 } }
    val remaining = mutable.filter { row -> row.any { it == 0 } }.map { it.toList() }
    val newBoard = List(fullRows) { List(10) { 0 } } + remaining
    val next = BlockState(board = newBoard, score = state.score + fullRows * 100)
    return if (canPlaceBlock(next, next.pieceX, next.pieceY, next.rotation)) next else next.copy(over = true)
}

@Composable
private fun BlockfallGame(palette: ArcadePalette, fg: Color, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(BlockState()) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running, state.over) {
        if (running && !state.over) {
            while (true) {
                delay(480)
                state = moveBlock(state, dy = 1)
                if (state.over) { running = false; break }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("PUNTEGGIO", color = fg.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text(state.score.toString(), color = palette.accent, fontSize = 25.sp, fontWeight = FontWeight.Light)
        }
        TextButton(onClick = { running = !running && !state.over }) { Text(if (running) "PAUSA" else if (state.over) "FINE PARTITA" else "GIOCA", color = palette.accent) }
        TextButton(onClick = { state = BlockState(); running = false }) { Text("RESET", color = fg) }
    }
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.board)) {
        val cell = minOf(size.width / 10f, size.height / 18f)
        val left = (size.width - cell * 10) / 2f
        val top = (size.height - cell * 18) / 2f
        for (y in 0 until 18) for (x in 0 until 10) {
            val v = state.board[y][x]
            if (v != 0) drawRoundRect(palette.blocks[v], Offset(left + x * cell + 1, top + y * cell + 1),
                androidx.compose.ui.geometry.Size(cell - 2, cell - 2), androidx.compose.ui.geometry.CornerRadius(cell * 0.12f))
        }
        blockCells(state.pieceType, state.rotation).forEach { (dx, dy) ->
            val x = state.pieceX + dx; val y = state.pieceY + dy
            if (x in 0 until 10 && y in 0 until 18) drawRoundRect(palette.blocks[state.pieceType], Offset(left + x * cell + 1, top + y * cell + 1),
                androidx.compose.ui.geometry.Size(cell - 2, cell - 2), androidx.compose.ui.geometry.CornerRadius(cell * 0.12f))
        }
        for (x in 0..10) drawLine(palette.grid, Offset(left + x * cell, top), Offset(left + x * cell, top + 18 * cell), 1.dp.toPx())
        for (y in 0..18) drawLine(palette.grid, Offset(left, top + y * cell), Offset(left + 10 * cell, top + y * cell), 1.dp.toPx())
        if (state.over) drawRect(Color.Black.copy(alpha = 0.55f), Offset(left, top), androidx.compose.ui.geometry.Size(10 * cell, 18 * cell))
    }
    if (state.over) Text("Blocco impilato — RESET per una nuova partita", color = fg.copy(alpha = 0.7f), fontSize = 12.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedButton(onClick = { state = moveBlock(state, dx = -1) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("←", color = palette.accent, fontSize = 22.sp) }
        OutlinedButton(onClick = { state = moveBlock(state, rotate = true) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("↻", color = palette.accent, fontSize = 22.sp) }
        OutlinedButton(onClick = { state = moveBlock(state, dx = 1) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("→", color = palette.accent, fontSize = 22.sp) }
        OutlinedButton(onClick = { state = moveBlock(state, hardDrop = true); if (state.over) running = false }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("↓↓", color = palette.accent, fontSize = 18.sp) }
    }
    Text("Sinistra · ruota · destra · caduta rapida", color = fg.copy(alpha = 0.42f), fontSize = 10.sp)
}


private fun launchApp(context: android.content.Context, app: LaunchableApp, onLaunched: (String) -> Unit) {
    val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
    if (launch != null) {
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launch)
            onLaunched(app.packageName)
        } catch (_: Exception) {
            Toast.makeText(context, "Impossibile aprire l'app", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun toggleFavorite(current: Set<String>, packageName: String, prefs: android.content.SharedPreferences): Set<String> {
    val next = current.toMutableSet()
    if (!next.add(packageName)) next.remove(packageName)
    prefs.edit().putStringSet("favorites", next).apply()
    return next.toSet()
}

/**
 * When monochrome mode is enabled, use the original Nivora glyph pack instead
 * of tinting arbitrary vendor artwork. This avoids black-background blobs and
 * gives the app drawer a consistent, recognizable icon language.
 */
private fun iconForDisplay(app: LaunchableApp, tint: Color?): Drawable {
    if (tint == null) {
        return app.icon.constantState?.newDrawable()?.mutate() ?: app.icon.mutate()
    }
    return NivoraIconDrawable(app.label, app.packageName, tint.toArgb())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: LaunchableApp, favorite: Boolean, showLabel: Boolean, fg: Color, compact: Boolean, iconSize: Int, fontChoice: String, iconTint: Color?,
                    onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(vertical = if (compact) 1.dp else 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                update = { view -> view.setImageDrawable(iconForDisplay(app, iconTint)); view.clearColorFilter() },
                modifier = Modifier.size(if (compact) iconSize.coerceAtMost(46).dp else iconSize.dp))
            if (favorite) Box(Modifier.padding(top = 1.dp, end = 1.dp).size(6.dp).clip(CircleShape).background(Color(0xFFD71920)))
        }
        if (showLabel) {
            Spacer(Modifier.height(7.dp))
            Text(app.label, color = fg, fontSize = if (compact) 9.sp else 10.sp, maxLines = 2, softWrap = true, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 12.sp,
                letterSpacing = if (fontChoice == "iOS Style") (-0.15).sp else 0.sp, fontWeight = if (fontChoice == "iOS Style") FontWeight.Medium else FontWeight.Normal)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListRow(app: LaunchableApp, favorite: Boolean, fg: Color, muted: Color, iconTint: Color?, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { view -> view.setImageDrawable(iconForDisplay(app, iconTint)); view.clearColorFilter() },
            modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, color = fg, fontSize = 15.sp, maxLines = 1)
            Text(app.packageName, color = muted, fontSize = 10.sp, maxLines = 1)
        }
        if (favorite) Text("●", color = Color(0xFFD71920), fontSize = 8.sp)
        else Text("↗", color = muted, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomAction(label: String, selected: Boolean, accent: Color, fg: Color, panel: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(0.48f).clip(RoundedCornerShape(14.dp)).background(if (selected) panel else Color.Transparent)
        .combinedClickable(onClick = onClick, onLongClick = {}).padding(vertical = 13.dp), horizontalArrangement = Arrangement.Center) {
        Text(label, color = if (selected) accent else fg, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    accent: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title.uppercase(Locale.ITALIAN), color = accent, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
        Text(description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 16.sp)
        content()
    }
}

@Composable
private fun ThemePresetPicker(selected: String, accent: Color, onSelect: (String) -> Unit) {
    val themes = listOf(
        Triple("Mono Glass", Color(0xFFB8C0C8), "Monocromo"),
        Triple("Frost", Color(0xFF9FC8D8), "Freddo e pulito"),
        Triple("Ember", Color(0xFFD99A83), "Caldo e morbido"),
        Triple("Moss", Color(0xFF9EAF96), "Naturale"),
        Triple("Violet", Color(0xFFB5A5D5), "Cromatico")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.chunked(3).forEach { rowThemes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowThemes.forEach { (name, color, subtitle) ->
                    val isSelected = selected == name
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                            .border(1.dp, if (isSelected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(15.dp))
                            .clickable { onSelect(name) }.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                            if (isSelected) Text("✓", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 9.sp, maxLines = 1)
                    }
                }
                repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SettingChoice(label: String, value: String, vararg choices: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        Box {
            TextButton(onClick = { expanded = true }) { Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { onChange(choice); expanded = false }) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}
