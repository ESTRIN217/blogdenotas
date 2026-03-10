package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class LienzoView extends View {

    // --- Constantes para Herramientas ---
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TOOL_PEN, TOOL_MARKER, TOOL_HIGHLIGHTER, TOOL_ERASER, TOOL_SELECTION})
    public @interface ToolMode {}
    public static final String TOOL_PEN = "PEN";
    public static final String TOOL_MARKER = "MARKER";
    public static final String TOOL_HIGHLIGHTER = "RESALTADOR";
    public static final String TOOL_ERASER = "ERASER";
    public static final String TOOL_SELECTION = "SELECTION";

    // --- Constantes para la UI de Selección ---
    private static final int HANDLE_RADIUS = 25;
    private static final int ROTATION_HANDLE_OFFSET = -60;
    private static final int HANDLE_TOUCH_TOLERANCE = 50;
    private static final int INVALID_HANDLE_ID = -1;
    private static final int HANDLE_TOP_LEFT = 0;
    private static final int HANDLE_TOP_RIGHT = 1;
    private static final int HANDLE_BOTTOM_LEFT = 2;
    private static final int HANDLE_BOTTOM_RIGHT = 3;
    private static final int HANDLE_ROTATION = 8;
    private static final int HANDLE_MOVE = 9;


    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Path mCurrentPath;
    private Paint mCurrentPaint;

    private final ArrayList<DibujoObjeto> mObjetos = new ArrayList<>();
    private DibujoObjeto objetoSeleccionado = null;

    private final ArrayList<DibujoObjeto> mUndoneObjetos = new ArrayList<>();

    private int mColorActual = Color.BLACK;
    private float mGrosorActual = 10f;
    @ToolMode private String herramientaActual = TOOL_PEN;

    private int handleTocado = INVALID_HANDLE_ID;
    private float lastTouchX;
    private float lastTouchY;
    private boolean mMostrarCuadricula = false;

    public LienzoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupNewPaint();
    }

    private void setupNewPaint() {
        mCurrentPaint = new Paint();
        mCurrentPaint.setAntiAlias(true);
        mCurrentPaint.setDither(true);
        mCurrentPaint.setStyle(Paint.Style.STROKE);
        mCurrentPaint.setStrokeJoin(Paint.Join.ROUND);
        mCurrentPaint.setStrokeCap(Paint.Cap.ROUND);
        mCurrentPaint.setColor(mColorActual);
        mCurrentPaint.setStrokeWidth(mGrosorActual);

        if (herramientaActual.equals(TOOL_HIGHLIGHTER)) {
            mCurrentPaint.setAlpha(80);
            mCurrentPaint.setStrokeCap(Paint.Cap.SQUARE);
        } else if (herramientaActual.equals(TOOL_ERASER)) {
            mCurrentPaint.setColor(Color.WHITE); // O usar PorterDuff.Mode.CLEAR
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mBitmap == null && w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawColor(Color.WHITE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

        if (mMostrarCuadricula) {
            drawGrid(canvas);
        }

        // Dibuja el trazo actual directamente en el canvas de la vista para fluidez
        if (mCurrentPath != null && mCurrentPaint != null) {
            canvas.drawPath(mCurrentPath, mCurrentPaint);
        }

        if (objetoSeleccionado != null) {
            // Dibuja el objeto transformado directamente sobre el canvas de la vista
            // para una vista previa fluida durante la transformación.
            canvas.drawPath(objetoSeleccionado.getTransformedPath(), objetoSeleccionado.paint);
            drawSelectionUI(canvas);
        }
    }
    
    private void drawGrid(Canvas canvas) {
        Paint pGrid = new Paint();
        pGrid.setColor(Color.LTGRAY);
        pGrid.setStrokeWidth(2f);
        pGrid.setAlpha(100);
        int paso = 100;
        for (int i = 0; i < getWidth(); i += paso) canvas.drawLine(i, 0, i, getHeight(), pGrid);
        for (int j = 0; j < getHeight(); j += paso) canvas.drawLine(0, j, getWidth(), j, pGrid);
    }

    private void drawSelectionUI(Canvas canvas) {
        RectF r = objetoSeleccionado.getBounds();
        Paint pCuadro = new Paint();
        pCuadro.setColor(Color.parseColor("#4285F4")); // Un azul estándar de Google
        pCuadro.setStyle(Paint.Style.STROKE);
        pCuadro.setStrokeWidth(4f);
        canvas.drawRect(r, pCuadro);

        pCuadro.setStyle(Paint.Style.FILL);
        // Manjeadores de escala
        canvas.drawCircle(r.left, r.top, HANDLE_RADIUS, pCuadro);
        canvas.drawCircle(r.right, r.top, HANDLE_RADIUS, pCuadro);
        canvas.drawCircle(r.left, r.bottom, HANDLE_RADIUS, pCuadro);
        canvas.drawCircle(r.right, r.bottom, HANDLE_RADIUS, pCuadro);

        // Manjeador de rotación
        float rotationHandleX = r.centerX();
        float rotationHandleY = r.top + ROTATION_HANDLE_OFFSET;
        pCuadro.setStyle(Paint.Style.STROKE);
        canvas.drawLine(r.centerX(), r.top, rotationHandleX, rotationHandleY, pCuadro);
        pCuadro.setStyle(Paint.Style.FILL);
        canvas.drawCircle(rotationHandleX, rotationHandleY, HANDLE_RADIUS, pCuadro);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (herramientaActual.equals(TOOL_SELECTION)) {
            handleSelectionMode(event, x, y);
        } else {
            handleDrawingMode(event, x, y);
        }

        invalidate();
        return true;
    }

    private void handleDrawingMode(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentPath = new Path();
                setupNewPaint(); // Configura el pincel para el nuevo trazo
                mCurrentPath.moveTo(x, y);
                mUndoneObjetos.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrentPath != null) {
                    mCurrentPath.lineTo(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mCurrentPath != null) {
                    DibujoObjeto nuevoObj = new DibujoObjeto(mCurrentPath, mCurrentPaint);
                    mObjetos.add(nuevoObj);
                    // Dibuja el trazo final en el bitmap persistente
                    mCanvas.drawPath(nuevoObj.pathOriginal, nuevoObj.paint);
                    mCurrentPath = null;
                }
                break;
        }
    }

    private void handleSelectionMode(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                handleTocado = detectarHandle(x, y);
                if (handleTocado == INVALID_HANDLE_ID) {
                    detectarSeleccion(x, y);
                    if (objetoSeleccionado != null) {
                        handleTocado = HANDLE_MOVE;
                    }
                }
                commitObjectTransformations(); // Commitea la transformación del objeto anterior antes de seleccionar uno nuevo
                break;
            case MotionEvent.ACTION_MOVE:
                if (objetoSeleccionado != null && handleTocado != INVALID_HANDLE_ID) {
                    aplicarTransformacion(x, y);
                    // invalidate() en onTouchEvent se encarga de redibujar la vista previa
                }
                break;
            case MotionEvent.ACTION_UP:
                if (objetoSeleccionado != null) {
                    commitObjectTransformations();
                }
                handleTocado = INVALID_HANDLE_ID;
                break;
        }
    }
    
    private void commitObjectTransformations() {
        if (objetoSeleccionado != null) {
            // Combina la matriz de transformación con el path original
            objetoSeleccionado.pathOriginal.transform(objetoSeleccionado.matrix);
            // Resetea la matriz para futuras transformaciones
            objetoSeleccionado.matrix.reset();
            // Redibuja todo para reflejar el estado final en el bitmap
            redrawAllObjectsToBitmap();
        }
    }

    private int detectarHandle(float x, float y) {
        if (objetoSeleccionado == null) return INVALID_HANDLE_ID;
        
        RectF r = objetoSeleccionado.getBounds();
        float rotationHandleX = r.centerX();
        float rotationHandleY = r.top + ROTATION_HANDLE_OFFSET;

        if (dist(x, y, rotationHandleX, rotationHandleY) < HANDLE_TOUCH_TOLERANCE) return HANDLE_ROTATION;
        if (dist(x, y, r.left, r.top) < HANDLE_TOUCH_TOLERANCE) return HANDLE_TOP_LEFT;
        if (dist(x, y, r.right, r.top) < HANDLE_TOUCH_TOLERANCE) return HANDLE_TOP_RIGHT;
        if (dist(x, y, r.left, r.bottom) < HANDLE_TOUCH_TOLERANCE) return HANDLE_BOTTOM_LEFT;
        if (dist(x, y, r.right, r.bottom) < HANDLE_TOUCH_TOLERANCE) return HANDLE_BOTTOM_RIGHT;
        if (r.contains(x, y)) return HANDLE_MOVE;

        return INVALID_HANDLE_ID;
    }

    private void detectarSeleccion(float x, float y) {
        deseleccionarTodo();
        for (int i = mObjetos.size() - 1; i >= 0; i--) {
            DibujoObjeto obj = mObjetos.get(i);
            if (obj.getBounds().contains(x, y)) {
                objetoSeleccionado = obj;
                // Movemos el objeto seleccionado al final de la lista para que se dibuje encima
                mObjetos.remove(i);
                mObjetos.add(obj);
                break;
            }
        }
    }
    
    public void deseleccionarTodo() {
        if (objetoSeleccionado != null) {
            commitObjectTransformations();
            objetoSeleccionado = null;
            invalidate();
        }
    }


    private void aplicarTransformacion(float x, float y) {
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;
        
        RectF r = objetoSeleccionado.getBounds();
        float cx = r.centerX();
        float cy = r.centerY();

        switch (handleTocado) {
            case HANDLE_MOVE:
                objetoSeleccionado.matrix.postTranslate(dx, dy);
                break;
            case HANDLE_ROTATION:
                float anguloActual = (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
                float anguloAnt = (float) Math.toDegrees(Math.atan2(lastTouchY - cy, lastTouchX - cx));
                objetoSeleccionado.matrix.postRotate(anguloActual - anguloAnt, cx, cy);
                break;
            case HANDLE_TOP_LEFT:
            case HANDLE_TOP_RIGHT:
            case HANDLE_BOTTOM_LEFT:
            case HANDLE_BOTTOM_RIGHT:
                float escala = dist(x, y, cx, cy) / dist(lastTouchX, lastTouchY, cx, cy);
                if (dist(x, y, cx, cy) > 30) { // Evita encoger a cero
                    objetoSeleccionado.matrix.postScale(escala, escala, cx, cy);
                }
                break;
        }
        
        lastTouchX = x;
        lastTouchY = y;
    }

    private void redrawAllObjectsToBitmap() {
        if (mBitmap != null && mCanvas != null) {
            mBitmap.eraseColor(Color.WHITE); // Limpia el bitmap
            // Vuelve a dibujar todos los objetos en su estado final
            for (DibujoObjeto obj : mObjetos) {
                mCanvas.drawPath(obj.pathOriginal, obj.paint);
            }
        }
        invalidate();
    }
    
    // --- Métodos Públicos de Control ---

    public Bitmap getBitmap() {
        // Asegurarse de que cualquier selección se aplique antes de exportar
        if(objetoSeleccionado != null) {
            commitObjectTransformations();
            objetoSeleccionado = null;
        }
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        if (mCanvas != null && bitmap != null) {
            // Asegura que el bitmap se ajuste al lienzo actual
            Bitmap escalado = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), false);
            mCanvas.drawBitmap(escalado, 0, 0, null);
            // Aquí deberías idealmente importar los paths del bitmap, pero para simplificar, lo tratamos como un fondo.
            mObjetos.clear();
            mUndoneObjetos.clear();
            invalidate();
        }
    }

    public void deshacer() {
        if (!mObjetos.isEmpty()) {
            deseleccionarTodo();
            mUndoneObjetos.add(mObjetos.remove(mObjetos.size() - 1));
            redrawAllObjectsToBitmap();
        }
    }

    public void rehacer() {
        if (!mUndoneObjetos.isEmpty()) {
            deseleccionarTodo();
            mObjetos.add(mUndoneObjetos.remove(mUndoneObjetos.size() - 1));
            redrawAllObjectsToBitmap();
        }
    }

    public void nuevoDibujo() {
        mObjetos.clear();
        mUndoneObjetos.clear();
        objetoSeleccionado = null;
        if (mCanvas != null) {
            mCanvas.drawColor(Color.WHITE);
        }
        invalidate();
    }

    public void setModo(@ToolMode String modo) {
        if (!this.herramientaActual.equals(TOOL_SELECTION) && modo.equals(TOOL_SELECTION)) {
             // Al entrar en modo selección, commiteamos el último trazo.
            commitObjectTransformations();
        }
        if (this.herramientaActual.equals(TOOL_SELECTION) && !modo.equals(TOOL_SELECTION)) {
            deseleccionarTodo();
        }
        
        this.herramientaActual = modo;
        
        switch (modo) {
            case TOOL_PEN: mGrosorActual = 10f; break;
            case TOOL_MARKER: mGrosorActual = 25f; break;
            case TOOL_HIGHLIGHTER: mGrosorActual = 50f; break;
            case TOOL_ERASER: mGrosorActual = 60f; break;
            default: break;
        }
        setupNewPaint();
    }
    
    public void setNuevoColor(int color) {
        this.mColorActual = color;
        if (objetoSeleccionado != null) {
            objetoSeleccionado.paint.setColor(color);
            // La actualización se verá en el próximo invalidate()
        } else {
             setupNewPaint();
        }
        invalidate();
    }
    
    public float getGrosorActual() { return mGrosorActual; }
    public void setGrosor(float nuevoGrosor) { this.mGrosorActual = nuevoGrosor; }
    public boolean toggleCuadricula() { mMostrarCuadricula = !mMostrarCuadricula; invalidate(); return mMostrarCuadricula; }
    public void modoBorrador() { setModo(TOOL_ERASER); }
    public void modoLapiz() { setModo(TOOL_PEN); }
    public void activarPluma() { setModo(TOOL_PEN); }
    public void activarMarcador() { setModo(TOOL_MARKER); }
    public void activarResaltador() { setModo(TOOL_HIGHLIGHTER); }
    public void activarBorrador() { setModo(TOOL_ERASER); }
    public void activarSeleccion() { setModo(TOOL_SELECTION); }


    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    // --- Clase Interna para Objetos de Dibujo ---
    private class DibujoObjeto {
        Path pathOriginal;
        Paint paint;
        Matrix matrix = new Matrix();
        private final RectF bounds = new RectF();
        private final Path tempPath = new Path(); // Path temporal para transformaciones

        DibujoObjeto(Path p, Paint pt) {
            this.pathOriginal = new Path(p);
            this.paint = new Paint(pt);
        }

        Path getTransformedPath() {
            tempPath.set(pathOriginal);
            tempPath.transform(matrix);
            return tempPath;
        }

        RectF getBounds() {
            getTransformedPath().computeBounds(bounds, true);
            return bounds;
        }
    }
}