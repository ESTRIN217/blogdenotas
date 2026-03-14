package com.Jhon.myempty.blogdenotasjava;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {

    private static final String TAG = "FloatingService";

    private WindowManager windowManager;
    private View floatingView;
    private EditText floatingTxtNota;
    private TextView floatingTitleText;

    private NoteIOHelper noteIOHelper;
    private Nota notaActual;
    private Uri uriDeArchivoActual;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        noteIOHelper = new NoteIOHelper(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupFloatingView();
    }

    private void setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_editor_layout, null);

        // Determina el tipo de layout param necesario basado en la versión de Android.
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                0, // Flags: sin "FLAG_NOT_FOCUSABLE" para permitir la edición.
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        windowManager.addView(floatingView, params);

        floatingTxtNota = floatingView.findViewById(R.id.floating_txt_nota);
        floatingTitleText = floatingView.findViewById(R.id.floating_title_text);

        floatingView.findViewById(R.id.btn_cerrar_flotante).setOnClickListener(v -> stopSelf());
        floatingView.findViewById(R.id.btn_guardar_flotante).setOnClickListener(v -> guardarNota());

        setupWindowDrag(floatingView.findViewById(R.id.floating_header), params);
    }

    private void setupWindowDrag(View header, final WindowManager.LayoutParams params) {
        header.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("uri_archivo")) {
            String uriString = intent.getStringExtra("uri_archivo");
            uriDeArchivoActual = Uri.parse(uriString);
            cargarContenido();
        }
        return START_NOT_STICKY; // Evita que el servicio se reinicie automáticamente.
    }

    private void cargarContenido() {
        notaActual = noteIOHelper.cargarNota(uriDeArchivoActual);
        if (notaActual == null) {
            Log.e(TAG, "Error al cargar la nota desde la URI: " + uriDeArchivoActual);
            Toast.makeText(this, "Error al cargar la nota.", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }

        floatingTitleText.setText(notaActual.getTitulo());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            floatingTxtNota.setText(Html.fromHtml(notaActual.getContenido(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            floatingTxtNota.setText(Html.fromHtml(notaActual.getContenido()));
        }
    }

    private void guardarNota() {
        if (notaActual == null || uriDeArchivoActual == null) {
            Toast.makeText(this, "No se puede guardar, no hay una nota activa.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualiza el contenido del objeto Nota.
        String contenidoActualizado;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contenidoActualizado = Html.toHtml(floatingTxtNota.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        } else {
            contenidoActualizado = Html.toHtml(floatingTxtNota.getText());
        }
        notaActual.setContenido(contenidoActualizado);

        // Guarda el objeto Nota completo usando el helper.
        boolean exito = noteIOHelper.guardarNota(notaActual, uriDeArchivoActual);

        if (exito) {
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al guardar la nota", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
