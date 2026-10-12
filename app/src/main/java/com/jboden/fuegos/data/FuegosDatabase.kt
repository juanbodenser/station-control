package com.jboden.fuegos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Unidad::class,
        Seccion::class,
        Producto::class,
        Recuento::class,
        LineaRecuento::class,
        Entrada::class,
        TareaProduccion::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FuegosDatabase : RoomDatabase() {

    abstract fun unidadDao(): UnidadDao
    abstract fun seccionDao(): SeccionDao
    abstract fun productoDao(): ProductoDao
    abstract fun recuentoDao(): RecuentoDao
    abstract fun entradaDao(): EntradaDao
    abstract fun tareaProduccionDao(): TareaProduccionDao

    companion object {
        @Volatile
        private var INSTANCE: FuegosDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE secciones ADD COLUMN origen TEXT NOT NULL DEFAULT 'PROVEEDOR'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tareas_produccion` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`texto` TEXT NOT NULL, " +
                    "`orden` INTEGER NOT NULL, " +
                    "`marcada` INTEGER NOT NULL)"
                )
                insertInitialTasks(database)
            }
        }

        fun getDatabase(context: Context): FuegosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuegosDatabase::class.java,
                    "fuegos_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(FuegosDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class FuegosDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            db.execSQL(
                "INSERT INTO unidades (id, nombre, abreviatura) VALUES " +
                "(1, 'kilo', 'kg'), " +
                "(2, 'unidad', 'ud'), " +
                "(3, 'litro', 'l'), " +
                "(4, 'tupper 3 l', 'tup'), " +
                "(5, 'bandeja 20 ud', 'bdj');"
            )

            db.execSQL(
                "INSERT INTO secciones (id, nombre, orden, origen) VALUES " +
                "(1, 'Proveedores', 1, 'PROVEEDOR'), " +
                "(2, 'Taller', 2, 'TALLER'), " +
                "(3, 'Elaboraciones', 3, 'COCINA'), " +
                "(4, 'Verduras', 4, 'PROVEEDOR');"
            )

            db.execSQL(
                "INSERT INTO productos (id, seccionId, unidadId, nombre, orden, activo) VALUES " +
                // Proveedores (seccionId = 1)
                "(1, 1, 1, 'Almejas', 1, 1), " +
                "(2, 1, 1, 'Pulpo', 2, 1), " +
                "(3, 1, 1, 'Langostinos tigre', 3, 1), " +
                "(4, 1, 1, 'Cigala', 4, 1), " +
                "(5, 1, 2, 'Codorniz', 5, 1), " +
                "(6, 1, 1, 'Chipirones', 6, 1), " +
                // Taller (seccionId = 2)
                "(7, 2, 5, 'Flores de alcachofa', 1, 1), " +
                "(8, 2, 1, 'Jamón', 2, 1), " +
                // Elaboraciones (seccionId = 3)
                "(9, 3, 4, 'Bivalvos', 1, 1), " +
                "(10, 3, 4, 'Meloso', 2, 1), " +
                "(11, 3, 4, 'Menestra', 3, 1), " +
                "(12, 3, 4, 'Boletus', 4, 1), " +
                "(13, 3, 4, 'Salsa de cigala', 5, 1), " +
                // Verduras (seccionId = 4)
                "(14, 4, 2, 'Hinojo', 1, 1), " +
                "(15, 4, 1, 'Guindilla roja fresca', 2, 1), " +
                "(16, 4, 2, 'Cebolleta china', 3, 1), " +
                "(17, 4, 1, 'Cebolla blanca', 4, 1), " +
                "(18, 4, 1, 'Cebolla roja', 5, 1);"
            )

            insertInitialTasks(db)
        }
    }
}

private fun insertInitialTasks(db: SupportSQLiteDatabase) {
    val tareas = listOf(
        "Salsa verde",
        "Huevo poché X2",
        "Patatas cocidas 3K y yuca 600g",
        "Verduras para codorniz",
        "Verduras para sofrito X2",
        "Cebolla blanca en juliana 10K",
        "Arroz CR7 1K",
        "Cortar jamón",
        "Limpiar langostinos tigre 2",
        "Limpiar cigalas 2"
    )
    tareas.forEachIndexed { index, texto ->
        db.execSQL("INSERT INTO tareas_produccion (texto, orden, marcada) VALUES ('$texto', ${index + 1}, 0)")
    }
}
