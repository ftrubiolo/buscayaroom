package com.example.buscaya.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.buscaya.data.models.BusinessResult
import com.example.buscaya.data.remote.dto.BusinessResultDto

/**
 * Entidad de Room que representa la tabla 'comercios' en SQLite.
 *
 * Cada propiedad representa una columna y cada instancia de la clase es una fila.
 * Se mantiene separada de los modelos de red (DTO) y de los modelos de UI (Dominio)
 * siguiendo el Principio de Responsabilidad Única.
 */
@Entity(tableName = "comercios")
data class ComercioEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: String?,
    val imagen: String?,
    @ColumnInfo(name = "negocio_nombre")
    val negocioNombre: String?,
    @ColumnInfo(name = "email_contacto")
    val emailContacto: String?,
    val telefono: String?,
    val fuente: String?,
    val ubicacion: String?,
    val rating: Double?,
    @ColumnInfo(name = "place_id")
    val placeId: String?,
    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: Long
)

/**
 * Mapper: Convierte el DTO recibido de Retrofit en una entidad persistible en Room.
 * Genera un id seguro si el servidor no proveyó uno explícito.
 */
fun BusinessResultDto.toEntity(ahora: Long = System.currentTimeMillis()): ComercioEntity {
    val cleanId = when {
        !id.isNullOrBlank() -> id
        !placeId.isNullOrBlank() -> placeId
        else -> "${nombre.orEmpty()}_${ubicacion.orEmpty()}"
    }
    return ComercioEntity(
        id = cleanId,
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
        placeId = placeId,
        fechaActualizacion = ahora
    )
}

/**
 * Mapper: Convierte una fila local de Room al modelo de dominio limpio que consume la interfaz.
 */
fun ComercioEntity.toDomain(): BusinessResult =
    BusinessResult(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
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
