package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.BicicletaDao
import com.example.data.dao.ClienteDao
import com.example.data.dao.FotoEvidenciaDao
import com.example.data.dao.RepuestoUsadoDao
import com.example.data.dao.TrabajoDao
import com.example.data.model.Bicicleta
import com.example.data.model.Cliente
import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.FotoEvidencia
import com.example.data.model.RepuestoUsado
import com.example.data.model.Trabajo

class Converters {
    @TypeConverter
    fun fromEstadoTrabajo(value: EstadoTrabajo?): String = value?.name ?: EstadoTrabajo.PRESUPUESTO_GENERADO.name

    @TypeConverter
    fun toEstadoTrabajo(value: String?): EstadoTrabajo = try {
        value?.let { EstadoTrabajo.valueOf(it) } ?: EstadoTrabajo.PRESUPUESTO_GENERADO
    } catch (_: Exception) {
        EstadoTrabajo.PRESUPUESTO_GENERADO
    }

    @TypeConverter
    fun fromEstadoPago(value: EstadoPago?): String = value?.name ?: EstadoPago.PENDIENTE.name

    @TypeConverter
    fun toEstadoPago(value: String?): EstadoPago = try {
        value?.let { EstadoPago.valueOf(it) } ?: EstadoPago.PENDIENTE
    } catch (_: Exception) {
        EstadoPago.PENDIENTE
    }
}

@Database(
    entities = [
        Cliente::class,
        Bicicleta::class,
        Trabajo::class,
        RepuestoUsado::class,
        FotoEvidencia::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BiciTallerDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun bicicletaDao(): BicicletaDao
    abstract fun trabajoDao(): TrabajoDao
    abstract fun repuestoUsadoDao(): RepuestoUsadoDao
    abstract fun fotoEvidenciaDao(): FotoEvidenciaDao

    companion object {
        @Volatile
        private var INSTANCE: BiciTallerDatabase? = null

        fun getDatabase(context: Context): BiciTallerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BiciTallerDatabase::class.java,
                    "bicitaller_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
