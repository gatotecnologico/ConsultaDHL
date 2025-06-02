package com.example.consultadhl

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

class MainActivity : AppCompatActivity() {

    private val DB_URL = "jdbc:mysql://10.0.2.2:3306/dhl"
    private val USER = "root"
    private val PASS = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_envio)

        val editTextIdEnvio = findViewById<EditText>(R.id.editTextIdEnvio)
        val buttonConsultar = findViewById<Button>(R.id.buttonConsultar)
        val textEstado = findViewById<TextView>(R.id.textEstado)
        val textFechaEstimada = findViewById<TextView>(R.id.textFechaEstimada)
        val textRutaAvion = findViewById<TextView>(R.id.textRutaAvion)

        buttonConsultar.setOnClickListener {
            val idEnvio = editTextIdEnvio.text.toString()
            if (idEnvio.isNotEmpty()) {
                // Iniciar una corrutina para hacer la operación de DB en segundo plano
                CoroutineScope(Dispatchers.IO).launch {
                    var connection: Connection? = null
                    var statement: Statement? = null
                    var resultSet: ResultSet? = null

                    try {
                        Class.forName("com.mysql.jdbc.Driver")

                        connection = DriverManager.getConnection(DB_URL, USER, PASS)

                        val query = "SELECT id_remitente, id_destinatario, fecha_registro, estado FROM envios WHERE id_envio = '$idEnvio'" // ¡PELIGRO de SQL Injection!
                        statement = connection.createStatement()
                        resultSet = statement.executeQuery(query)

                        var estado = "No encontrado"
                        var fechaEstimada = "-"
                        var rutaAvion = "-"

                        if (resultSet.next()) {
                            estado = resultSet.getString("estado")
                            fechaEstimada = resultSet.getString("fecha_registro") // Asumimos que la DB devuelve un String legible
                            rutaAvion = resultSet.getString("id_remitente")
                        }

                        // Volver al hilo principal (UI thread) para actualizar la interfaz
                        withContext(Dispatchers.Main) {
                            textEstado.text = "Estado: $estado"
                            textFechaEstimada.text = "Fecha estimada de llegada: $fechaEstimada"
                            textRutaAvion.text = "Ruta del avión: $rutaAvion"
                        }

                    } catch (e: Exception) {
                        Log.e("DB_CONNECTION", "Error al conectar o consultar MySQL: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            textEstado.text = "Estado: Error de conexión/consulta"
                            textFechaEstimada.text = "Fecha estimada de llegada: -"
                            textRutaAvion.text = "Ruta del avión: -"
                        }
                    } finally {
                        // Cerrar recursos en el orden inverso de apertura para evitar fugas
                        resultSet?.close()
                        statement?.close()
                        connection?.close()
                    }
                }
            } else {
                textEstado.text = "Estado: ID no válido"
                textFechaEstimada.text = "Fecha estimada de llegada: -"
                textRutaAvion.text = "Ruta del avión: -"
            }
        }
    }
}
