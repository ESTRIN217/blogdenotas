package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private NotaAdapter adaptador;

    private RecyclerView recyclerNotas;
    private FloatingActionButton btnNuevaNota;
    private EditText buscar;
    private MaterialButton btnToggleView, btnOpenMenu, btnSort;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private SharedPreferences sharedPreferences;
    private boolean esModoCuadricula = false;

    private static final String KEY_VISTA_GRID = "vista_en_cuadricula";
    private static final String PREFS_NAME = "com.Jhon.myempty.blogdenotasjava.prefs";

    private final ActivityResultLauncher<Intent> editorLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) {
                viewModel.cargarNotas();
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa el ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        inicializarVistas();
        configurarRecyclerView();
        configurarListeners();
        observarViewModel();

        cargarPreferencias();
        aplicarModoVista();
    }

    private void inicializarVistas() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view_start);
        recyclerNotas = findViewById(R.id.recyclerNotas);
        btnNuevaNota = findViewById(R.id.btnNuevaNota);
        buscar = findViewById(R.id.buscar);
        btnToggleView = findViewById(R.id.btnToggleView);
        btnOpenMenu = findViewById(R.id.btnOpenMenu);
        btnSort = findViewById(R.id.btnSort);
    }

    private void configurarRecyclerView() {
        adaptador = new NotaAdapter(new ArrayList<>(),
            nota -> {
                if (adaptador.haySeleccion()) {
                    adaptador.toggleSeleccion(nota, 0);
                } else {
                    abrirEditor(nota.getUri());
                }
            },
            (view, nota, position) -> adaptador.toggleSeleccion(nota, position)
        );
        recyclerNotas.setAdapter(adaptador);
        configurarItemTouchHelper();
    }

    private void configurarListeners() {
        btnNuevaNota.setOnClickListener(v -> abrirEditor(null));
        btnOpenMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnToggleView.setOnClickListener(v -> toggleModoVista());
        btnSort.setOnClickListener(v -> mostrarDialogoOrden());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_sobre) {
                startActivity(new Intent(this, SobreActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        buscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filtrarNotas(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observarViewModel() {
        viewModel.notas.observe(this, notas -> {
            if (notas != null) {
                adaptador.actualizarLista(notas);
            }
        });
    }

    private void cargarPreferencias() {
        esModoCuadricula = sharedPreferences.getBoolean(KEY_VISTA_GRID, false);
    }

    private void abrirEditor(@Nullable String uriString) {
        Intent intent = new Intent(this, EditorActivity.class);
        if (uriString != null && !uriString.isEmpty()) {
            intent.putExtra("uri_archivo", uriString);
        }
        editorLauncher.launch(intent);
    }

    private void toggleModoVista() {
        esModoCuadricula = !esModoCuadricula;
        sharedPreferences.edit().putBoolean(KEY_VISTA_GRID, esModoCuadricula).apply();
        aplicarModoVista();
    }

    private void aplicarModoVista() {
        if (esModoCuadricula) {
            recyclerNotas.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            btnToggleView.setIconResource(R.drawable.outline_view_agenda);
        } else {
            recyclerNotas.setLayoutManager(new LinearLayoutManager(this));
            btnToggleView.setIconResource(R.drawable.grid_view);
        }
    }

    private void mostrarDialogoOrden() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.button_sheet_orden, null);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.fecha_de_modificacion).setOnClickListener(v -> {
            viewModel.cambiarCriterioOrden(0);
            bottomSheetDialog.dismiss();
        });
        view.findViewById(R.id.fecha_de_creacion).setOnClickListener(v -> {
            viewModel.cambiarCriterioOrden(1);
            bottomSheetDialog.dismiss();
        });
        view.findViewById(R.id.personalizado).setOnClickListener(v -> {
            viewModel.cambiarCriterioOrden(2);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void configurarItemTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getBindingAdapterPosition();
                int toPos = target.getBindingAdapterPosition();
                adaptador.moverNota(fromPos, toPos);
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (sharedPreferences.getInt("criterio_orden", 0) == 2) {
                    viewModel.guardarOrdenPersonalizado(adaptador.getListaNotas());
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerNotas);
    }
}
