package com.example.a039a21gestormedicamentos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamentos ORDER BY nombre")
    fun observarMedicamentos(): Flow<List<MedicamentoEntity>>

    @Query("SELECT * FROM medicamentos")
    suspend fun obtenerMedicamentos(): List<MedicamentoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMedicamento(medicamento: MedicamentoEntity)

    @Insert
    suspend fun insertarToma(toma: TomaEntity)

    @Query("SELECT * FROM tomas WHERE fecha = :fecha ORDER BY tomadaEnMillis DESC")
    fun observarTomasDelDia(fecha: String): Flow<List<TomaEntity>>

    @Query(
        "SELECT COUNT(*) > 0 FROM tomas " +
                "WHERE medicamentoId = :medicamentoId AND fecha = :fecha AND minutoProgramado = :minuto"
    )
    suspend fun existeToma(medicamentoId: String, fecha: String, minuto: Int): Boolean

    @Query("SELECT * FROM tomas WHERE fecha BETWEEN :desde AND :hasta")
    fun observarTomasEntre(desde: String, hasta: String): Flow<List<TomaEntity>>

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    suspend fun obtenerMedicamento(id: String): MedicamentoEntity?

    /** Resta una dosis sin bajar de 0. No hace nada si el medicamento no lleva inventario. */
    @Query(
        "UPDATE medicamentos SET inventario = MAX(inventario - :cantidad, 0) " +
                "WHERE id = :id AND inventario IS NOT NULL"
    )
    suspend fun descontarInventario(id: String, cantidad: Double)

    @Query("UPDATE medicamentos SET inventario = COALESCE(inventario, 0) + :cantidad WHERE id = :id")
    suspend fun reabastecer(id: String, cantidad: Double)
}