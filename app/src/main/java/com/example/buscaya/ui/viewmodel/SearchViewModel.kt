package com.example.buscaya.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.buscaya.data.ResultadoRefresco
import com.example.buscaya.data.local.BuscaYaDatabase
import com.example.buscaya.data.models.BusinessResult
import com.example.buscaya.data.models.SearchUiState
import com.example.buscaya.data.remote.RetrofitClient
import com.example.buscaya.data.repository.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona el estado de UI para la búsqueda de comercios y farmacias.
 *
 * Combina el flujo de datos reactivo de Room con los estados de la red para emitir
 * un único StateFlow<SearchUiState>.
 */
class SearchViewModel(
    private val repository: SearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _refrescando = MutableStateFlow(false)
    val refrescando: StateFlow<Boolean> = _refrescando

    private val _ultimoResultado = MutableStateFlow<ResultadoRefresco?>(null)

    // Observa Room en función de la búsqueda ingresada por el usuario (flatMapLatest)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val listaLocal = _query.flatMapLatest { texto ->
        if (texto.isBlank()) {
            repository.comercios
        } else {
            repository.buscarLocal(texto)
        }
    }

    /**
     * Estado de pantalla observable expuesto a Jetpack Compose mediante StateFlow.
     * Sobrevive a rotaciones de pantalla y se detiene si no hay observadores activos.
     */
    val uiState: StateFlow<SearchUiState> = combine(
        listaLocal,
        _refrescando,
        _ultimoResultado
    ) { lista, cargando, resultado ->
        armarEstado(lista, cargando, resultado)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState.Cargando
    )

    private var searchJob: Job? = null

    init {
        // Al iniciar la app: si Room está vacío busca en la API; si ya tiene datos, los muestra al instante.
        refrescar(forzar = false)
    }

    /**
     * Dispara la actualización contra la API remota.
     *
     * @param forzar Si es true, va a la API aunque ya existan datos locales (botón "Actualizar" o swipe).
     */
    fun refrescar(forzar: Boolean = true) {
        if (_refrescando.value) return // Previene llamadas duplicadas concurrentes
        viewModelScope.launch {
            _refrescando.value = true
            _ultimoResultado.value = repository.refrescar(
                query = _query.value.ifBlank { "comercio" },
                forzar = forzar
            )
            _refrescando.value = false
        }
    }

    /**
     * Búsqueda híbrida / Offline-First:
     * 1. Inmediato (0ms): actualiza _query para que Room filtre SQLite con LIKE en tiempo real (funciona offline).
     * 2. Auto-búsqueda remota (debounce 400ms): si hay conexión y el usuario terminó de tipear,
     *    consulta a la API remota, guarda los resultados nuevos en Room y Room los emite en vivo a la pantalla.
     */
    fun search(nuevaQuery: String) {
        _query.value = nuevaQuery

        searchJob?.cancel()
        if (nuevaQuery.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(400) // Debounce para no saturar la red mientras escribe
                _refrescando.value = true
                _ultimoResultado.value = repository.refrescar(query = nuevaQuery, forzar = true)
                _refrescando.value = false
            }
        }
    }

    /**
     * Mapea los valores emitidos por Room y la red a los estados sellados de UI.
     */
    private fun armarEstado(
        lista: List<BusinessResult>,
        cargando: Boolean,
        resultado: ResultadoRefresco?
    ): SearchUiState {
        return when {
            // Caso 1: Hay datos guardados en Room y ocurrió un error de red (modo avión / sin conexión)
            lista.isNotEmpty() && resultado is ResultadoRefresco.SinConexion && !cargando -> {
                SearchUiState.SinConexion(lista, "Sin conexión, mostrando datos guardados")
            }

            // Caso 2: Hay datos disponibles en Room (modo normal)
            lista.isNotEmpty() -> {
                SearchUiState.Exito(lista)
            }

            // Caso 3: Room está vacío y se está esperando la respuesta inicial de la red
            cargando || (resultado == null && lista.isEmpty()) -> {
                SearchUiState.Cargando
            }

            // Caso 4: Falló la red y Room está completamente vacío
            resultado is ResultadoRefresco.SinConexion -> {
                SearchUiState.Error("Sin conexión a internet y no hay datos guardados localmente")
            }
            resultado is ResultadoRefresco.ErrorServidor -> {
                SearchUiState.Error("Error del servidor (código ${resultado.codigo})")
            }
            resultado is ResultadoRefresco.ErrorDesconocido -> {
                SearchUiState.Error(resultado.mensaje ?: "Error al actualizar catálogo")
            }

            // Caso 5: No se encontraron resultados para la búsqueda
            else -> {
                SearchUiState.Exito(emptyList())
            }
        }
    }

    companion object {
        /**
         * Factory manual para instanciar el ViewModel sin requerir Hilt ni frameworks externos.
         */
        fun factory(context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val db = BuscaYaDatabase.getInstance(context)
                    val repository = SearchRepository(db, RetrofitClient.api)
                    SearchViewModel(repository)
                }
            }
    }
}
