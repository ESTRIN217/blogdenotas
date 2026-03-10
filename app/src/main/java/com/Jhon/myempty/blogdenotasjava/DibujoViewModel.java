package com.Jhon.myempty.blogdenotasjava;

import android.app.Application;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class DibujoViewModel extends AndroidViewModel {

    // Estado del lienzo
    private final MutableLiveData<List<Trazo>> _trazos = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Trazo>> trazos = _trazos;

    private final ArrayList<Trazo> historialDeshacer = new ArrayList<>();

    // Estado del trazo actual (para dibujar en tiempo real)
    private final MutableLiveData<Path> _trazoActual = new MutableLiveData<>();
    public final LiveData<Path> trazoActual = _trazoActual;

    private Paint paintActual;

    // Estado de las herramientas
    private final MutableLiveData<Integer> _colorPincel = new MutableLiveData<>(Color.BLACK);
    public final LiveData<Integer> colorPincel = _colorPincel;

    private final MutableLiveData<Float> _grosorPincel = new MutableLiveData<>(12f);
    public final LiveData<Float> grosorPincel = _grosorPincel;

    private final MutableLiveData<Boolean> _esModoBorrador = new MutableLiveData<>(false);
    public final LiveData<Boolean> esModoBorrador = _esModoBorrador;

    public DibujoViewModel(@NonNull Application application) {
        super(application);
        configurarPincelInicial();
    }

    private void configurarPincelInicial() {
        paintActual = new Paint();
        paintActual.setAntiAlias(true);
        paintActual.setStyle(Paint.Style.STROKE);
        paintActual.setStrokeJoin(Paint.Join.ROUND);
        paintActual.setStrokeCap(Paint.Cap.ROUND);
        paintActual.setColor(_colorPincel.getValue());
        paintActual.setStrokeWidth(_grosorPincel.getValue());
    }

    // --- Acciones del Usuario ---

    public void iniciarTrazo(float x, float y) {
        historialDeshacer.clear();
        Path nuevoPath = new Path();
        nuevoPath.moveTo(x, y);
        _trazoActual.setValue(nuevoPath);
    }

    public void moverTrazo(float x, float y) {
        Path path = _trazoActual.getValue();
        if (path != null) {
            path.lineTo(x, y);
            _trazoActual.setValue(path);
        }
    }

    public void finalizarTrazo() {
        Path path = _trazoActual.getValue();
        if (path != null) {
            List<Trazo> listaTrazos = new ArrayList<>(_trazos.getValue());
            listaTrazos.add(new Trazo(path, new Paint(paintActual)));
            _trazos.setValue(listaTrazos);
        }
        _trazoActual.setValue(null); // Limpiar el trazo actual
    }

    public void deshacer() {
        List<Trazo> listaTrazos = _trazos.getValue();
        if (listaTrazos != null && !listaTrazos.isEmpty()) {
            Trazo ultimoTrazo = listaTrazos.remove(listaTrazos.size() - 1);
            historialDeshacer.add(ultimoTrazo);
            _trazos.setValue(new ArrayList<>(listaTrazos));
        }
    }

    public void rehacer() {
        if (!historialDeshacer.isEmpty()) {
            Trazo trazoRestaurado = historialDeshacer.remove(historialDeshacer.size() - 1);
            List<Trazo> listaTrazos = new ArrayList<>(_trazos.getValue());
            listaTrazos.add(trazoRestaurado);
            _trazos.setValue(listaTrazos);
        }
    }
    
    public void limpiarLienzo() {
        _trazos.setValue(new ArrayList<>());
        historialDeshacer.clear();
    }

    public void setColorPincel(int color) {
        _colorPincel.setValue(color);
        paintActual.setColor(color);
        _esModoBorrador.setValue(false);
    }

    public void setGrosorPincel(float grosor) {
        _grosorPincel.setValue(grosor);
        paintActual.setStrokeWidth(grosor);
    }

    public void activarModoBorrador() {
        paintActual.setColor(Color.WHITE); // Asumiendo fondo blanco
        _esModoBorrador.setValue(true);
    }

    public void desactivarModoBorrador() {
        paintActual.setColor(_colorPincel.getValue());
        _esModoBorrador.setValue(false);
    }

    // Clase interna para representar un trazo
    public static class Trazo {
        private final Path path;
        private final Paint paint;

        public Trazo(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }

        public Path getPath() {
            return path;
        }

        public Paint getPaint() {
            return paint;
        }
    }
}
