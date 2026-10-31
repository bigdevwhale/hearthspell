package app.hearthspell.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A room of the home, drawn as a floor of the tower. An empty [name] shows the kind's tower name in the app's language. */
@Entity(tableName = "floors")
data class Floor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [app.hearthspell.content.FloorKind] name. */
    val kind: String,
    val name: String = "",
    /** 0 is the top of the tower. */
    val sort: Int = 0,
)

/**
 * A chore, as a spell. Its freshness fades from 1 to 0 over [intervalDays] after [lastCastAt].
 * A spell from the catalog keeps its [presetKey]; an empty [name] then shows the preset's name.
 */
@Entity(tableName = "spells", indices = [Index("floorId")])
data class Spell(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val floorId: Long,
    val presetKey: String? = null,
    val name: String = "",
    val intervalDays: Int,
    /** 1 small (a cantrip), 2 usual, 3 big. */
    val effort: Int = 2,
    /** [app.hearthspell.content.HelperKind] name. */
    val helper: String,
    /** Last time it was cast; at creation a made-up moment so a new tower isn't all dust at once. */
    val lastCastAt: Long,
    val createdAt: Long,
    /** Paused spells don't fade (holidays, a broken washing machine). */
    val paused: Boolean = false,
    val sort: Int = 0,
)

/** One casting of a spell: the chore was done. [mana] is what it earned, doubled for the master's requests. */
@Entity(tableName = "casts", indices = [Index("spellId"), Index("at")])
data class Cast(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spellId: Long,
    val at: Long,
    val mana: Int,
    /** It was one of the master's three requests this week. */
    val requested: Boolean = false,
    /** The spell's lastCastAt before this cast, restored on undo. */
    val prevLastCastAt: Long = 0,
)

/** Something on the shopping list, or remembered from earlier trips. */
@Entity(tableName = "reagents")
data class Reagent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [app.hearthspell.content.Aisle] name. */
    val aisle: String,
    /** On the list right now. */
    val needed: Boolean = true,
    /** Ticked in the basket during a market trip (still on the list until the trip ends). */
    val inBasket: Boolean = false,
    val note: String = "",
    val addedAt: Long,
    val lastBoughtAt: Long? = null,
    val boughtCount: Int = 0,
    /** Mean days between purchases, 0 until bought twice. Used to suggest "running low". */
    val avgGapDays: Double = 0.0,
)

/** A piece of furniture bought with mana and placed on a floor. [cost] is kept so prices can change later. */
@Entity(tableName = "decor", indices = [Index("floorId")])
data class Decor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [app.hearthspell.content.DecorKind] name. */
    val kind: String,
    /** 0 = in the chest, not placed. */
    val floorId: Long,
    val slot: Int = 0,
    val cost: Int,
    val boughtAt: Long,
)

/**
 * Something found or met: a trinket a helper left ([FIND_GIFT], its [helper] and the [day] it was found for)
 * or an imp shown the door ([FIND_IMP], with the [castId] that did it, so undo takes it back).
 */
@Entity(tableName = "finds", indices = [Index("castId")])
data class Find(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    /** [app.hearthspell.content.Trinket] or [app.hearthspell.content.ImpKind] name. */
    val kind: String,
    val helper: String = "",
    val day: Long = 0,
    val at: Long,
    val castId: Long = 0,
)

const val FIND_GIFT = "GIFT"
const val FIND_IMP = "IMP"

/** A potion brewed at [at]; it lasts [app.hearthspell.content.Potion.DAYS] days. */
@Entity(tableName = "brews")
data class Brew(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [app.hearthspell.content.Potion] name. */
    val potion: String,
    val at: Long,
)

/** A piece of the apprentice's wardrobe, bought with mana. */
@Entity(tableName = "wardrobe")
data class Wear(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [app.hearthspell.content.Garment] name. */
    val kind: String,
    val cost: Int,
    val boughtAt: Long,
)

@Dao
interface HearthDao {
    @Query("SELECT * FROM floors ORDER BY sort, id") fun observeFloors(): Flow<List<Floor>>
    @Query("SELECT * FROM spells ORDER BY sort, id") fun observeSpells(): Flow<List<Spell>>
    @Query("SELECT * FROM casts ORDER BY at DESC") fun observeCasts(): Flow<List<Cast>>
    @Query("SELECT * FROM reagents ORDER BY name COLLATE NOCASE") fun observeReagents(): Flow<List<Reagent>>
    @Query("SELECT * FROM decor ORDER BY boughtAt") fun observeDecor(): Flow<List<Decor>>
    @Query("SELECT * FROM finds ORDER BY at DESC") fun observeFinds(): Flow<List<Find>>
    @Query("SELECT * FROM brews ORDER BY at DESC") fun observeBrews(): Flow<List<Brew>>
    @Query("SELECT * FROM wardrobe ORDER BY boughtAt") fun observeWardrobe(): Flow<List<Wear>>

    @Query("SELECT * FROM floors ORDER BY sort, id") suspend fun floors(): List<Floor>
    @Query("SELECT * FROM spells ORDER BY sort, id") suspend fun spells(): List<Spell>
    @Query("SELECT * FROM casts ORDER BY at DESC") suspend fun casts(): List<Cast>
    @Query("SELECT * FROM reagents ORDER BY name COLLATE NOCASE") suspend fun reagents(): List<Reagent>
    @Query("SELECT * FROM decor ORDER BY boughtAt") suspend fun decor(): List<Decor>
    @Query("SELECT * FROM finds ORDER BY at DESC") suspend fun finds(): List<Find>
    @Query("SELECT * FROM brews ORDER BY at DESC") suspend fun brews(): List<Brew>
    @Query("SELECT * FROM wardrobe ORDER BY boughtAt") suspend fun wardrobe(): List<Wear>

    @Upsert suspend fun upsertFloor(f: Floor): Long
    @Query("DELETE FROM floors WHERE id = :id") suspend fun deleteFloorRow(id: Long)

    @Upsert suspend fun upsertSpell(s: Spell): Long
    @Query("SELECT * FROM spells WHERE id = :id") suspend fun spell(id: Long): Spell?
    @Query("SELECT * FROM spells WHERE presetKey = :key LIMIT 1") suspend fun spellByPreset(key: String): Spell?
    @Query("DELETE FROM spells WHERE id = :id") suspend fun deleteSpellRow(id: Long)
    @Query("SELECT id FROM spells WHERE floorId = :floorId") suspend fun spellIdsOf(floorId: Long): List<Long>

    @Insert suspend fun insertCast(c: Cast): Long
    @Query("SELECT * FROM casts WHERE id = :id") suspend fun cast(id: Long): Cast?
    @Query("DELETE FROM casts WHERE id = :id") suspend fun deleteCast(id: Long)
    @Query("DELETE FROM casts WHERE spellId = :spellId") suspend fun deleteCastsOf(spellId: Long)
    @Query("SELECT MAX(at) FROM casts WHERE spellId = :spellId") suspend fun lastCastOf(spellId: Long): Long?
    @Query("UPDATE casts SET prevLastCastAt = :prev WHERE spellId = :spellId AND prevLastCastAt = :at AND at > :at")
    suspend fun relinkCasts(spellId: Long, at: Long, prev: Long)

    @Upsert suspend fun upsertReagent(r: Reagent): Long
    @Query("DELETE FROM reagents WHERE id = :id") suspend fun deleteReagent(id: Long)

    @Upsert suspend fun upsertDecor(d: Decor): Long
    @Query("DELETE FROM decor WHERE id = :id") suspend fun deleteDecor(id: Long)
    @Query("UPDATE decor SET floorId = 0 WHERE floorId = :floorId") suspend fun unplaceDecorOf(floorId: Long)

    @Insert suspend fun insertFind(f: Find): Long
    @Query("DELETE FROM finds WHERE castId = :castId AND castId != 0") suspend fun deleteFindsOfCast(castId: Long)
    @Insert suspend fun insertBrew(b: Brew): Long
    @Insert suspend fun insertWear(w: Wear): Long

    @Query("DELETE FROM floors") suspend fun wipeFloors()
    @Query("DELETE FROM spells") suspend fun wipeSpells()
    @Query("DELETE FROM casts") suspend fun wipeCasts()
    @Query("DELETE FROM reagents") suspend fun wipeReagents()
    @Query("DELETE FROM decor") suspend fun wipeDecor()
    @Query("DELETE FROM finds") suspend fun wipeFinds()
    @Query("DELETE FROM brews") suspend fun wipeBrews()
    @Query("DELETE FROM wardrobe") suspend fun wipeWardrobe()
}

@Database(
    entities = [Floor::class, Spell::class, Cast::class, Reagent::class, Decor::class, Find::class, Brew::class, Wear::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class HearthDatabase : RoomDatabase() {
    abstract fun dao(): HearthDao

    companion object {
        fun create(context: Context): HearthDatabase =
            Room.databaseBuilder(context.applicationContext, HearthDatabase::class.java, "hearthspell.db").build()
    }
}
