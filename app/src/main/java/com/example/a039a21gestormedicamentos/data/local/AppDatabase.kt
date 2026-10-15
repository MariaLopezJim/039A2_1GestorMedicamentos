package com.example.a039a21gestormedicamentos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MedicamentoEntity::class, TomaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicamentoDao(): MedicamentoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        /** Versión 1 → 2: fecha de registro. */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE medicamentos ADD COLUMN fechaRegistroMillis INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE medicamentos SET fechaRegistroMillis = ${System.currentTimeMillis()}"
                )
            }
        }

        /** Versión 2 → 3: inventario (opcional, por eso puede ser NULL). */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicamentos ADD COLUMN inventario REAL")
            }
        }

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor_medicamentos.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .build()
                    .also { instancia = it }
            }
    }
}