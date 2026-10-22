package com.example.buscaya.data.repository

import androidx.room.withTransaction
import com.example.buscaya.data.ResultadoRefresco
import com.example.buscaya.data.local.BuscaYaDatabase
import com.example.buscaya.data.local.entity.toDomain
import com.example.buscaya.data.local.entity.toEntity
import com.example.buscaya.data.models.BusinessResult
import com.example.buscaya.data.remote.BuscaYaApi
import com.example.buscaya.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Repositorio de Comercios aplicando el patrón Cache-First y Single Source of Truth (SSOT).
 *
 * La pantalla NUNCA lee directamente de la API: siempre observa el Flow que proviene de Room.
 * Cuando se refrescan los datos de la red, se actualiza Room mediante una transacción atómica
 * (db.withTransaction) y el Flow se encarga de emitir la lista fresca a la UI.
 */
class SearchRepository(
    private val db: BuscaYaDatabase,
    private val api: BuscaYaApi = RetrofitClient.api
) {
    private val dao = db.comercioDao()

    /**
     * Catálogo completo observado directamente desde la base local (Única Fuente de Verdad).
     */
    val comercios: Flow<List<BusinessResult>> =
        dao.obtenerTodos().map { lista -> lista.map { it.toDomain() } }

    /**
     * Búsqueda por nombre ejecutada sobre la base SQLite local con LIKE parametrizado.
     * Funciona 100% offline.
     */
    fun buscarLocal(query: String): Flow<List<BusinessResult>> =
        dao.buscarPorNombre(query.trim()).map { lista -> lista.map { it.toDomain() } }

    /**
     * Trae datos frescos de la API y los guarda en Room.
     *
     * @param query Término de búsqueda a consultar en la API remota.
     * @param forzar false = solo va a la API si Room está vacío (inicio rápido).
     *               true = consulta a la API aunque haya datos (botón Actualizar o búsqueda activa).
     */
    suspend fun refrescar(query: String = "comercio", forzar: Boolean = false): ResultadoRefresco = withContext(Dispatchers.IO) {
        val hayDatosLocales = dao.contar() > 0
        if (hayDatosLocales && !forzar) {
            return@withContext ResultadoRefresco.SinCambios
        }

        try {
            val response = api.searchBusinesses(query.ifBlank { "comercio" })
            val ahora = System.currentTimeMillis()
            val entidades = response.results?.map { it.toEntity(ahora) } ?: emptyList()

            if (entidades.isNotEmpty()) {
                db.withTransaction {
                    // Si se fuerza la recarga con la barra vacía (botón Actualizar general),
                    // refrescamos el catálogo base. Si es una búsqueda específica, insertamos/actualizamos
                    // sin borrar los comercios previos para enriquecer la base local de Room.
                    if (query.isBlank() && forzar) {
                        dao.borrarTodos()
                    }
                    dao.insertarTodos(entidades)
                }
            }
            ResultadoRefresco.Exito(entidades.size)
        } catch (e: IOException) {
            // Sin conexión, timeout, modo avión: Room NO se toca y se muestran los datos locales
            ResultadoRefresco.SinConexion
        } catch (e: HttpException) {
            ResultadoRefresco.ErrorServidor(e.code())
        } catch (e: CancellationException) {
            throw e // Nunca atrapar la cancelación de una corrutina
        } catch (e: Exception) {
            ResultadoRefresco.ErrorDesconocido(e.message)
        }
    }
}
