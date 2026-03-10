package com.Jhon.myempty.blogdenotasjava;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";
    private final NoteIOHelper noteIOHelper;
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    private final MutableLiveData<List<Nota>> _notas = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Nota>> notas = _notas;

    private List<Nota> listaDeNotasCompleta = new ArrayList<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.noteIOHelper = new NoteIOHelper(application.getApplicationContext());
        this.sharedPreferences = application.getSharedPreferences(
            "com.Jhon.myempty.blogdenotasjava.prefs", Context.MODE_PRIVATE);
        cargarNotas();
    }

    public void cargarNotas() {
        new Thread(() -> {
            File notesDir = new File(getApplication().getFilesDir(), "notas");
            if (!notesDir.exists() || !notesDir.isDirectory()) {
                _notas.postValue(new ArrayList<>()); // Publica lista vacía si no hay directorio
                return;
            }

            File[] archivos = notesDir.listFiles();
            if (archivos == null) {
                _notas.postValue(new ArrayList<>()); // Publica lista vacía si falla la lista de archivos
                return;
            }

            List<Nota> notasCargadas = new ArrayList<>();
            for (File file : archivos) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    Uri uri = Uri.fromFile(file);
                    Nota nota = noteIOHelper.cargarNota(uri);
                    if (nota != null) {
                        notasCargadas.add(nota);
                    }
                }
            }

            listaDeNotasCompleta = new ArrayList<>(notasCargadas);
            ordenarYPublicarNotas();
        }).start();
    }

    public void filtrarNotas(String query) {
        if (query == null || query.trim().isEmpty()) {
            _notas.setValue(new ArrayList<>(listaDeNotasCompleta));
            return;
        }

        List<Nota> listaFiltrada = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Nota nota : listaDeNotasCompleta) {
            if (nota.getTitulo().toLowerCase().contains(lowerCaseQuery) ||
                nota.getContenido().toLowerCase().contains(lowerCaseQuery)) {
                listaFiltrada.add(nota);
            }
        }
        _notas.setValue(listaFiltrada);
    }

    public void cambiarCriterioOrden(int nuevoCriterio) {
        sharedPreferences.edit().putInt("criterio_orden", nuevoCriterio).apply();
        ordenarYPublicarNotas();
    }

    private void ordenarYPublicarNotas() {
        int criterio = sharedPreferences.getInt("criterio_orden", 0);
        List<Nota> notasParaOrdenar = new ArrayList<>(listaDeNotasCompleta);

        switch (criterio) {
            case 0: // Modificación descendente
                Collections.sort(notasParaOrdenar, Comparator.comparingLong((Nota nota) -> new File(Uri.parse(nota.getUri()).getPath()).lastModified()).reversed());
                break;
            case 1: // Modificación ascendente
                Collections.sort(notasParaOrdenar, Comparator.comparingLong((Nota nota) -> new File(Uri.parse(nota.getUri()).getPath()).lastModified()));
                break;
            case 2: // Orden personalizado
                String ordenGuardado = sharedPreferences.getString("orden_personalizado", "");
                if (!ordenGuardado.isEmpty()) {
                    List<String> ranking = Arrays.asList(ordenGuardado.split(","));
                    Map<String, Nota> notaMap = new HashMap<>();
                    for (Nota n : notasParaOrdenar) {
                        notaMap.put(new File(Uri.parse(n.getUri()).getPath()).getName().replace(".json", ""), n);
                    }
                    
                    List<Nota> notasOrdenadas = new ArrayList<>();
                    for (String key : ranking) {
                        if (notaMap.containsKey(key)) {
                            notasOrdenadas.add(notaMap.get(key));
                            notaMap.remove(key);
                        }
                    }
                    notasOrdenadas.addAll(notaMap.values());
                    notasParaOrdenar = notasOrdenadas;
                }
                break;
        }
        _notas.postValue(notasParaOrdenar);
    }
    
    public void guardarOrdenPersonalizado(List<Nota> notas) {
        StringBuilder sb = new StringBuilder();
        for (Nota n : notas) {
            try {
                String fileName = new File(Uri.parse(n.getUri()).getPath()).getName().replace(".json", "");
                sb.append(fileName).append(",");
            } catch (Exception e) {
                Log.e(TAG, "Error al guardar el orden personalizado para la URI: " + n.getUri(), e);
            }
        }
        if (sb.length() > 0) {
            sharedPreferences.edit().putString("orden_personalizado", sb.toString()).apply();
        }
    }
}
