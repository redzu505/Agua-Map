package com.aguamap.app.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aguamap.app.data.local.DatabaseHelper
import com.aguamap.app.data.local.LocalDataSource
import com.aguamap.app.data.local.SessionManager
import com.aguamap.app.data.local.FavoritosManager
import com.aguamap.app.data.remote.RemoteDataSource
import com.aguamap.app.data.remote.RetrofitClient
import com.aguamap.app.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncReportWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dbHelper = DatabaseHelper(context)
        val sessionManager = SessionManager(context)
        val repository = AppRepository(
            LocalDataSource(dbHelper),
            RemoteDataSource(RetrofitClient.supabaseApi),
            sessionManager,
            FavoritosManager(context),
            context
        )

        // 1. Obtener la lista de reportes que aún no se han sincronizado (COL_R_SYNCED = 0)
        val unsyncedReports = dbHelper.getUnsyncedReports()

        if (unsyncedReports.isEmpty()) return@withContext Result.success()

        var anyFailures = false

        // 2. Procesar cada reporte pendiente
        for (report in unsyncedReports) {
            try {
                // 3. Intentar recuperar los bytes de la imagen si hay una ruta local guardada
                val imageBytes = if (!report.imageUrl.isNullOrBlank() && !report.imageUrl.startsWith("http")) {
                    try {
                        val file = java.io.File(report.imageUrl)
                        if (file.exists()) {
                            // Caso A: Es una ruta de archivo física (/data/user/0/...)
                            file.readBytes()
                        } else {
                            // Caso B: Es una URI (content://)
                            val uri = android.net.Uri.parse(report.imageUrl)
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SyncReportWorker", "No se pudo leer la imagen local: ${report.imageUrl}", e)
                        null
                    }
                } else null

                // 4. Intentar subir el reporte (con sus bytes si existen) usando el repositorio.
                val result = repository.addReport(report, imageBytes)
                
                if (result.isSuccess) {
                    // 5. Si la subida fue exitosa, marcar en SQLite como sincronizado
                    dbHelper.markReportAsSynced(report.id)
                } else {
                    // 6. Si falló (ej. error de servidor), registramos y marcamos para reintento
                    android.util.Log.e("SyncReportWorker", "Error al sincronizar reporte ${report.id}: ${result.exceptionOrNull()?.message}")
                    anyFailures = true
                }
            } catch (e: Exception) {
                // Si hubo un corte de internet o excepción, registramos y marcamos para reintento
                android.util.Log.e("SyncReportWorker", "Excepción al sincronizar reporte ${report.id}", e)
                anyFailures = true
            }
        }

        // 6. Si hubo algún error durante el proceso, solicitamos un reintento automático
        if (anyFailures) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
