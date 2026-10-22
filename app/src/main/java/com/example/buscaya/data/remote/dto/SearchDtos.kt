package com.example.buscaya.data.remote.dto

import com.example.buscaya.data.models.BusinessResult
import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta de búsqueda híbrida de BuscaYA.
 */
data class HybridSearchResponseDto(
    @SerializedName("results")
    val results: List<BusinessResultDto>? = null,
    @SerializedName("total")
    val total: Int? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("message")
    val message: String? = null
)

/**
 * DTO de cada comercio/negocio recibido de la API.
 * Propiedades nullables por seguridad contra datos incompletos.
 */
data class BusinessResultDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("nombre")
    val nombre: String?,
    @SerializedName("descripcion")
    val descripcion: String?,
    @SerializedName("precio")
    val precio: String?,
    @SerializedName("imagen")
    val imagen: String?,
    @SerializedName("negocio_nombre")
    val negocioNombre: String?,
    @SerializedName("email_contacto")
    val emailContacto: String?,
    @SerializedName("telefono")
    val telefono: String?,
    @SerializedName("fuente")
    val fuente: String?,
    @SerializedName("ubicacion")
    val ubicacion: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("place_id")
    val placeId: String?
)

/**
 * Convierte el DTO de la API en el modelo de dominio utilizado por la UI.
 */
fun BusinessResultDto.toDomain(): BusinessResult =
    BusinessResult(
        id = id ?: placeId ?: "",
        nombre = nombre?.trim().orEmpty(),
        descripcion = descripcion?.trim().orEmpty(),
        precio = precio,
        imagen = imagen,
        negocioNombre = negocioNombre,
        emailContacto = emailContacto,
        telefono = telefono,
        fuente = fuente,
        ubicacion = ubicacion,
        rating = rating,
        placeId = placeId
    )
