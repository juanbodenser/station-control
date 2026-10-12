package com.jboden.fuegos.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromOrigen(origen: Origen?): String? {
        return origen?.name
    }

    @TypeConverter
    fun toOrigen(value: String?): Origen? {
        return value?.let { enumValueOf<Origen>(it) }
    }
}
