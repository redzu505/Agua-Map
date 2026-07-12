package com.aguamap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aguamap.app.data.repository.AppRepository
import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.CommunityNews
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
import com.aguamap.app.util.DateUtils
import com.aguamap.app.util.ModeracionPalabras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * CAPA DE VIEWMODEL
 * Gestiona el estado de la UI y se comunica con la capa de dominio.
 */
class HomeViewModel(private val repository: AppRepository) : ViewModel() {

    private val _waterPoints = MutableStateFlow<List<WaterPoint>>(emptyList())
    val waterPoints: StateFlow<List<WaterPoint>> = _waterPoints

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _reports = MutableStateFlow<List<WaterPointReport>>(emptyList())
    val reports: StateFlow<List<WaterPointReport>> = _reports

    // Valoración (1-5) que el usuario dio al punto que está viendo. null = aún no vota.
    private val _miValoracion = MutableStateFlow<Int?>(null)
    val miValoracion: StateFlow<Int?> = _miValoracion

    private val _news = MutableStateFlow<List<CommunityNews>>(emptyList())
    val news: StateFlow<List<CommunityNews>> = _news

    // Reportes recientes de toda la comunidad (pantalla Comunidad)
    private val _recentReports = MutableStateFlow<List<WaterPointReport>>(emptyList())
    val recentReports: StateFlow<List<WaterPointReport>> = _recentReports

    // IDs de los puntos guardados (favoritos) del usuario
    private val _favoritos = MutableStateFlow<Set<String>>(emptySet())
    val favoritos: StateFlow<Set<String>> = _favoritos

    // Estadísticas del perfil: (reportes enviados, comentarios hechos)
    private val _estadisticas = MutableStateFlow(0 to 0)
    val estadisticas: StateFlow<Pair<Int, Int>> = _estadisticas

    /** Carga los contadores reales de actividad del usuario para la pantalla de perfil. */
    fun cargarEstadisticasUsuario() {
        viewModelScope.launch {
            _estadisticas.value = repository.getEstadisticasUsuario()
        }
    }

    fun loadFavoritos() {
        viewModelScope.launch {
            _favoritos.value = repository.getFavoritos()
        }
    }

    /**
     * Marca/desmarca un punto como guardado y actualiza el estado al instante.
     */
    fun toggleFavorito(pointId: String) {
        viewModelScope.launch {
            val ahoraEsFavorito = repository.toggleFavorito(pointId)
            _favoritos.value = if (ahoraEsFavorito) {
                _favoritos.value + pointId
            } else {
                _favoritos.value - pointId
            }
        }
    }

    // Nombre del usuario logueado, usado como autor de los comentarios.
    // Si es invitado o no hay nombre, usamos un valor genérico.
    private var currentUserName: String = "Vecino SJL"

    // Estado para la ruta activa en el mapa
    private val _routeDestination = MutableStateFlow<WaterPoint?>(null)
    val routeDestination: StateFlow<WaterPoint?> = _routeDestination

    fun setRouteDestination(point: WaterPoint?) {
        _routeDestination.value = point
    }

    /**
     * Lo llama la navegación cuando entra al Home para saber quién comenta.
     */
    fun setCurrentUser(name: String?) {
        currentUserName = if (name.isNullOrBlank()) "Vecino SJL" else name
    }

    fun loadWaterPoints() {
        loadFavoritos() // cargamos también los puntos guardados del usuario
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _waterPoints.value = repository.getWaterPoints()
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addWaterPoint(point: WaterPoint) {
        viewModelScope.launch {
            repository.addWaterPoint(point)
            loadWaterPoints()
        }
    }

    fun loadDetails(pointId: String) {
        viewModelScope.launch {
            _comments.value = repository.getComments(pointId)
            _reports.value = repository.getReports(pointId)
            _miValoracion.value = repository.getMiValoracion(pointId)
        }
    }

    /**
     * Registra/modifica la valoración del usuario para un punto. Tras votar,
     * recarga los puntos para reflejar el nuevo promedio (lo recalcula Supabase).
     */
    fun valorarPunto(pointId: String, valor: Int) {
        viewModelScope.launch {
            val resultado = repository.valorarPunto(pointId, valor)
            resultado.onSuccess {
                _miValoracion.value = valor
                // Recargamos los puntos para traer la calificación promedio actualizada
                _waterPoints.value = repository.getWaterPoints()
            }
        }
    }

    fun addComment(pointId: String, content: String, rating: Int) {
        viewModelScope.launch {
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                author = currentUserName,
                content = ModeracionPalabras.censurar(content),
                rating = rating,
                date = DateUtils.fechaHoraActual()
            )
            repository.addComment(pointId, comment)
            loadDetails(pointId)
        }
    }

    /**
     * Agrega un reporte. Opcionalmente con una foto (bytes leídos desde la UI)
     * que se subirá a Supabase Storage dentro del repositorio.
     */
    fun addReport(report: WaterPointReport, imageBytes: ByteArray? = null) {
        viewModelScope.launch {
            // Censuramos la descripción antes de guardar
            val reporteLimpio = report.copy(
                description = ModeracionPalabras.censurar(report.description)
            )
            repository.addReport(reporteLimpio, imageBytes)
            loadDetails(report.pointId)
        }
    }

    fun loadCommunityData() {
        viewModelScope.launch {
            _news.value = repository.getNews()
            // Reportes reales de la comunidad
            _recentReports.value = repository.getRecentReports()
            // Aseguramos tener los puntos cargados para poder mostrar el nombre del punto reportado
            if (_waterPoints.value.isEmpty()) {
                _waterPoints.value = repository.getWaterPoints()
            }
        }
    }
}
