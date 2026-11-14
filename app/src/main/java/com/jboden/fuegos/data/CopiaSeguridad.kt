package com.jboden.fuegos.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class CopiaSeguridad(private val database: FuegosDatabase, private val context: Context) {

    private val prefs = context.getSharedPreferences("fuegos_copia_prefs", Context.MODE_PRIVATE)

    suspend fun exportarCopia(uri: Uri): Result<Unit> = runCatching {
        val unidades = database.unidadDao().getUnidades().first()
        val secciones = database.seccionDao().getSecciones().first()
        val productos = database.productoDao().getAllProductos().first()
        val recuentos = database.recuentoDao().getAllRecuentos().first()
        val lineasRecuento = database.recuentoDao().getAllLineasRecuento().first()
        val entradas = database.entradaDao().getAllEntradas().first()
        val tareas = database.tareaProduccionDao().getAllTareas().first()

        val plantillaEncabezado = prefs.getString("plantilla_encabezado", "Producción para Fuegos:")
        val plantillaCierre = prefs.getString("plantilla_cierre", "Gracias, buenas noches.")
        val partidaEncabezado = prefs.getString("partida_plantilla_encabezado", null)
        val partidaCierre = prefs.getString("partida_plantilla_cierre", null)

        val json = JSONObject().apply {
            put("formato", "fuegos-backup")
            put("version", 3)
            put("fechaExportacion", System.currentTimeMillis())
            put("plantillaEncabezado", plantillaEncabezado)
            put("plantillaCierre", plantillaCierre)
            partidaEncabezado?.let { put("partidaPlantillaEncabezado", it) }
            partidaCierre?.let { put("partidaPlantillaCierre", it) }

            put("unidades", JSONArray().apply {
                unidades.forEach { u ->
                    put(JSONObject().apply {
                        put("id", u.id)
                        put("nombre", u.nombre)
                        put("abreviatura", u.abreviatura)
                    })
                }
            })

            put("secciones", JSONArray().apply {
                secciones.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("nombre", s.nombre)
                        put("orden", s.orden)
                        put("origen", s.origen.name)
                    })
                }
            })

            put("productos", JSONArray().apply {
                productos.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("seccionId", p.seccionId)
                        put("unidadId", p.unidadId)
                        put("nombre", p.nombre)
                        put("orden", p.orden)
                        put("activo", p.activo)
                    })
                }
            })

            put("recuentos", JSONArray().apply {
                recuentos.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("seccionId", r.seccionId)
                        put("fecha", r.fecha)
                    })
                }
            })

            put("lineasRecuento", JSONArray().apply {
                lineasRecuento.forEach { lr ->
                    put(JSONObject().apply {
                        put("id", lr.id)
                        put("recuentoId", lr.recuentoId)
                        put("productoId", lr.productoId)
                        put("cantidad", lr.cantidad)
                    })
                }
            })

            put("entradas", JSONArray().apply {
                entradas.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("productoId", e.productoId)
                        put("fecha", e.fecha)
                        put("cantidad", e.cantidad)
                        put("origen", e.origen.name)
                    })
                }
            })

            put("tareasProduccion", JSONArray().apply {
                tareas.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("texto", t.texto)
                        put("orden", t.orden)
                        put("marcada", t.marcada)
                        put("lista", t.lista)
                    })
                }
            })
        }

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(json.toString(2).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")
    }

    suspend fun importarCopia(uri: Uri): Result<Unit> = runCatching {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

        val json = JSONObject(jsonString)

        if (json.optString("formato") != "fuegos-backup") {
            throw IllegalArgumentException("El archivo no es una copia de seguridad válida de Fuegos")
        }
        val version = json.optInt("version", 0)
        if (version > 3) {
            throw IllegalArgumentException("La versión del archivo de copia ($version) es superior a la soportada por la aplicación (3)")
        }

        val unidadesJson = json.getJSONArray("unidades")
        val seccionesJson = json.getJSONArray("secciones")
        val productosJson = json.getJSONArray("productos")
        val recuentosJson = json.getJSONArray("recuentos")
        val lineasRecuentoJson = json.getJSONArray("lineasRecuento")
        val entradasJson = json.getJSONArray("entradas")
        val tareasJson = json.optJSONArray("tareasProduccion")

        val plantillaEncabezado = json.optString("plantillaEncabezado", "")
        val plantillaCierre = json.optString("plantillaCierre", "")
        val partidaEncabezado = if (json.has("partidaPlantillaEncabezado")) json.getString("partidaPlantillaEncabezado") else null
        val partidaCierre = if (json.has("partidaPlantillaCierre")) json.getString("partidaPlantillaCierre") else null

        val unidadesList = mutableListOf<Unidad>()
        for (i in 0 until unidadesJson.length()) {
            val obj = unidadesJson.getJSONObject(i)
            unidadesList.add(
                Unidad(
                    id = obj.getLong("id"),
                    nombre = obj.getString("nombre"),
                    abreviatura = obj.getString("abreviatura")
                )
            )
        }

        val seccionesList = mutableListOf<Seccion>()
        for (i in 0 until seccionesJson.length()) {
            val obj = seccionesJson.getJSONObject(i)
            seccionesList.add(
                Seccion(
                    id = obj.getLong("id"),
                    nombre = obj.getString("nombre"),
                    orden = obj.getInt("orden"),
                    origen = Origen.valueOf(obj.getString("origen"))
                )
            )
        }

        val productosList = mutableListOf<Producto>()
        for (i in 0 until productosJson.length()) {
            val obj = productosJson.getJSONObject(i)
            productosList.add(
                Producto(
                    id = obj.getLong("id"),
                    seccionId = obj.getLong("seccionId"),
                    unidadId = obj.getLong("unidadId"),
                    nombre = obj.getString("nombre"),
                    orden = obj.getInt("orden"),
                    activo = obj.getBoolean("activo")
                )
            )
        }

        val recuentosList = mutableListOf<Recuento>()
        for (i in 0 until recuentosJson.length()) {
            val obj = recuentosJson.getJSONObject(i)
            recuentosList.add(
                Recuento(
                    id = obj.getLong("id"),
                    seccionId = obj.getLong("seccionId"),
                    fecha = obj.getLong("fecha")
                )
            )
        }

        val lineasRecuentoList = mutableListOf<LineaRecuento>()
        for (i in 0 until lineasRecuentoJson.length()) {
            val obj = lineasRecuentoJson.getJSONObject(i)
            lineasRecuentoList.add(
                LineaRecuento(
                    id = obj.getLong("id"),
                    recuentoId = obj.getLong("recuentoId"),
                    productoId = obj.getLong("productoId"),
                    cantidad = obj.getDouble("cantidad")
                )
            )
        }

        val entradasList = mutableListOf<Entrada>()
        for (i in 0 until entradasJson.length()) {
            val obj = entradasJson.getJSONObject(i)
            entradasList.add(
                Entrada(
                    id = obj.getLong("id"),
                    productoId = obj.getLong("productoId"),
                    fecha = obj.getLong("fecha"),
                    cantidad = obj.getDouble("cantidad"),
                    origen = Origen.valueOf(obj.getString("origen"))
                )
            )
        }

        val tareasList = mutableListOf<TareaProduccion>()
        // Las copias anteriores a la pestaña Partida no traen el campo "lista":
        // en ese caso solo se sustituyen las tareas de Producción y se conserva Partida.
        var copiaConListas = false
        if (tareasJson != null) {
            for (i in 0 until tareasJson.length()) {
                if (tareasJson.getJSONObject(i).has("lista")) copiaConListas = true
            }
            for (i in 0 until tareasJson.length()) {
                val obj = tareasJson.getJSONObject(i)
                tareasList.add(
                    TareaProduccion(
                        id = if (copiaConListas) obj.getLong("id") else 0,
                        texto = obj.getString("texto"),
                        orden = obj.getInt("orden"),
                        marcada = obj.getBoolean("marcada"),
                        lista = obj.optString("lista", LISTA_PRODUCCION)
                    )
                )
            }
        }

        database.withTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM lineas_recuento")
            db.execSQL("DELETE FROM entradas")
            db.execSQL("DELETE FROM recuentos")
            db.execSQL("DELETE FROM productos")
            db.execSQL("DELETE FROM secciones")
            db.execSQL("DELETE FROM unidades")
            if (tareasJson != null) {
                if (copiaConListas) {
                    db.execSQL("DELETE FROM tareas_produccion")
                } else {
                    db.execSQL("DELETE FROM tareas_produccion WHERE lista = ?", arrayOf<Any>(LISTA_PRODUCCION))
                }
            }

            val unidadDao = database.unidadDao()
            val seccionDao = database.seccionDao()
            val productoDao = database.productoDao()
            val recuentoDao = database.recuentoDao()
            val entradaDao = database.entradaDao()
            val tareaDao = database.tareaProduccionDao()

            unidadesList.forEach { unidadDao.insert(it) }
            seccionesList.forEach { seccionDao.insert(it) }
            productosList.forEach { productoDao.insert(it) }
            recuentosList.forEach { recuentoDao.insertRecuento(it) }
            if (lineasRecuentoList.isNotEmpty()) {
                recuentoDao.insertLineasRecuento(lineasRecuentoList)
            }
            if (entradasList.isNotEmpty()) {
                entradaDao.insertAll(entradasList)
            }
            if (tareasJson != null && tareasList.isNotEmpty()) {
                tareasList.forEach { tareaDao.insert(it) }
            }
        }

        if (plantillaEncabezado.isNotBlank()) {
            prefs.edit().putString("plantilla_encabezado", plantillaEncabezado).apply()
        }
        partidaEncabezado?.let { prefs.edit().putString("partida_plantilla_encabezado", it).apply() }
        partidaCierre?.let { prefs.edit().putString("partida_plantilla_cierre", it).apply() }
        if (plantillaCierre.isNotBlank()) {
            prefs.edit().putString("plantilla_cierre", plantillaCierre).apply()
        }
    }
}
