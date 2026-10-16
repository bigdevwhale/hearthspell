package app.hearthspell.data

import android.content.Context
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

@Dao
interface HearthDao {
    @Query("SELECT * FROM floors ORDER BY sort, id") fun observeFloors(): Flow<List<Floor>>
    @Query("SELECT * FROM spells ORDER BY sort, id") fun observeSpells(): Flow<List<Spell>>
    @Query("SELECT * FROM casts ORDER BY at DESC") fun observeCasts(): Flow<List<Cast>>
    @Query("SELECT * FROM reagents ORDER BY name COLLATE NOCASE") fun observeReagents(): Flow<List<Reagent>>
    @Query("SELECT * FROM decor ORDER BY boughtAt") fun observeDecor(): Flow<List<Decor>>

    @Query("SELECT * FROM floors ORDER BY sort, id") suspend fun floors(): List<Floor>
    @Query("SELECT * FROM spells ORDER BY sort, id") suspend fun spells(): List<Spell>
    @Query("SELECT * FROM casts ORDER BY at DESC") suspend fun casts(): List<Cast>
    @Query("SELECT * FROM reagents ORDER BY name COLLATE NOCASE") suspend fun reagents(): List<Reagent>
    @Query("SELECT * FROM decor ORDER BY boughtAt") suspend fun decor(): List<Decor>

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

    @Upsert suspend fun upsertReagent(r: Reagent): Long
    @Query("DELETE FROM reagents WHERE id = :id") suspend fun deleteReagent(id: Long)

    @Upsert suspend fun upsertDecor(d: Decor): Long
    @Query("DELETE FROM decor WHERE id = :id") suspend fun deleteDecor(id: Long)
    @Query("UPDATE decor SET floorId = 0 WHERE floorId = :floorId") suspend fun unplaceDecorOf(floorId: Long)

    @Query("DELETE FROM floors") suspend fun wipeFloors()
    @Query("DELETE FROM spells") suspend fun wipeSpells()
    @Query("DELETE FROM casts") suspend fun wipeCasts()
    @Query("DELETE FROM reagents") suspend fun wipeReagents()
    @Query("DELETE FROM decor") suspend fun wipeDecor()
}

@Database(
    entities = [Floor::class, Spell::class, Cast::class, Reagent::class, Decor::class],
    version = 1,
    exportSchema = true,
)
abstract class HearthDatabase : RoomDatabase() {
    abstract fun dao(): HearthDao

    companion object {
        fun create(context: Context): HearthDatabase =
            Room.databaseBuilder(context.applicationContext, HearthDatabase::class.java, "hearthspell.db").build()
    }
}
