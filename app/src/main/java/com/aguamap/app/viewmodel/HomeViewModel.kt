package com.aguamap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aguamap.app.data.repository.AppRepository
import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.CommunityNews
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
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

    private val _news = MutableStateFlow<List<CommunityNews>>(emptyList())
    val news: StateFlow<List<CommunityNews>> = _news

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
        }
    }

    fun addComment(pointId: String, content: String, rating: Int) {
        viewModelScope.launch {
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                author = currentUserName,
                content = content,
                rating = rating,
                date = "Hoy"
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
            repository.addReport(report, imageBytes)
            loadDetails(report.pointId)
        }
    }

    fun loadCommunityData() {
        viewModelScope.launch {
            _news.value = repository.getNews()
        }
    }
}
