package com.aguamap.app.data.local

import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.CommunityNews
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport

/**
 * CAPA DE DATOS - LOCAL
 * Gestiona la interacción con la base de datos SQLite y preferencias locales.
 */
class LocalDataSource(private val dbHelper: DatabaseHelper) {

    fun getWaterPoints(): List<WaterPoint> = dbHelper.getAllWaterPoints()

    fun saveWaterPoint(point: WaterPoint) = dbHelper.insertWaterPoint(point)

    fun getComments(pointId: String): List<Comment> = dbHelper.getCommentsForPoint(pointId)

    fun saveComment(pointId: String, comment: Comment) = dbHelper.insertComment(pointId, comment)

    fun getReports(pointId: String): List<WaterPointReport> = dbHelper.getReportsForPoint(pointId)

    fun saveReport(report: WaterPointReport) = dbHelper.insertReport(report)

    fun getNews(): List<CommunityNews> = dbHelper.getAllNews()

    fun saveNews(news: CommunityNews) = dbHelper.insertNews(news)
}
