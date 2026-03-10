package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class LienzoView extends View {

    private Paint paintCuadricula;

    public LienzoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintCuadricula = new Paint();
        paintCuadricula.setColor(Color.LTGRAY);
        paintCuadricula.setStrokeWidth(1f);
        paintCuadricula.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // El fondo se establece desde la actividad/fragmento
    }

    public void dibujarCuadricula(Canvas canvas) {
        if (canvas == null) return;
        int paso = 100;
        for (int x = 0; x < canvas.getWidth(); x += paso) {
            canvas.drawLine(x, 0, x, canvas.getHeight(), paintCuadricula);
        }
        for (int y = 0; y < canvas.getHeight(); y += paso) {
            canvas.drawLine(0, y, canvas.getWidth(), y, paintCuadricula);
        }
    }

    public void dibujarTrazos(Canvas canvas, List<DibujoViewModel.Trazo> trazos) {
        if (canvas == null || trazos == null) return;
        for (DibujoViewModel.Trazo trazo : trazos) {
            canvas.drawPath(trazo.getPath(), trazo.getPaint());
        }
    }

    public void dibujarTrazoActual(Canvas canvas, Path trazo, Paint paint) {
        if (canvas == null || trazo == null || paint == null) return;
        canvas.drawPath(trazo, paint);
    }

    public void dibujarUISeleccion(Canvas canvas, RectF bounds) {
        if (canvas == null || bounds == null) return;

        Paint paintSeleccion = new Paint();
        paintSeleccion.setColor(Color.parseColor("#4285F4")); // Azul de Google
        paintSeleccion.setStyle(Paint.Style.STROKE);
        paintSeleccion.setStrokeWidth(4f);

        canvas.drawRect(bounds, paintSeleccion);

        paintSeleccion.setStyle(Paint.Style.FILL);
        float handleRadius = 15f;
        canvas.drawCircle(bounds.left, bounds.top, handleRadius, paintSeleccion);
        canvas.drawCircle(bounds.right, bounds.top, handleRadius, paintSeleccion);
        canvas.drawCircle(bounds.left, bounds.bottom, handleRadius, paintSeleccion);
        canvas.drawCircle(bounds.right, bounds.bottom, handleRadius, paintSeleccion);
    }

    public Bitmap crearBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas); // Dibuja la vista actual en el canvas del bitmap
        return bitmap;
    }
}
