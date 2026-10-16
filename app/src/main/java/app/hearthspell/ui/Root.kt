package app.hearthspell.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.hearthspell.BuildConfig
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.data.Settings
import app.hearthspell.domain.World
import app.hearthspell.notify.Alarms
import app.hearthspell.ui.art.Glyph
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.components.PX
import app.hearthspell.ui.components.rememberNow
import app.hearthspell.ui.decor.DecorScreen
import app.hearthspell.ui.grimoire.GrimoireTab
import app.hearthspell.ui.grimoire.HelperScreen
import app.hearthspell.ui.grimoire.SpellScreen
import app.hearthspell.ui.letters.LetterScreen
import app.hearthspell.ui.letters.LettersTab
import app.hearthspell.ui.letters.Letters
import app.hearthspell.ui.market.MarketTab
import app.hearthspell.ui.market.TripScreen
import app.hearthspell.ui.settings.SettingsScreen
import app.hearthspell.ui.setup.FloorFormScreen
import app.hearthspell.ui.setup.FloorsScreen
import app.hearthspell.ui.setup.OnboardingScreen
import app.hearthspell.ui.setup.SpellFormScreen
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import app.hearthspell.ui.tower.CantripsScreen
import app.hearthspell.ui.tower.FloorScreen
import app.hearthspell.ui.tower.TowerTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class Tab { TOWER, GRIMOIRE, MARKET, LETTERS }

/** Everything stored plus the settings, rebuilt once a minute or on any change. */
@Immutable
class WData(val world: World, val settings: Settings) {
    val now: Long get() = world.now
}

/** Everything a screen can open. Ids of 0 mean "new". */
class Go(private val nav: NavHostController) {
    fun floor(id: Long) = nav.navigate("floor/$id")
    /** The spell editor: an existing spell, or a new one on [floorId]. */
    fun spellForm(id: Long = 0, floorId: Long = 0) = nav.navigate("spellForm?id=$id&floor=$floorId")
    fun floorForm(id: Long = 0) = nav.navigate("floorForm/$id")
    fun floors() = nav.navigate("floors")
    /** A spell's page in the grimoire. */
    fun spell(id: Long) = nav.navigate("spell/$id")
    fun helper(kind: String) = nav.navigate("helper/$kind")
    /** The decor shop, placing on [floorId] (0 = into the chest). */
    fun decor(floorId: Long = 0) = nav.navigate("decor?floor=$floorId")
    fun letter(n: Int) = nav.navigate("letter/$n")
    fun trip() = nav.navigate("trip")
    fun cantrips() = nav.navigate("cantrips")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
    fun home() = nav.popBackStack("main", inclusive = false)
}

@Composable
fun HearthRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.TOWER) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.dao
    val floors by dao.observeFloors().collectAsStateWithLifecycle(initialValue = null)
    val spells by dao.observeSpells().collectAsStateWithLifecycle(initialValue = emptyList())
    val casts by dao.observeCasts().collectAsStateWithLifecycle(initialValue = emptyList())
    val reagents by dao.observeReagents().collectAsStateWithLifecycle(initialValue = emptyList())
    val decor by dao.observeDecor().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(60_000)
    val data = remember(floors, spells, casts, reagents, decor, settings, minute) {
        WData(
            World(floors.orEmpty(), spells, casts, reagents, decor, System.currentTimeMillis(), ZoneId.systemDefault(), settings.startDay),
            settings,
        )
    }

    // Onboarding finished elsewhere (the demo seed, a restored backup) while the welcome screen was up.
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && nav.currentDestination?.route == "welcome") {
            nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
        }
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded, floors != null) {
        val route = requested ?: return@LaunchedEffect
        if (!settings.onboarded || floors == null) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        when {
            route == "tower" -> tab = Tab.TOWER
            route.startsWith("floor/") -> { tab = Tab.TOWER; route.removePrefix("floor/").toLongOrNull()?.let { go.floor(it) } }
            route.startsWith("letter/") -> { tab = Tab.LETTERS; route.removePrefix("letter/").toIntOrNull()?.let { go.letter(it) } }
        }
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalPerson provides Person(settings.name, settings.gender)) {
        Box(Modifier.fillMaxSize().background(U.c.bg)) {
            NavHost(
                navController = nav,
                startDestination = start,
                enterTransition = { fadeIn(tween(160)) },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(160)) },
                popExitTransition = { fadeOut(tween(120)) },
            ) {
                composable("welcome") {
                    OnboardingScreen(data) { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
                }
                composable("main") { MainTabs(tab, { tab = it }, go, data) }
                composable("floor/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    FloorScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable(
                    "spellForm?id={id}&floor={floor}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("floor") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { e -> SpellFormScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getLong("floor") ?: 0L, data, go) }
                composable("floorForm/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    FloorFormScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable("floors") { FloorsScreen(data, go) }
                composable("spell/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    SpellScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable("helper/{kind}", arguments = listOf(navArgument("kind") { type = NavType.StringType })) { e ->
                    HelperScreen(e.arguments?.getString("kind").orEmpty(), data, go)
                }
                composable("decor?floor={floor}", arguments = listOf(navArgument("floor") { type = NavType.LongType; defaultValue = 0L })) { e ->
                    DecorScreen(e.arguments?.getLong("floor") ?: 0L, data, go)
                }
                composable("letter/{n}", arguments = listOf(navArgument("n") { type = NavType.IntType })) { e ->
                    LetterScreen(e.arguments?.getInt("n") ?: 0, data, go)
                }
                composable("trip") { TripScreen(data, go) }
                composable("cantrips") { CantripsScreen(data, go) }
                composable("settings") { SettingsScreen(data, go) }
            }
        }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: WData) {
    val unread = Letters.unread(data)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.TOWER -> TowerTab(data, go)
                Tab.GRIMOIRE -> GrimoireTab(data, go)
                Tab.MARKET -> MarketTab(data, go)
                Tab.LETTERS -> LettersTab(data, go)
            }
        }
        val c = U.c
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(c.surface)
                    drawRect(c.outline, Offset.Zero, Size(size.width, PX.toPx()))
                    drawRect(c.bevel, Offset(0f, PX.toPx()), Size(size.width, PX.toPx()))
                }
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Glyphs.tower, stringResource(R.string.tab_tower), tab == Tab.TOWER, badge = data.world.totalImps > 0) { onTab(Tab.TOWER) }
            NavItem(Glyphs.book, stringResource(R.string.tab_grimoire), tab == Tab.GRIMOIRE) { onTab(Tab.GRIMOIRE) }
            NavItem(Glyphs.basket, stringResource(R.string.tab_market), tab == Tab.MARKET, badge = data.world.shoppingList.isNotEmpty()) { onTab(Tab.MARKET) }
            NavItem(Glyphs.letter, stringResource(R.string.tab_letters), tab == Tab.LETTERS, badge = unread > 0) { onTab(Tab.LETTERS) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: Glyph, label: String, selected: Boolean, badge: Boolean = false, onClick: () -> Unit) {
    val c = U.c
    val color = if (selected) c.accent else c.faint
    Column(
        Modifier
            .weight(1f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            PixelIcon(icon, color, size = 24.dp)
            // The badge: one red pixel-square at the icon's corner.
            if (badge) Box(Modifier.align(Alignment.TopEnd).size(7.dp).background(c.danger))
        }
        Spacer(Modifier.height(5.dp))
        Text(label.uppercase(), style = UType.Eyebrow.copy(color = color), maxLines = 1)
    }
}
