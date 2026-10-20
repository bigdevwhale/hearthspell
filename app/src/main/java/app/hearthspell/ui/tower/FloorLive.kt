package app.hearthspell.ui.tower

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.content.HelperLines
import app.hearthspell.content.L
import app.hearthspell.data.Floor
import app.hearthspell.data.claim
import app.hearthspell.ui.LocalFeel
import app.hearthspell.ui.Tone
import app.hearthspell.ui.WData
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.Nes
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.delay
import kotlin.math.floor

/** A line said over a floor scene, at [x] in scene pixels. [found] marks a picked-up trinket's name. */
private class Bubble(val line: L, val x: Int, val key: Long, val found: Boolean = false)

private val apprenticeLines = listOf(
    L("I live here now. It still feels like a dream.", "Я теперь тут живу. До сих пор как во сне."),
    L("Where did I put my wand? Oh, it's in my hand.", "Куда я дел{|а} палочку? А, она в руке."),
    L("Hello, tower!", "Привет, башня!"),
)
private val catLine = L("Prrrr...", "Мррр...")

/** A floor scene you can touch: pat the helpers, the cat and the apprentice, pick up gifts. */
@Composable
internal fun LiveFloor(data: WData, room: Floor, tick: State<Long>, sky: Sky) {
    val look = data.look(room, withApprentice = true)
    val current by rememberUpdatedState(look)
    val world by rememberUpdatedState(data.world)
    val m = SCENES.getValue(look.kind)
    val touch = remember { Touch() }
    val feel = LocalFeel.current
    val container = LocalContext.current.container
    var bubble by remember { mutableStateOf<Bubble?>(null) }
    var pats by remember { mutableIntStateOf(0) }
    val bg = ImageBitmap.imageResource(m.res)
    LaunchedEffect(bubble?.key) {
        if (bubble != null) {
            delay(2600)
            bubble = null
        }
    }
    BoxWithConstraints {
        val wpx = constraints.maxWidth.toFloat()
        val px = cellPx(wpx)
        val ox = floor((wpx - GW * px) / 2)
        SceneCanvas(
            SCENE_H,
            Modifier.pointerInput(wpx) {
                detectTapGestures { o ->
                    val lk = current
                    val gx = ((o.x - ox) / px).toInt() - TX
                    val gy = (o.y / px).toInt()
                    val t = tick.value
                    if (gy < SCENE_FLOOR - 40) return@detectTapGestures
                    val gift = world.giftOn(room.id)
                    if (gift != null && gx in GIFT_X - 4..GIFT_X + 18) {
                        container.write { claim(gift) }
                        feel?.tone(Tone.GIFT)
                        bubble = Bubble(gift.trinket.title, GIFT_X + 7, System.nanoTime(), found = true)
                        return@detectTapGestures
                    }
                    if (lk.cat && gx in lk.catX(m) - 2..lk.catX(m) + 30) {
                        touch.cat = t
                        feel?.purr()
                        bubble = Bubble(catLine, lk.catX(m) + 12, System.nanoTime())
                        return@detectTapGestures
                    }
                    lk.helperSpots(t, m).firstOrNull { (_, r) -> gx in r.first - 3..r.last + 3 }?.let { (kind, r) ->
                        touch.pats[kind] = t
                        feel?.tone(Tone.PAT, 0.3f)
                        bubble = Bubble(if (lk.asleep) HelperLines.sleepy else HelperLines.of(kind, pats++), (r.first + r.last) / 2, System.nanoTime())
                        return@detectTapGestures
                    }
                    if (lk.apprentice != null) {
                        val lanes = lk.helpers.size + 1
                        val s = lk.apprentice.invoke(0)
                        val (x, _) = if (lk.asleep) restX(lk.helpers.size, s.w, lk.id, m.walk, lanes) else helperX(lk.helpers.size, s.w, t, lk.id + 5, m.walk, lanes)
                        if (gx in x - 3..x + s.w + 3) {
                            touch.apprentice = t
                            feel?.tone(Tone.PAT, 0.3f)
                            bubble = Bubble(apprenticeLines[pats++ % apprenticeLines.size], x + s.w / 2, System.nanoTime())
                        }
                    }
                }
            },
        ) { drawFloor(current, bg, tick.value, sky, touch) }
        bubble?.let { b ->
            val density = LocalDensity.current
            val cx = with(density) { (ox + (TX + b.x) * px).toDp() }
            val text = if (b.found) gs(R.string.gift_picked, t(b.line)) else t(b.line)
            Text(
                text,
                style = UType.Small.copy(color = Nes.night),
                modifier = Modifier
                    .offset(x = (cx - 80.dp).coerceIn(8.dp, maxWidth - 168.dp), y = 8.dp)
                    .widthIn(max = 160.dp)
                    .pixelBox(Nes.cream, Nes.black, Nes.white)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
