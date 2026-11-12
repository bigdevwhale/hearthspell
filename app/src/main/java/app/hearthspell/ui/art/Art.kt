package app.hearthspell.ui.art

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import app.hearthspell.R
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.ImpKind

/**
 * Every character and object of the game: fine pixel art drawn with the Codex image tool and cut by
 * tools/art/cut.py into res/drawable-nodpi (4 pixels per scene cell). Generated: edit the tools, not this file.
 */
object Art {
    private lateinit var res: Resources

    /** Call once at start-up, before anything is drawn. */
    fun init(context: Context) {
        res = context.applicationContext.resources
    }

    private val cache = HashMap<Int, Sprite>()

    private fun load(id: Int): Sprite = synchronized(cache) {
        cache.getOrPut(id) {
            val b = BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inScaled = false })
            val px = IntArray(b.width * b.height)
            b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
            Sprite(b.width, b.height, px)
        }
    }

    /** The same sprite one cell lower inside a cell-taller frame: the down step of a bob. */
    private val lifted = HashMap<Sprite, Sprite>()
    private fun down(s: Sprite): Sprite = synchronized(lifted) {
        lifted.getOrPut(s) {
            val out = IntArray(s.rw * (s.rh + ART_D))
            System.arraycopy(s.argb, 0, out, s.rw * ART_D, s.argb.size)
            Sprite(s.rw, s.rh + ART_D, out)
        }
    }
    private fun up(s: Sprite): Sprite {
        val out = IntArray(s.rw * (s.rh + ART_D))
        System.arraycopy(s.argb, 0, out, 0, s.argb.size)
        return Sprite(s.rw, s.rh + ART_D, out)
    }

    private val helpers = mapOf(
        HelperKind.BROOM to arrayOf(R.drawable.hs_s_helper_broom_0, R.drawable.hs_s_helper_broom_1),
        HelperKind.SPONGE to arrayOf(R.drawable.hs_s_helper_sponge_0, R.drawable.hs_s_helper_sponge_1),
        HelperKind.CLOTH to arrayOf(R.drawable.hs_s_helper_cloth_0, R.drawable.hs_s_helper_cloth_1),
        HelperKind.BUCKET to arrayOf(R.drawable.hs_s_helper_bucket_0, R.drawable.hs_s_helper_bucket_1),
        HelperKind.DUSTER to arrayOf(R.drawable.hs_s_helper_duster_0, R.drawable.hs_s_helper_duster_1),
        HelperKind.SACK to arrayOf(R.drawable.hs_s_helper_sack_0, R.drawable.hs_s_helper_sack_1),
        HelperKind.LADLE to arrayOf(R.drawable.hs_s_helper_ladle_0, R.drawable.hs_s_helper_ladle_1),
        HelperKind.TEAPOT to arrayOf(R.drawable.hs_s_helper_teapot_0, R.drawable.hs_s_helper_teapot_1),
        HelperKind.PILLOW to arrayOf(R.drawable.hs_s_helper_pillow_0, R.drawable.hs_s_helper_pillow_1),
        HelperKind.BASKET to arrayOf(R.drawable.hs_s_helper_basket_0, R.drawable.hs_s_helper_basket_1),
        HelperKind.IRON to arrayOf(R.drawable.hs_s_helper_iron_0, R.drawable.hs_s_helper_iron_1),
        HelperKind.CAN to arrayOf(R.drawable.hs_s_helper_can_0, R.drawable.hs_s_helper_can_1),
        HelperKind.CANDLE to arrayOf(R.drawable.hs_s_helper_candle_0, R.drawable.hs_s_helper_candle_1),
        HelperKind.BOOK to arrayOf(R.drawable.hs_s_helper_book_0, R.drawable.hs_s_helper_book_1),
        HelperKind.KEY to arrayOf(R.drawable.hs_s_helper_key_0, R.drawable.hs_s_helper_key_1),
        HelperKind.BOWL to arrayOf(R.drawable.hs_s_helper_bowl_0, R.drawable.hs_s_helper_bowl_1),
        HelperKind.SATCHEL to arrayOf(R.drawable.hs_s_helper_satchel_0, R.drawable.hs_s_helper_satchel_1),
    )
    private val imps = mapOf(
        ImpKind.PIP to arrayOf(R.drawable.hs_s_imp_pip_0),
        ImpKind.HIX to arrayOf(R.drawable.hs_s_imp_hix_0),
        ImpKind.GRUMBLE to arrayOf(R.drawable.hs_s_imp_grumble_0),
        ImpKind.NIBS to arrayOf(R.drawable.hs_s_imp_nibs_0),
        ImpKind.CRUMBWHISTLE to arrayOf(R.drawable.hs_s_imp_crumbwhistle_0),
        ImpKind.INKBLOT to arrayOf(R.drawable.hs_s_imp_inkblot_0),
        ImpKind.BUBBLEGOB to arrayOf(R.drawable.hs_s_imp_bubblegob_0),
        ImpKind.SNOREWICK to arrayOf(R.drawable.hs_s_imp_snorewick_0),
        ImpKind.BOOTLACE to arrayOf(R.drawable.hs_s_imp_bootlace_0),
        ImpKind.LEAFNIBBLE to arrayOf(R.drawable.hs_s_imp_leafnibble_0),
        ImpKind.CUSHIONSQUAT to arrayOf(R.drawable.hs_s_imp_cushionsquat_0),
        ImpKind.BLOCKSTUB to arrayOf(R.drawable.hs_s_imp_blockstub_0),
        ImpKind.ODDSOCK to arrayOf(R.drawable.hs_s_imp_oddsock_0),
        ImpKind.FURBALL to arrayOf(R.drawable.hs_s_imp_furball_0),
        ImpKind.DUSTBUNNY to arrayOf(R.drawable.hs_s_imp_dustbunny_0),
        ImpKind.HALLOWIMP to arrayOf(R.drawable.hs_s_imp_hallowimp_0),
        ImpKind.FROSTNIP to arrayOf(R.drawable.hs_s_imp_frostnip_0),
        ImpKind.TINSELTOE to arrayOf(R.drawable.hs_s_imp_tinseltoe_0),
    )
    private val decors = mapOf(
        DecorKind.CANDLES to arrayOf(R.drawable.hs_s_decor_candles_0),
        DecorKind.HERBS to arrayOf(R.drawable.hs_s_decor_herbs_0),
        DecorKind.LANTERN to arrayOf(R.drawable.hs_s_decor_lantern_0),
        DecorKind.FERN to arrayOf(R.drawable.hs_s_decor_fern_0),
        DecorKind.BOOKS to arrayOf(R.drawable.hs_s_decor_books_0),
        DecorKind.MUSHROOMS to arrayOf(R.drawable.hs_s_decor_mushrooms_0),
        DecorKind.RUG to arrayOf(R.drawable.hs_s_decor_rug_0),
        DecorKind.POTIONS to arrayOf(R.drawable.hs_s_decor_potions_0),
        DecorKind.BANNER to arrayOf(R.drawable.hs_s_decor_banner_0),
        DecorKind.PAINTING to arrayOf(R.drawable.hs_s_decor_painting_0),
        DecorKind.GLOBE to arrayOf(R.drawable.hs_s_decor_globe_0),
        DecorKind.HOURGLASS to arrayOf(R.drawable.hs_s_decor_hourglass_0),
        DecorKind.CLOCK to arrayOf(R.drawable.hs_s_decor_clock_0),
        DecorKind.CHEST to arrayOf(R.drawable.hs_s_decor_chest_0),
        DecorKind.CAULDRON to arrayOf(R.drawable.hs_s_decor_cauldron_0),
        DecorKind.CRYSTAL to arrayOf(R.drawable.hs_s_decor_crystal_0),
        DecorKind.CAT to arrayOf(R.drawable.hs_s_decor_cat_0),
        DecorKind.TELESCOPE to arrayOf(R.drawable.hs_s_decor_telescope_0),
        DecorKind.OWL to arrayOf(R.drawable.hs_s_decor_owl_0),
        DecorKind.FIREPLACE to arrayOf(R.drawable.hs_s_decor_fireplace_0),
        DecorKind.CUSHION to arrayOf(R.drawable.hs_s_decor_cushion_0),
        DecorKind.PUMPKIN to arrayOf(R.drawable.hs_s_decor_pumpkin_0),
        DecorKind.FIR to arrayOf(R.drawable.hs_s_decor_fir_0),
        DecorKind.TEACUP to arrayOf(R.drawable.hs_s_decor_teacup_0),
        DecorKind.FROG to arrayOf(R.drawable.hs_s_decor_frog_0),
        DecorKind.SNOWGLOBE to arrayOf(R.drawable.hs_s_decor_snowglobe_0),
        DecorKind.DRAGON_SCALE to arrayOf(R.drawable.hs_s_decor_dragon_scale_0),
        DecorKind.IMP_JAR to arrayOf(R.drawable.hs_s_decor_imp_jar_0),
        DecorKind.BELL to arrayOf(R.drawable.hs_s_decor_bell_0),
        DecorKind.LIGHTHOUSE to arrayOf(R.drawable.hs_s_decor_lighthouse_0),
        DecorKind.SHIP to arrayOf(R.drawable.hs_s_decor_ship_0),
        DecorKind.PAPER_LANTERN to arrayOf(R.drawable.hs_s_decor_paper_lantern_0),
        DecorKind.OLD_MAP to arrayOf(R.drawable.hs_s_decor_old_map_0),
        DecorKind.AURORA to arrayOf(R.drawable.hs_s_decor_aurora_0),
        DecorKind.COMPASS to arrayOf(R.drawable.hs_s_decor_compass_0),
    )
    private val trinkets = mapOf(
        "BUTTON" to R.drawable.hs_s_trinket_button_0,
        "FEATHER" to R.drawable.hs_s_trinket_feather_0,
        "ACORN" to R.drawable.hs_s_trinket_acorn_0,
        "PEBBLE" to R.drawable.hs_s_trinket_pebble_0,
        "THIMBLE" to R.drawable.hs_s_trinket_thimble_0,
        "BEAD" to R.drawable.hs_s_trinket_bead_0,
        "MARBLE" to R.drawable.hs_s_trinket_marble_0,
        "SHELL" to R.drawable.hs_s_trinket_shell_0,
        "RIBBON" to R.drawable.hs_s_trinket_ribbon_0,
        "CORK" to R.drawable.hs_s_trinket_cork_0,
        "SPOOL" to R.drawable.hs_s_trinket_spool_0,
        "SOCK" to R.drawable.hs_s_trinket_sock_0,
        "KEY" to R.drawable.hs_s_trinket_key_0,
        "LEAF" to R.drawable.hs_s_trinket_leaf_0,
        "CLOVER" to R.drawable.hs_s_trinket_clover_0,
        "STAR" to R.drawable.hs_s_trinket_star_0,
        "GOLD_BUTTON" to R.drawable.hs_s_trinket_gold_button_0,
    )
    private val hats = mapOf(
        "POINTED" to R.drawable.hs_s_hat_pointed_0,
        "FLOWERS" to R.drawable.hs_s_hat_flowers_0,
        "WIDE" to R.drawable.hs_s_hat_wide_0,
        "CAT" to R.drawable.hs_s_hat_cat_0,
        "FROG" to R.drawable.hs_s_hat_frog_0,
        "STAR" to R.drawable.hs_s_hat_star_0,
        "CROWN" to R.drawable.hs_s_hat_crown_0,
    )
    private val hatSinks = mapOf("POINTED" to 34, "FLOWERS" to 58, "WIDE" to 33, "CAT" to 50, "FROG" to 47, "STAR" to 42, "CROWN" to 62)
    private val envelopes = mapOf<String, Int>(
        "sealed" to R.drawable.hs_s_envelope_sealed_0,
        "open" to R.drawable.hs_s_envelope_open_0,
        "waiting" to R.drawable.hs_s_envelope_waiting_0
    )
    private val aisles = mapOf<String, Int>(
        "PRODUCE" to R.drawable.hs_s_aisle_produce_0,
        "BAKERY" to R.drawable.hs_s_aisle_bakery_0,
        "DAIRY" to R.drawable.hs_s_aisle_dairy_0,
        "MEAT" to R.drawable.hs_s_aisle_meat_0,
        "PANTRY" to R.drawable.hs_s_aisle_pantry_0,
        "FROZEN" to R.drawable.hs_s_aisle_frozen_0,
        "DRINKS" to R.drawable.hs_s_aisle_drinks_0,
        "SWEETS" to R.drawable.hs_s_aisle_sweets_0,
        "HOUSEHOLD" to R.drawable.hs_s_aisle_household_0,
        "CARE" to R.drawable.hs_s_aisle_care_0,
        "PETS" to R.drawable.hs_s_aisle_pets_0,
        "OTHER" to R.drawable.hs_s_aisle_other_0
    )

    /** The picture for a market aisle, by [app.hearthspell.content.Aisle] name; null until drawn. */
    fun aisle(name: String): Sprite? = aisles[name]?.let { load(it) }

    /** The banner over the market: a stall at dusk; null until drawn. */
    val marketBanner: Sprite? get() = R.drawable.hs_market?.let { load(it) }

    /** The banner over the grimoire: an open book on a lectern; null until drawn. */
    val grimoireBanner: Sprite? get() = R.drawable.hs_grimoire?.let { load(it) }

    private val icons = mapOf<String, Int>(
        "tower" to R.drawable.hs_s_icon_tower_0,
        "book" to R.drawable.hs_s_icon_book_0,
        "basket" to R.drawable.hs_s_icon_basket_0,
        "letter" to R.drawable.hs_s_icon_letter_0,
        "gear" to R.drawable.hs_s_icon_gear_0,
        "question" to R.drawable.hs_s_icon_question_0,
        "chest" to R.drawable.hs_s_icon_chest_0,
        "pencil" to R.drawable.hs_s_icon_pencil_0,
        "flask" to R.drawable.hs_s_icon_flask_0,
        "star" to R.drawable.hs_s_icon_star_0,
        "mana" to R.drawable.hs_s_icon_mana_0,
        "imp" to R.drawable.hs_s_icon_imp_0,
        "stats" to R.drawable.hs_s_icon_stats_0
    )
    private val masters = arrayOf(R.drawable.hs_s_master_0)
    private val bodies = arrayOf(R.drawable.hs_s_body_0)
    private val catsAsleep = arrayOf(R.drawable.hs_s_cat_sleep_0)
    private val catsAwake = arrayOf(R.drawable.hs_s_cat_sit_0)
    private val bats = arrayOf(R.drawable.hs_s_bat_0, R.drawable.hs_s_bat_1)
    private val flames = arrayOf(R.drawable.hs_s_flame_0, R.drawable.hs_s_flame_1, R.drawable.hs_s_flame_2)
    private val steams = arrayOf(R.drawable.hs_s_steam_0, R.drawable.hs_s_steam_1, R.drawable.hs_s_steam_2, R.drawable.hs_s_steam_3)

    private fun frame(ids: Array<Int>, f: Int): Sprite = load(ids[Math.floorMod(f, ids.size)])

    private val helperFrames = HashMap<HelperKind, Array<Sprite>>()

    /** A helper; [frame] 0..3 cycles rest, lift, rest, eyes shut (also its sleeping face). */
    fun helper(kind: HelperKind, frame: Int = 0): Sprite = synchronized(helperFrames) {
        helperFrames.getOrPut(kind) {
            val ids = helpers.getValue(kind)
            val awake = load(ids[0])
            val shut = load(ids[1])
            arrayOf(down(awake), up(awake), down(awake), down(shut))
        }[frame and 3]
    }

    private val asleep = HashMap<HelperKind, Sprite>()

    /** A helper still asleep: greyed out, cached. */
    fun helperAsleep(kind: HelperKind): Sprite = synchronized(asleep) { asleep.getOrPut(kind) { helper(kind, 3).muted() } }

    /** An imp, each kind its own creature; [frame] 0 or 1. */
    fun imp(kind: ImpKind, frame: Int = 0): Sprite = frame(imps.getValue(kind), frame)

    fun decor(kind: DecorKind): Sprite = load(decors.getValue(kind)[0])

    /** A decor item's animation frame; still items have one frame. */
    fun decor(kind: DecorKind, frame: Int): Sprite = frame(decors.getValue(kind), frame)

    /** The old master, for his letters. */
    fun master(frame: Int = 0): Sprite = frame(masters, frame)

    /** A candle flame, three frames. */
    fun flame(frame: Int): Sprite = frame(flames, frame)

    /** A puff of rising steam, four frames. */
    fun steam(frame: Int): Sprite = frame(steams, frame)

    val moon: Sprite get() = load(R.drawable.hs_s_extra_moon_0)
    val cloud: Sprite get() = load(R.drawable.hs_s_extra_cloud_0)
    val cobweb: Sprite get() = load(R.drawable.hs_s_extra_cobweb_0)
    val teaTable: Sprite get() = load(R.drawable.hs_s_extra_tea_0)
    val heart: Sprite get() = load(R.drawable.hs_s_extra_heart_0)

    /** A trinket a helper brought, by [app.hearthspell.content.Trinket] name. */
    fun trinket(name: String): Sprite = load(trinkets.getValue(name))

    /** The tower cat: curled up asleep or sitting up. */
    fun catAsleep(frame: Int): Sprite = frame(catsAsleep, frame)
    fun catAwake(frame: Int): Sprite = frame(catsAwake, frame)

    /** The apprentice without a hat, in the red robe. */
    fun apprenticeBody(frame: Int): Sprite = frame(bodies, frame)

    /** A hat, drawn over the body with its bottom centre at [HAT_AT] lowered by [hatSink] percent of its height. */
    fun hat(name: String): Sprite = load(hats.getValue(name))
    fun hatSink(name: String): Int = hatSinks[name] ?: 30
    val HAT_AT: Pair<Int, Int> = 37 to 0

    fun bat(frame: Int): Sprite = frame(bats, frame)

    /** A floor still being built (the "add a floor" card), SCENE_W x SCENE_H / 2 cells; null until drawn. */
    val scaffold: Sprite? get() = R.drawable.hs_scaffold?.let { load(it) }

    /** The grass and soil strip under the tower, repeating sideways; null until drawn. */
    val ground: Sprite? get() = R.drawable.hs_ground?.let { load(it) }

    /** The tiling sky texture, night or day, ART_D pixels per cell; null until drawn. */
    fun sky(day: Boolean): Sprite? = (if (day) R.drawable.hs_sky_day else R.drawable.hs_sky_night)?.let { load(it) }

    /** An envelope for the master's letters: "sealed", "open" or "waiting"; null until drawn. */
    fun envelope(state: String): Sprite? = envelopes[state]?.let { load(it) }

    /** The full-colour picture for a UI icon, by name, if one was drawn. */
    fun icon(name: String): Sprite? = icons[name]?.let { load(it) }

    /** A furnishing that tells a floor's kind at a glance. */
    fun prop(kind: FloorKind): Sprite = when (kind) {
        FloorKind.KITCHEN -> decor(DecorKind.CAULDRON)
        FloorKind.LIVING -> decor(DecorKind.BOOKS)
        FloorKind.BEDROOM -> decor(DecorKind.TELESCOPE)
        FloorKind.BATH -> helper(HelperKind.BUCKET)
        FloorKind.HALL -> decor(DecorKind.LANTERN)
        FloorKind.BALCONY -> decor(DecorKind.FERN)
        FloorKind.KIDS -> decor(DecorKind.RUG)
        FloorKind.STUDY -> decor(DecorKind.CANDLES)
        FloorKind.LAUNDRY -> helper(HelperKind.BASKET)
        FloorKind.PETS -> decor(DecorKind.CAT)
        FloorKind.WHOLE -> decor(DecorKind.FIREPLACE)
    }
}
