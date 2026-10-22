package com.example.buscaya.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.buscaya.data.local.entity.ComercioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la tabla 'comercios'.
 *
 * Expone las operaciones CRUD sobre la base de datos SQLite:
 * - Consultas observables: devuelven Flow y NO son suspend (emiten en tiempo real ante cambios).
 * - Operaciones de escritura: son suspend para garantizar que se ejecuten fuera del hilo principal.
 */
@Dao
interface ComercioDao {

    /**
     * Obtiene todos los comercios ordenados alfabéticamente por nombre.
     * Devuelve un Flow reactivo: cada vez que la tabla 'comercios' sufre un cambio,
     * Room reemite la lista actualizada automáticamente.
     */
    @Query("SELECT * FROM comercios ORDER BY nombre ASC")
    fun obtenerTodos(): Flow<List<ComercioEntity>>

    /**
     * Inserta una lista completa de comercios.
     * Con OnConflictStrategy.REPLACE, si ya existe un registro con la misma clave primaria (@PrimaryKey id),
     * Room sobreescribe la fila con la información actualizada.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(comercios: List<ComercioEntity>)

    /**
     * Actualiza un comercio existente en base a su @PrimaryKey.
     * Retorna el número de filas modificadas.
     */
    @Update
    suspend fun actualizar(comercio: ComercioEntity): Int

    /**
     * Elimina un comercio específico de la base de datos local.
     * Retorna el número de filas eliminadas.
     */
    @Delete
    suspend fun eliminar(comercio: ComercioEntity): Int

    /**
     * Búsqueda por nombre mediante consulta SQL parametrizada con LIKE.
     * El parámetro :texto se vincula de forma segura a SQLite (previene ataques de Inyección SQL;
     * nunca se concatena texto crudo como '%$texto%').
     */
    @Query("SELECT * FROM comercios WHERE nombre LIKE '%' || :texto || '%' ORDER BY nombre ASC")
    fun buscarPorNombre(texto: String): Flow<List<ComercioEntity>>

    /**
     * Cuenta la cantidad total de comercios guardados en la base local.
     * Útil para la estrategia Cache-First (saber si Room ya tiene datos o está vacío).
     */
    @Query("SELECT COUNT(*) FROM comercios")
    suspend fun contar(): Int

    /**
     * Elimina todos los registros de la tabla 'comercios'.
     * Se invoca dentro de transacciones atómicas antes de insertar el catálogo fresco.
     */
    @Query("DELETE FROM comercios")
    suspend fun borrarTodos()
}
