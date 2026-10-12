package com.jboden.fuegos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntradaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entrada: Entrada): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entradas: List<Entrada>)

    @Query("SELECT * FROM entradas WHERE productoId = :productoId AND fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    fun getEntradasPorProductoEntreFechas(productoId: Long, fechaInicio: Long, fechaFin: Long): Flow<List<Entrada>>

    @Query("SELECT * FROM entradas")
    fun getAllEntradas(): Flow<List<Entrada>>

    @Query(
        "SELECT p.id AS productoId, COALESCE(SUM(e.cantidad), 0.0) AS sumaEntradas " +
        "FROM productos p " +
        "LEFT JOIN entradas e ON p.id = e.productoId " +
        "AND e.fecha > COALESCE((" +
        "    SELECT MAX(r.fecha) " +
        "    FROM lineas_recuento lr " +
        "    INNER JOIN recuentos r ON lr.recuentoId = r.id " +
        "    WHERE lr.productoId = p.id" +
        "), 0) " +
        "WHERE p.seccionId = :seccionId " +
        "GROUP BY p.id"
    )
    fun getSumaEntradasPosterioresPorSeccion(seccionId: Long): Flow<List<ProductoEntradasPosteriores>>

    @Query(
        "SELECT e.id AS entradaId, p.id AS productoId, p.nombre AS productoNombre, " +
        "u.abreviatura AS unidadAbreviatura, e.cantidad, e.fecha, " +
        "s.id AS seccionId, s.nombre AS seccionNombre " +
        "FROM entradas e " +
        "INNER JOIN productos p ON e.productoId = p.id " +
        "INNER JOIN unidades u ON p.unidadId = u.id " +
        "INNER JOIN secciones s ON p.seccionId = s.id " +
        "ORDER BY e.fecha DESC"
    )
    fun getEntradasConDetalle(): Flow<List<EntradaConDetalle>>

    @Query("UPDATE entradas SET cantidad = :cantidad WHERE id = :entradaId")
    suspend fun updateEntradaCantidad(entradaId: Long, cantidad: Double)

    @Query("DELETE FROM entradas WHERE id = :entradaId")
    suspend fun deleteEntrada(entradaId: Long)
}
