package com.Jhon.myempty.blogdenotasjava;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DibujoActivity extends AppCompatActivity {

    private static final String TAG = "DibujoActivity";

    private LienzoView lienzoView;
    private DibujoViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialogo_dibujo);

        viewModel = new ViewModelProvider(this).get(DibujoViewModel.class);
        lienzoView = findViewById(R.id.lienzo); // ID corregido

        configurarControles();
        observarViewModel();
        configurarLienzoTouchListener();
    }

    private void configurarControles() {
        // IDs corregidos para coincidir con dialogo_dibujo.xml
        findViewById(R.id.btnDone).setOnClickListener(v -> guardarDibujo());
        findViewById(R.id.undo).setOnClickListener(v -> viewModel.deshacer());
        findViewById(R.id.redo).setOnClickListener(v -> viewModel.rehacer());
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.btnAccionBorrarLienzo).setOnClickListener(v -> viewModel.limpiarLienzo());

        // Asumiendo que tienes un botón para el modo borrador y lápiz en tu layout final.
        // Si no es así, puedes añadirlos o controlar el modo de otra forma.
        // Por ahora, lo conecto a los botones que sí existen.
        findViewById(R.id.redo).setOnClickListener(v -> viewModel.desactivarModoBorrador());

        Slider sliderGrosor = findViewById(R.id.sliderGrosor);
        sliderGrosor.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                viewModel.setGrosorPincel(value);
            }
        });

        // Aquí añadirías la lógica para tu selector de color (contenedorColores)
    }

    private void observarViewModel() {
        viewModel.trazos.observe(this, trazos -> lienzoView.invalidate());
        viewModel.trazoActual.observe(this, trazo -> lienzoView.invalidate());

        viewModel.grosorPincel.observe(this, grosor -> {
            Slider sliderGrosor = findViewById(R.id.sliderGrosor);
            if (sliderGrosor.getValue() != grosor) {
                sliderGrosor.setValue(grosor);
            }
        });

        // Observa otros LiveData (color, modoBorrador) y actualiza la UI correspondientemente
    }

    private void configurarLienzoTouchListener() {
        lienzoView.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    viewModel.iniciarTrazo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    viewModel.moverTrazo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    viewModel.finalizarTrazo();
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    private void guardarDibujo() {
        Bitmap bitmap = Bitmap.createBitmap(lienzoView.getWidth(), lienzoView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        lienzoView.draw(canvas); // Dibuja la vista en el canvas

        File adjuntosDir = new File(getFilesDir(), "adjuntos");
        if (!adjuntosDir.exists() && !adjuntosDir.mkdirs()) {
            Log.e(TAG, "Error al crear el directorio 'adjuntos'");
            Toast.makeText(this, "Error al guardar el dibujo.", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombreArchivo = "dibujo_" + System.currentTimeMillis() + ".png";
        File file = new File(adjuntosDir, nombreArchivo);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Toast.makeText(this, "Dibujo guardado", Toast.LENGTH_SHORT).show();

            Intent resultIntent = new Intent();
            resultIntent.setData(Uri.fromFile(file));
            setResult(Activity.RESULT_OK, resultIntent);
            finish();

        } catch (IOException e) {
            Log.e(TAG, "Error al escribir el archivo del dibujo", e);
            Toast.makeText(this, "Error al guardar el dibujo.", Toast.LENGTH_SHORT).show();
        }
    }
}
