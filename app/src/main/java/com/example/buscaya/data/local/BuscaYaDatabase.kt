package com.example.buscaya.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.buscaya.data.local.dao.ComercioDao
import com.example.buscaya.data.local.entity.ComercioEntity

/**
 * Base de datos principal de la aplicación construida con Room.
 *
 * Hereda de [RoomDatabase] y centraliza el acceso a SQLite.
 * Utiliza el patrón Singleton para garantizar una única conexión activa en toda la app.
 */
@Database(
    entities = [ComercioEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BuscaYaDatabase : RoomDatabase() {

    abstract fun comercioDao(): ComercioDao

    companion object {
        @Volatile
        private var INSTANCIA: BuscaYaDatabase? = null

        /**
         * Retorna la instancia única de [BuscaYaDatabase].
         *
         * Usa context.applicationContext para no retener referencias directas a una Activity,
         * evitando así pérdidas de memoria (memory leaks).
         */
        fun getInstance(context: Context): BuscaYaDatabase {
            return INSTANCIA ?: synchronized(this) {
                INSTANCIA ?: Room.databaseBuilder(
                    context.applicationContext,
                    BuscaYaDatabase::class.java,
                    "buscaya.db"
                )
                    // NOTA DIDÁCTICA:
                    // fallbackToDestructiveMigration(dropAllTables = true) le indica a Room que,
                    // si se cambia la estructura de las tablas e incrementa la 'version' sin definir
                    // un objeto Migration manual, borre las tablas existentes y las vuelva a crear.
                    // Esto es EXCLUSIVAMENTE para etapa de DESARROLLO o datos que funcionan como caché.
                    // En producción con datos sensibles del usuario se debe escribir una clase Migration.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCIA = it }
            }
        }
    }
}
