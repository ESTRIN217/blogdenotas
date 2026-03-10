package com.Jhon.myempty.blogdenotasjava;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class NoteIOHelper {

    private static final String TAG = "NoteIOHelper";
    private final ContentResolver contentResolver;
    private final Context context;
    private final Gson gson;

    public NoteIOHelper(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        // Configura Gson para un formato de salida legible
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Nota cargarNota(Uri uri) {
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            // Usa Gson para parsear el JSON directamente a un objeto Nota
            return gson.fromJson(reader, Nota.class);

        } catch (Exception e) {
            Log.e(TAG, "Error al cargar la nota desde la URI: " + uri, e);
            return null; // Devuelve null si hay un error
        }
    }

    public boolean guardarNota(Nota nota, Uri uri) {
        try (OutputStream outputStream = contentResolver.openOutputStream(uri, "w"); // "w" para truncar el archivo si existe
             OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {

            // Usa Gson para convertir el objeto Nota a JSON
            String jsonString = gson.toJson(nota);
            writer.write(jsonString);
            return true; // Éxito

        } catch (Exception e) {
            Log.e(TAG, "Error al guardar la nota en la URI: " + uri, e);
            return false; // Falla
        }
    }

    public Uri crearNuevaNota() {
        File notesDir = new File(context.getFilesDir(), "notas");
        if (!notesDir.exists() && !notesDir.mkdirs()) {
            Log.e(TAG, "No se pudo crear el directorio 'notas'");
            return null;
        }

        String nombreArchivo = "nota_" + System.currentTimeMillis() + ".json";
        File file = new File(notesDir, nombreArchivo);

        // Crea una nota vacía y la guarda para obtener una URI válida
        Nota nuevaNota = new Nota("", "", 0, ""); 
        Uri nuevaUri = Uri.fromFile(file);
        
        // Asigna la URI a la nota antes de guardarla
        nuevaNota.setUri(nuevaUri.toString());

        if (guardarNota(nuevaNota, nuevaUri)) {
            return nuevaUri;
        } else {
            // Si falla el guardado inicial, intenta eliminar el archivo si se creó
            if(file.exists()) file.delete();
            return null;
        }
    }
}
