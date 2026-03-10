package com.Jhon.myempty.blogdenotasjava;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class EditorActivity extends AppCompatActivity {

    private EditorViewModel viewModel;

    private EditText txtTitulo;
    private EditText txtNota;
    private RecyclerView contenedorAdjuntos;
    private SimpleAdapter adjuntoAdapter;

    private ActivityResultLauncher<Intent> dibujoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);

        viewModel = new ViewModelProvider(this).get(EditorViewModel.class);

        inicializarVistas();
        configurarLanzadores();
        configurarAdaptadores();
        configurarListeners();

        observarViewModel();

        if (savedInstanceState == null) {
            viewModel.cargarOcrearNota(getIntent());
        }
    }

    private void inicializarVistas() {
        txtTitulo = findViewById(R.id.txtTitulo);
        txtNota = findViewById(R.id.txtNota);
        contenedorAdjuntos = findViewById(R.id.contenedorAdjuntos);
    }

    private void configurarLanzadores() {
        dibujoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri dibujoUri = result.getData().getData();
                    if (dibujoUri != null) {
                        ItemAdjunto nuevoDibujo = new ItemAdjunto(ItemAdjunto.TIPO_DIBUJO, dibujoUri.toString());
                        viewModel.agregarAdjunto(nuevoDibujo);
                    }
                }
            }
        );
    }

    private void configurarAdaptadores() {
        adjuntoAdapter = new SimpleAdapter(this);
        contenedorAdjuntos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        contenedorAdjuntos.setAdapter(adjuntoAdapter);
    }

    private void configurarListeners() {
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarYSalir());
        findViewById(R.id.btnAtras).setOnClickListener(v -> gestionarSalida());
        // Añadir aquí los listeners para la barra de herramientas inferior
    }

    private void observarViewModel() {
        viewModel.notaActual.observe(this, nota -> {
            if (nota != null) {
                mostrarNota(nota);
            } else {
                Toast.makeText(this, "Error al cargar o crear la nota.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void mostrarNota(Nota nota) {
        txtTitulo.setText(nota.getTitulo());
        txtNota.setText(Html.fromHtml(nota.getContenido() != null ? nota.getContenido() : "", Html.FROM_HTML_MODE_COMPACT));
        adjuntoAdapter.setItems(nota.getAdjuntos());
    }

    private void guardarYSalir() {
        String titulo = txtTitulo.getText().toString().trim();
        String contenido = Html.toHtml(txtNota.getEditableText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        if (titulo.isEmpty()) {
            Toast.makeText(this, "La nota debe tener un título", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exito = viewModel.guardarNota(titulo, contenido);

        if (exito) {
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("uri_archivo_guardado", viewModel.getUriDeArchivoActual().toString());
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Error al guardar la nota", Toast.LENGTH_LONG).show();
        }
    }
    
    private void gestionarSalida() {
        String titulo = txtTitulo.getText().toString().trim();
        String contenido = Html.toHtml(txtNota.getEditableText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        if (viewModel.hayCambios(titulo, contenido)) {
            // Aquí podrías mostrar un diálogo "¿Guardar cambios?"
            // Por ahora, simplemente guardamos
            guardarYSalir();
        } else {
            finish(); // Salir sin guardar si no hay cambios
        }
    }

    @Override
    public void onBackPressed() {
        gestionarSalida();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adjuntoAdapter != null) {
            adjuntoAdapter.liberarRecursos();
        }
    }
}
