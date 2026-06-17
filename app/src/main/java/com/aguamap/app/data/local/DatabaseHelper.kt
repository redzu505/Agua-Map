package com.aguamap.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aguamap.app.domain.*

/**
 * CAPA DE DATOS - SQLite Helper
 * Gestiona el esquema de la base de datos local para la estrategia Offline-First.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "aguamap_db"
        private const val DATABASE_VERSION = 2

        // Tablas
        const val TABLE_PUNTOS = "puntos_agua"
        const val TABLE_NOTICIAS = "noticias_comunidad"
        const val TABLE_REPORTES = "reportes_locales"
        const val TABLE_COMENTARIOS = "comentarios_cache"

        // Columnas Comunes
        const val COLUMN_ID = "id"
        const val COLUMN_DATE = "fecha"

        // Columnas Puntos
        const val COL_NAME = "nombre"
        const val COL_ADDRESS = "direccion"
        const val COL_RATING = "calificacion"
        const val COL_DISTANCE = "distancia"
        const val COL_HOURS = "horario"
        const val COL_STATUS = "estado"
        const val COL_TYPE = "tipo"
        const val COL_LAT = "latitud"
        const val COL_LNG = "longitud"
        const val COL_IMAGE = "imagen_url"

        // Columnas Reportes
        const val COL_R_POINT_ID = "punto_id"
        const val COL_R_TYPE = "tipo_reporte"
        const val COL_R_DESC = "descripcion"
        const val COL_R_IMAGE = "imagen_url"
        const val COL_R_SYNCED = "sincronizado"

        // Columnas Comentarios
        const val COL_C_POINT_ID = "punto_id"
        const val COL_C_AUTHOR = "autor"
        const val COL_C_CONTENT = "contenido"
        const val COL_C_RATING = "calificacion_valor"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createPuntos = """
            CREATE TABLE $TABLE_PUNTOS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COL_NAME TEXT,
                $COL_ADDRESS TEXT,
                $COL_RATING REAL,
                $COL_DISTANCE TEXT,
                $COL_HOURS TEXT,
                $COL_STATUS TEXT,
                $COL_TYPE TEXT,
                $COL_LAT REAL,
                $COL_LNG REAL,
                $COL_IMAGE TEXT
            )
        """.trimIndent()

        val createNoticias = """
            CREATE TABLE $TABLE_NOTICIAS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                "titulo" TEXT,
                "contenido" TEXT,
                $COLUMN_DATE TEXT
            )
        """.trimIndent()

        val createReportes = """
            CREATE TABLE $TABLE_REPORTES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COL_R_POINT_ID TEXT,
                $COL_R_TYPE TEXT,
                $COL_R_DESC TEXT,
                $COL_R_IMAGE TEXT,
                $COLUMN_DATE TEXT,
                $COL_R_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createComentarios = """
            CREATE TABLE $TABLE_COMENTARIOS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COL_C_POINT_ID TEXT,
                $COL_C_AUTHOR TEXT,
                $COL_C_CONTENT TEXT,
                $COL_C_RATING INTEGER,
                $COLUMN_DATE TEXT
            )
        """.trimIndent()

        db.execSQL(createPuntos)
        db.execSQL(createNoticias)
        db.execSQL(createReportes)
        db.execSQL(createComentarios)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PUNTOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTICIAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMENTARIOS")
        onCreate(db)
    }

    // --- MÉTODOS DE ACCESO A DATOS (DAO Simulado) ---

    fun insertWaterPoint(point: WaterPoint) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, point.id)
            put(COL_NAME, point.name)
            put(COL_ADDRESS, point.address)
            put(COL_RATING, point.rating)
            put(COL_DISTANCE, point.distance)
            put(COL_HOURS, point.hours)
            put(COL_STATUS, point.status.name)
            put(COL_TYPE, point.type.name)
            put(COL_LAT, point.latitude)
            put(COL_LNG, point.longitude)
            put(COL_IMAGE, point.imageUrl)
        }
        db.insertWithOnConflict(TABLE_PUNTOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllWaterPoints(): List<WaterPoint> {
        val points = mutableListOf<WaterPoint>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PUNTOS, null, null, null, null, null, null)
        
        with(cursor) {
            while (moveToNext()) {
                points.add(
                    WaterPoint(
                        id = getString(getColumnIndexOrThrow(COLUMN_ID)),
                        name = getString(getColumnIndexOrThrow(COL_NAME)),
                        address = getString(getColumnIndexOrThrow(COL_ADDRESS)),
                        rating = getDouble(getColumnIndexOrThrow(COL_RATING)),
                        distance = getString(getColumnIndexOrThrow(COL_DISTANCE)),
                        hours = getString(getColumnIndexOrThrow(COL_HOURS)),
                        status = WaterPointStatus.valueOf(getString(getColumnIndexOrThrow(COL_STATUS))),
                        type = WaterPointType.valueOf(getString(getColumnIndexOrThrow(COL_TYPE))),
                        latitude = getDouble(getColumnIndexOrThrow(COL_LAT)),
                        longitude = getDouble(getColumnIndexOrThrow(COL_LNG)),
                        imageUrl = getString(getColumnIndexOrThrow(COL_IMAGE))
                    )
                )
            }
            close()
        }
        return points
    }

    fun insertComment(pointId: String, comment: Comment) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, comment.id)
            put(COL_C_POINT_ID, pointId)
            put(COL_C_AUTHOR, comment.author)
            put(COL_C_CONTENT, comment.content)
            put(COL_C_RATING, comment.rating)
            put(COLUMN_DATE, comment.date)
        }
        db.insertWithOnConflict(TABLE_COMENTARIOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCommentsForPoint(pointId: String): List<Comment> {
        val comments = mutableListOf<Comment>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COMENTARIOS, 
            null, 
            "$COL_C_POINT_ID = ?", 
            arrayOf(pointId), 
            null, null, "$COLUMN_DATE DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                comments.add(
                    Comment(
                        id = getString(getColumnIndexOrThrow(COLUMN_ID)),
                        author = getString(getColumnIndexOrThrow(COL_C_AUTHOR)),
                        content = getString(getColumnIndexOrThrow(COL_C_CONTENT)),
                        rating = getInt(getColumnIndexOrThrow(COL_C_RATING)),
                        date = getString(getColumnIndexOrThrow(COLUMN_DATE))
                    )
                )
            }
            close()
        }
        return comments
    }

    fun insertReport(report: WaterPointReport) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, report.id)
            put(COL_R_POINT_ID, report.pointId)
            put(COL_R_TYPE, report.type.name)
            put(COL_R_DESC, report.description)
            put(COL_R_IMAGE, report.imageUrl)
            put(COLUMN_DATE, report.date)
            put(COL_R_SYNCED, 0)
        }
        db.insertWithOnConflict(TABLE_REPORTES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getReportsForPoint(pointId: String): List<WaterPointReport> {
        val reports = mutableListOf<WaterPointReport>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REPORTES,
            null,
            "$COL_R_POINT_ID = ?",
            arrayOf(pointId),
            null, null, "$COLUMN_DATE DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                reports.add(
                    WaterPointReport(
                        id = getString(getColumnIndexOrThrow(COLUMN_ID)),
                        pointId = getString(getColumnIndexOrThrow(COL_R_POINT_ID)),
                        type = ReportType.valueOf(getString(getColumnIndexOrThrow(COL_R_TYPE))),
                        description = getString(getColumnIndexOrThrow(COL_R_DESC)),
                        date = getString(getColumnIndexOrThrow(COLUMN_DATE)),
                        imageUrl = getString(getColumnIndexOrThrow(COL_R_IMAGE))
                    )
                )
            }
            close()
        }
        return reports
    }

    fun getAllReports(): List<WaterPointReport> {
        val reports = mutableListOf<WaterPointReport>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REPORTES,
            null,
            null,
            null,
            null, null, "$COLUMN_DATE DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                reports.add(
                    WaterPointReport(
                        id = getString(getColumnIndexOrThrow(COLUMN_ID)),
                        pointId = getString(getColumnIndexOrThrow(COL_R_POINT_ID)),
                        type = ReportType.valueOf(getString(getColumnIndexOrThrow(COL_R_TYPE))),
                        description = getString(getColumnIndexOrThrow(COL_R_DESC)),
                        date = getString(getColumnIndexOrThrow(COLUMN_DATE)),
                        imageUrl = getString(getColumnIndexOrThrow(COL_R_IMAGE))
                    )
                )
            }
            close()
        }
        return reports
    }

    // --- NOTICIAS ---
    fun insertNews(news: CommunityNews) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("titulo", news.title)
            put("contenido", news.content)
            put(COLUMN_DATE, news.date)
        }
        db.insert(TABLE_NOTICIAS, null, values)
    }

    fun getAllNews(): List<CommunityNews> {
        val newsList = mutableListOf<CommunityNews>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NOTICIAS, null, null, null, null, null, "$COLUMN_DATE DESC")
        with(cursor) {
            while (moveToNext()) {
                newsList.add(
                    CommunityNews(
                        id = getInt(getColumnIndexOrThrow(COLUMN_ID)),
                        title = getString(getColumnIndexOrThrow("titulo")),
                        content = getString(getColumnIndexOrThrow("contenido")),
                        date = getString(getColumnIndexOrThrow(COLUMN_DATE))
                    )
                )
            }
            close()
        }
        return newsList
    }
}
