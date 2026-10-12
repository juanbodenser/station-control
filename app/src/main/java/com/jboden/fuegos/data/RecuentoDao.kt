package com.jboden.fuegos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecuentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRecuento(recuento: Recuento): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLineasRecuento(lineas: List<LineaRecuento>)

    @Transaction
    open suspend fun insertRecuentoConLineas(recuento: Recuento, lineas: List<LineaRecuento>): Long {
        val recuentoId = insertRecuento(recuento)
        val lineasConRecuentoId = lineas.map { it.copy(recuentoId = recuentoId) }
        insertLineasRecuento(lineasConRecuentoId)
        return recuentoId
    }

    @Query("SELECT * FROM recuentos WHERE seccionId = :seccionId ORDER BY fecha DESC LIMIT 1")
    abstract fun getUltimoRecuentoPorSeccion(seccionId: Long): Flow<Recuento?>

    @Query("SELECT * FROM lineas_recuento WHERE recuentoId = :recuentoId")
    abstract fun getLineasPorRecuento(recuentoId: Long): Flow<List<LineaRecuento>>

    @Query("SELECT * FROM recuentos")
    abstract fun getAllRecuentos(): Flow<List<Recuento>>

    @Query("SELECT * FROM lineas_recuento")
    abstract fun getAllLineasRecuento(): Flow<List<LineaRecuento>>

    @Query(
        "SELECT lr.productoId, lr.cantidad, r.fecha " +
        "FROM lineas_recuento lr " +
        "INNER JOIN recuentos r ON lr.recuentoId = r.id " +
        "INNER JOIN productos p ON lr.productoId = p.id " +
        "WHERE p.seccionId = :seccionId " +
        "AND r.fecha = (" +
        "    SELECT MAX(r2.fecha) " +
        "    FROM lineas_recuento lr2 " +
        "    INNER JOIN recuentos r2 ON lr2.recuentoId = r2.id " +
        "    WHERE lr2.productoId = lr.productoId" +
        ")"
    )
    abstract fun getUltimasCantidadesPorSeccion(seccionId: Long): Flow<List<ProductoUltimaCantidad>>

    @Query(
        "SELECT lr.id AS lineaRecuentoId, r.id AS recuentoId, p.id AS productoId, " +
        "p.nombre AS productoNombre, u.abreviatura AS unidadAbreviatura, lr.cantidad, r.fecha, " +
        "s.id AS seccionId, s.nombre AS seccionNombre " +
        "FROM lineas_recuento lr " +
        "INNER JOIN recuentos r ON lr.recuentoId = r.id " +
        "INNER JOIN productos p ON lr.productoId = p.id " +
        "INNER JOIN unidades u ON p.unidadId = u.id " +
        "INNER JOIN secciones s ON r.seccionId = s.id " +
        "ORDER BY r.fecha DESC"
    )
    abstract fun getRecuentoLineasConDetalle(): Flow<List<RecuentoLineaConDetalle>>

    @Query("UPDATE lineas_recuento SET cantidad = :cantidad WHERE id = :lineaId")
    abstract suspend fun updateLineaRecuentoCantidad(lineaId: Long, cantidad: Double)

    @Query("DELETE FROM lineas_recuento WHERE id = :lineaId")
    abstract suspend fun deleteLineaRecuento(lineaId: Long)

    @Query("SELECT COUNT(*) FROM lineas_recuento WHERE recuentoId = :recuentoId")
    abstract suspend fun countLineasByRecuento(recuentoId: Long): Int

    @Query("DELETE FROM recuentos WHERE id = :recuentoId")
    abstract suspend fun deleteRecuento(recuentoId: Long)
}
