package com.example.buscaya.data.models

/**
 * Modelo de Dominio limpio para representar un comercio o servicio en la interfaz.
 * No posee dependencias ni anotaciones de Room ni de Retrofit/Gson.
 */
data class BusinessResult(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: String?,
    val imagen: String?,
    val negocioNombre: String?,
    val emailContacto: String?,
    val telefono: String?,
    val fuente: String?,
    val ubicacion: String?,
    val rating: Double?,
    val placeId: String?
)

/**
 * Estados de UI modelados mediante una interfaz sellada (sealed interface).
 *
 * Permite representar de forma exhaustiva y segura todos los estados posibles de la pantalla:
 * - [Cargando]: Primera carga cuando la base está vacía y se está contactando al servidor.
 * - [Exito]: Hay comercios disponibles para mostrar.
 * - [SinConexion]: La petición de red falló (modo avión o sin internet), pero hay datos guardados
 *   en Room para mostrar junto con un mensaje de advertencia.
 * - [Error]: Falló la red y la base de datos local está completamente vacía.
 */
sealed interface SearchUiState {
    data object Cargando : SearchUiState

    data class Exito(
        val results: List<BusinessResult>
    ) : SearchUiState

    data class SinConexion(
        val results: List<BusinessResult>,
        val mensaje: String = "Sin conexión, mostrando datos guardados"
    ) : SearchUiState

    data class Error(
        val mensaje: String
    ) : SearchUiState
}
