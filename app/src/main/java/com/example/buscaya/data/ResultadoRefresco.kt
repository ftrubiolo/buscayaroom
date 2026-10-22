package com.example.buscaya.data

/**
 * Representa el resultado de un intento de sincronizar datos con la API remota.
 */
sealed interface ResultadoRefresco {
    data class Exito(val cantidad: Int) : ResultadoRefresco
    data object SinCambios : ResultadoRefresco
    data object SinConexion : ResultadoRefresco
    data class ErrorServidor(val codigo: Int) : ResultadoRefresco
    data class ErrorDesconocido(val mensaje: String?) : ResultadoRefresco
}
