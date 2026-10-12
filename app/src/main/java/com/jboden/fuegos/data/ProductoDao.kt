package com.jboden.fuegos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE seccionId = :seccionId AND activo = 1 ORDER BY orden ASC")
    fun getProductosActivosPorSeccion(seccionId: Long): Flow<List<Producto>>

    @Query("SELECT * FROM productos")
    fun getAllProductos(): Flow<List<Producto>>

    @Query(
        "SELECT productos.*, unidades.abreviatura AS abreviaturaUnidad " +
        "FROM productos " +
        "INNER JOIN unidades ON productos.unidadId = unidades.id"
    )
    fun getAllProductosWithUnit(): Flow<List<ProductoConUnidad>>

    @Query(
        "SELECT productos.*, unidades.abreviatura AS abreviaturaUnidad " +
        "FROM productos " +
        "INNER JOIN unidades ON productos.unidadId = unidades.id " +
        "WHERE productos.seccionId = :seccionId AND productos.activo = 1 " +
        "ORDER BY productos.orden ASC"
    )
    fun getProductosConUnidadPorSeccion(seccionId: Long): Flow<List<ProductoConUnidad>>

    @Query(
        "SELECT productos.*, unidades.abreviatura AS abreviaturaUnidad " +
        "FROM productos " +
        "INNER JOIN unidades ON productos.unidadId = unidades.id " +
        "WHERE productos.seccionId = :seccionId " +
        "ORDER BY productos.orden ASC"
    )
    fun getProductosConUnidadTodosPorSeccion(seccionId: Long): Flow<List<ProductoConUnidad>>

    @Query("SELECT COUNT(*) FROM lineas_recuento WHERE productoId = :productoId")
    suspend fun countLineasRecuentoByProducto(productoId: Long): Int

    @Query("SELECT COUNT(*) FROM entradas WHERE productoId = :productoId")
    suspend fun countEntradasByProducto(productoId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: Producto): Long

    @Update
    suspend fun update(producto: Producto)

    @Update
    suspend fun updateAll(productos: List<Producto>)

    @Delete
    suspend fun delete(producto: Producto)
}
