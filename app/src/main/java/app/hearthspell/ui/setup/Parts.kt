package app.hearthspell.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hearthspell.R
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.domain.World
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.art.SpriteImage
import app.hearthspell.ui.components.pixelBox
import app.hearthspell.ui.gp
import app.hearthspell.ui.gs
import app.hearthspell.ui.t
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType

/** Top-to-bottom order a new tower gets: the observatory up high, the gate by the door. */
internal val towerOrder = listOf(
    FloorKind.BEDROOM, FloorKind.BALCONY, FloorKind.STUDY, FloorKind.KIDS, FloorKind.LIVING, FloorKind.KITCHEN,
    FloorKind.BATH, FloorKind.LAUNDRY, FloorKind.PETS, FloorKind.WHOLE, FloorKind.HALL,
)

/** "Every day", "every 3 days". */
@Composable
internal fun everyLabel(days: Int): String = if (days == 1) gs(R.string.setup_every_day) else gp(R.plurals.setup_every_days, days, days)

/** A selectable box for a kind of room: the real room and the floor it becomes. */
@Composable
internal fun KindTile(kind: FloorKind, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = U.c
    Row(
        modifier
            .pixelBox(if (selected) c.surface2 else c.surface, if (selected) c.accent else c.outline, c.bevel)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(selected)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t(kind.room), style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t(kind.magic), style = UType.Small.copy(color = if (selected) c.accent else c.faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** An 8-bit check box. */
@Composable
internal fun CheckBox(checked: Boolean, color: Color = U.c.accent) {
    val c = U.c
    Box(Modifier.size(24.dp).pixelBox(if (checked) color else c.surface2, c.outline), contentAlignment = Alignment.Center) {
        if (checked) PixelIcon(Glyphs.check, c.onAccent, size = 14.dp)
    }
}

/** Every helper as a tile; asleep ones are greyed out. */
@Composable
internal fun HelperGrid(world: World, selected: HelperKind, onPick: (HelperKind) -> Unit) {
    val c = U.c
    val perRow = 4
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HelperKind.entries.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kind ->
                    val awake = world.helper(kind).awake
                    val on = kind == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .pixelBox(if (on) c.surface2 else c.surface, if (on) c.accent else c.outline, c.bevel)
                            .clickable { onPick(kind) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val sprite = Art.helper(kind)
                        SpriteImage(if (awake) sprite else sprite.muted(), 40.dp)
                        Text(
                            t(kind.title), style = UType.Small.copy(color = if (on) c.accent else c.muted),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
