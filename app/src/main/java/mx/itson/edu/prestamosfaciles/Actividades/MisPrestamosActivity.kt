package mx.itson.edu.prestamosfaciles.Actividades

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import mx.itson.edu.prestamosfaciles.Entidades.Producto
import mx.itson.edu.prestamosfaciles.Entidades.User
import mx.itson.edu.prestamosfaciles.R

class MisPrestamosActivity : AppCompatActivity() {

    private val productoRef = FirebaseFirestore.getInstance().collection("productos")
    private val rentasRef = FirebaseFirestore.getInstance().collection("rentas")
    private val arrayMisProductos: ArrayList<Producto> = arrayListOf()
    private val storageProductoRef = FirebaseStorage.getInstance().reference

    var idUsuario: String? = null
    var correo: String? = null
    var nombre: String? = null

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_prestamos)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        val bundle = intent.extras

        val btn_back: Button = findViewById(R.id.btn_back)

        if (bundle != null) {
            nombre = bundle.getString("name")
            correo = bundle.getString("email")
            idUsuario = bundle.getString("id")
        }

        swipeRefreshLayout.setOnRefreshListener {
            // Llamamos a la función que se encarga de actualizar los datos
            cargarMisPrestamos(idUsuario)
        }


        val gridview: GridView = findViewById(R.id.id_gridMisPrestamos)

        gridview.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { parent, view, position, id ->

                // Obtenemos el producto seleccionado por el usuario
                val selectedItem = parent.getItemAtPosition(position) as Producto

                // Crea un diálogo con las opciones de acción
                val dialog = AlertDialog.Builder(this)
                    .setTitle("¿Qué deseas hacer?")
                    .setItems(
                        arrayOf(
                            "Actualizar",
                            "Eliminar"
                        )
                    ) { _, which ->
                        when (which) {
                            0 -> {
                                // Actualizamos producto
                                actualizarProducto(selectedItem)
                            }
                            1 -> {
                                // Eliminamos producto
                                eliminarProducto(selectedItem)
                            }
                        }
                    }
                    .create()

                // Mostrar el diálogo
                dialog.show()

                // Devuelve verdadero para indicar que el evento ha sido manejado
                true
            }

        btn_back.setOnClickListener { finish() }

        val helpButton: ImageView = findViewById(R.id.helpButton)
        helpButton.setOnClickListener {
            Toast.makeText(
                this,
                "Aquí encontrarás información sobre los productos que estás prestando.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    override fun onResume() {
        super.onResume()
        cargarMisPrestamos(idUsuario)
    }

    private fun actualizarProducto(producto: Producto){
        var intent = Intent(this, AgregarProductoActivity::class.java)
        intent.putExtra("producto",producto)
        intent.putExtra("idUsuario",idUsuario)
        intent.putExtra("actualizar","actualizar")
        startActivity(intent)
    }
    private fun eliminarProducto(producto: Producto){

        val referenciaCarpeta = storageProductoRef.child("prestodo_objetos")
        val referenciaArchivo = referenciaCarpeta.child(producto.id)

        productoRef.whereEqualTo("id", producto.id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // No se encontró ningún producto con el nombre dado
                    Toast.makeText(this, "No se encontró el producto a eliminar", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                var flag = true
                // Mostrar un diálogo de confirmación al usuario
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Eliminar producto")
                    .setMessage("¿Está seguro de que desea eliminar este producto?")
                    .setPositiveButton("Sí") { dialog, which ->

                        rentasRef.whereEqualTo("producto.id", producto.id)
                            .get()
                            .addOnSuccessListener { documents ->
                                for (document in documents) {
                                    Toast.makeText(
                                        this,
                                        "No puedes eliminar un préstamo que está siendo rentado!\nTermina el prestamo y vuelve a intentarlo." +
                                                "",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    flag = false
                                    break
                                }

                                if (flag) {
                                    referenciaArchivo.delete()
                                        .addOnSuccessListener {
                                            // Archivo eliminado exitosamente
                                        }
                                        .addOnFailureListener {
                                            // Error al eliminar archivo
                                        }
                                    // Eliminar el documento
                                    for (document in querySnapshot) {
                                        document.reference.delete().addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                "Producto eliminado correctamente",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            cargarMisPrestamos(idUsuario)
                                        }.addOnFailureListener {
                                            Toast.makeText(
                                                this,
                                                "Error al eliminar el producto",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                    }
                    .setNegativeButton("No") { dialog, which ->
                        // No hacer nada
                    }
                builder.create().show()
            }
            .addOnFailureListener { exception ->
                // Manejar errores
                Toast.makeText(this, "Error al consultar los productos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarMisPrestamos(id: String?){
        arrayMisProductos.clear()
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        swipeRefreshLayout.isRefreshing = true
        productoRef
            .whereEqualTo("idVendedor", id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val producto = document.toObject(Producto::class.java)
                    arrayMisProductos.add(producto)
                }
                val adapter = PrincipalActivity.ProductoAdapter(arrayMisProductos, this, intent.extras,null)
                val gridview: GridView = findViewById(R.id.id_gridMisPrestamos)
                gridview.adapter = adapter
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al buscar productos por nombre: $exception")
            }
    }
}