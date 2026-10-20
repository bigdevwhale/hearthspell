package app.hearthspell.ui.tower

import app.hearthspell.R
import app.hearthspell.content.FloorKind

/** Where things are in a floor scene bitmap, in its pixels (generated with the art). */
internal class SceneMeta(
    val res: Int,
    /** The window glass: x, y, w, h, and whether it is round. The live sky is drawn there, under the bitmap. */
    val glass: IntArray,
    val round: Boolean,
    /** Wick tops of the candles and fires that flicker, with a size. */
    val flames: List<Triple<Int, Int, Float>>,
    /** Where steam rises from. */
    val steam: List<Pair<Int, Int>>,
    /** Free spots on the wall for hanging decor (top-left corners). */
    val wall: List<Pair<Int, Int>>,
    /** Free spots along the floor for standing decor (left x). */
    val floor: List<Int>,
    /** How far helpers walk. */
    val walk: IntRange,
)

internal const val SCENE_W = 120
internal const val SCENE_H = 72
internal const val SCENE_FLOOR = 62
internal const val ROOF_W = 128
internal const val ROOF_H = 70
internal const val BASE_W = 120
internal const val BASE_H = 34
internal const val BASE_GROUND = 28
internal val ROOF_CHIMNEY = 93 to 18
internal val ROOF_LANTERN = 64 to 2
internal val BASE_TORCHES = listOf(35 to 10, 85 to 10)
internal const val WALL_H = 26
/** The window of the outside wall (hs_wall): x, y, w, h. */
internal val WALL_GLASS = intArrayOf(53, 6, 14, 13)

internal val SCENES: Map<FloorKind, SceneMeta> = mapOf(
    FloorKind.KITCHEN to SceneMeta(
        R.drawable.hs_scene_kitchen,
        intArrayOf(85, 13, 14, 22), false,
        listOf(Triple(20, 61, 1.3f), Triple(27, 61, 1.5f), Triple(34, 61, 1.3f)),
        listOf(27 to 32),
        listOf(86 to 42),
        listOf(86, 98),
        10..109,
    ),
    FloorKind.LIVING to SceneMeta(
        R.drawable.hs_scene_living,
        intArrayOf(51, 13, 14, 22), false,
        listOf(),
        listOf(),
        listOf(72 to 12, 42 to 12),
        listOf(44, 86),
        10..109,
    ),
    FloorKind.BEDROOM to SceneMeta(
        R.drawable.hs_scene_bedroom,
        intArrayOf(41, 9, 24, 24), true,
        listOf(Triple(51, 41, 0.7f)),
        listOf(),
        listOf(14 to 12, 88 to 10),
        listOf(30, 111),
        10..109,
    ),
    FloorKind.BATH to SceneMeta(
        R.drawable.hs_scene_bath,
        intArrayOf(47, 11, 12, 12), true,
        listOf(),
        listOf(70 to 42, 84 to 41, 94 to 43),
        listOf(10 to 10, 100 to 10),
        listOf(8, 102),
        10..109,
    ),
    FloorKind.HALL to SceneMeta(
        R.drawable.hs_scene_hall,
        intArrayOf(77, 13, 12, 20), false,
        listOf(Triple(104, 28, 1.0f)),
        listOf(),
        listOf(38 to 12, 98 to 40),
        listOf(90, 100),
        10..109,
    ),
    FloorKind.BALCONY to SceneMeta(
        R.drawable.hs_scene_balcony,
        intArrayOf(12, 8, 96, 34), false,
        listOf(),
        listOf(),
        listOf(),
        listOf(80),
        10..109,
    ),
    FloorKind.KIDS to SceneMeta(
        R.drawable.hs_scene_kids,
        intArrayOf(83, 13, 14, 20), false,
        listOf(),
        listOf(),
        listOf(62 to 12),
        listOf(80, 96),
        10..109,
    ),
    FloorKind.STUDY to SceneMeta(
        R.drawable.hs_scene_study,
        intArrayOf(51, 11, 12, 20), false,
        listOf(Triple(90, 38, 0.7f)),
        listOf(),
        listOf(38 to 12, 96 to 10),
        listOf(38, 100),
        10..109,
    ),
    FloorKind.LAUNDRY to SceneMeta(
        R.drawable.hs_scene_laundry,
        intArrayOf(93, 13, 10, 18), false,
        listOf(),
        listOf(56 to 42),
        listOf(),
        listOf(74, 98),
        10..109,
    ),
    FloorKind.PETS to SceneMeta(
        R.drawable.hs_scene_pets,
        intArrayOf(51, 13, 12, 20), false,
        listOf(),
        listOf(),
        listOf(66 to 10),
        listOf(68, 82),
        10..109,
    ),
    FloorKind.WHOLE to SceneMeta(
        R.drawable.hs_scene_whole,
        intArrayOf(91, 13, 12, 20), false,
        listOf(Triple(54, 56, 1.6f), Triple(60, 55, 1.9f), Triple(66, 56, 1.6f)),
        listOf(),
        listOf(30 to 10, 80 to 10),
        listOf(82, 100),
        10..109,
    ),
)
