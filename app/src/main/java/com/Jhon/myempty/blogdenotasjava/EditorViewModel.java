package com.Jhon.myempty.blogdenotasjava;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class EditorViewModel extends AndroidViewModel {

    private final NoteIOHelper noteIOHelper;

    private final MutableLiveData<Nota> _notaActual = new MutableLiveData<>();
    public final LiveData<Nota> notaActual = _notaActual;

    private Uri uriDeArchivoActual;
    private String contenidoOriginal;

    public EditorViewModel(@NonNull Application application) {
        super(application);
        this.noteIOHelper = new NoteIOHelper(application.getApplicationContext());
    }

    public void cargarOcrearNota(Intent intent) {
        String uriString = intent.getStringExtra("uri_archivo");

        if (uriString != null && !uriString.isEmpty()) {
            uriDeArchivoActual = Uri.parse(uriString);
            Nota nota = noteIOHelper.cargarNota(uriDeArchivoActual);
            if (nota != null) {
                _notaActual.setValue(nota);
                contenidoOriginal = nota.getContenido(); // Guardar estado original
            } else {
                // Manejar error de carga
                 _notaActual.setValue(null);
            }
        } else {
            uriDeArchivoActual = noteIOHelper.crearNuevaNota();
            if (uriDeArchivoActual != null) {
                Nota nuevaNota = new Nota();
                nuevaNota.setUri(uriDeArchivoActual.toString());
                _notaActual.setValue(nuevaNota);
                contenidoOriginal = ""; // Nota nueva, contenido original vacío
            } else {
                // Manejar error de creación
                _notaActual.setValue(null);
            }
        }
    }

    public boolean guardarNota(String titulo, String contenido) {
        Nota nota = _notaActual.getValue();
        if (nota == null || uriDeArchivoActual == null) {
            return false;
        }

        nota.setTitulo(titulo);
        nota.setContenido(contenido);
        // Los adjuntos se actualizan directamente en el objeto nota

        boolean exito = noteIOHelper.guardarNota(nota, uriDeArchivoActual);
        if (exito) {
            contenidoOriginal = contenido; // Actualizar el contenido original tras guardar
        }
        return exito;
    }

    public void agregarAdjunto(ItemAdjunto adjunto) {
        Nota nota = _notaActual.getValue();
        if (nota != null) {
            nota.addAdjunto(adjunto);
            // Forzar la actualización del LiveData para que los observadores reaccionen
            _notaActual.setValue(nota);
        }
    }
    
    public Uri getUriDeArchivoActual() {
        return uriDeArchivoActual;
    }

    public boolean hayCambios(String tituloActual, String contenidoActual) {
        Nota nota = _notaActual.getValue();
        if (nota == null) return false;
        
        // Compara el título y el contenido actuales con los originales
        return !tituloActual.equals(nota.getTitulo()) || !contenidoActual.equals(contenidoOriginal);
    }
}
