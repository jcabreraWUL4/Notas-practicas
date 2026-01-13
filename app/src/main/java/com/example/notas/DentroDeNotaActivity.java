package com.example.notas;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DentroDeNotaActivity extends AppCompatActivity {

    private EditText editTextContenido;
    private int notaId = -1;
    private String notaTitulo = "";
    private String notaFecha = "";

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable guardarRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dentronota_activity);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            // Al pulsar atras
            @Override
            public void handleOnBackPressed() {
                if (editTextContenido.hasFocus()) {
                    editTextContenido.clearFocus();
                } else {
                    guardarYSalir();
                }
            }
        });

        // Anadir boton de atras
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editTextContenido = findViewById(R.id.editTextNota);

        // Coger el id de la nota
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("NOTA_ID")) {
            notaId = intent.getIntExtra("NOTA_ID", -1);
        }

        // Si hay nota, cargarla
        if (notaId != -1) {
            cargarNota();
            getSupportActionBar().setTitle(notaTitulo);
            getSupportActionBar().setSubtitle(notaFecha);
        // Si no hay nota, salir
        } else {
            getSupportActionBar().setTitle("Nueva Nota (Error)");
            Toast.makeText(this, "Error al cargar la nota", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Activar el autoguardado
        setupAutoGuardado();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Al tocar
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Crear una vista con el foco actual
            View v = getCurrentFocus();

            // Si la vista actual es un EditText
            if (v instanceof EditText) {
                // Crear rectangulo
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                // Si no se toca dentro del rectangulo, perder el foco
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void setupAutoGuardado() {
        // Runnable que se ejecutara cada vez que se modifique el texto
        guardarRunnable = () -> {
            guardarNota();
        };

        // Cuando se modifica el texto, se ejecuta el Runnable
        editTextContenido.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                handler.removeCallbacks(guardarRunnable);

                // Delay de 300 milisegundos antes de ejecutar el Runnable, para tener
                // buen rendimiento y que no guarde constantemente a menos que deje
                // de escribir
                handler.postDelayed(guardarRunnable, 400);
            }
        });

        // Si pierde el focus, tambien se guarda
        editTextContenido.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                handler.removeCallbacks(guardarRunnable);
                guardarNota();
            }
        });
    }

    private void cargarNota() {
        String nombreArchivo = "nota_" + notaId + ".txt";
        StringBuilder contenidoParaEditor = new StringBuilder();

        // Leer la nota y cargarla en el editor
        try (FileInputStream fis = openFileInput(nombreArchivo);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(isr)) {

            // Leer la primera linea para obtener el titulo y la fecha
            // Separados por //;;
            String primeraLinea = bufferedReader.readLine();

            if (primeraLinea != null && primeraLinea.contains("//;;")) {
                String[] partes = primeraLinea.split("//;;");
                this.notaTitulo = partes[0];
                this.notaFecha = partes.length > 1 ? partes[1] : "";
            } else {
                if (primeraLinea != null) {
                    contenidoParaEditor.append(primeraLinea).append('\n');
                }
            }

            // Poner el resto, que no es titulo ni fecha, en el editor
            String linea;
            while ((linea = bufferedReader.readLine()) != null) {
                contenidoParaEditor.append(linea).append('\n');
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Colocar el contenido en el editor
        editTextContenido.setText(contenidoParaEditor.toString().trim());
        editTextContenido.setSelection(editTextContenido.getText().length());

        // Poner en la actionBar el titulo y la fecha
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(notaTitulo);
            getSupportActionBar().setSubtitle(notaFecha);
        }
    }

    private void guardarNota() {
        // Si no hay nota, no hacer nada
        if (notaId == -1) return; 

        // Actualizar la fecha a la actual cada vez que se guarda
        String fechaActual = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String metadatos = this.notaTitulo + "//;;" + fechaActual + "\n";
        String contenido = editTextContenido.getText().toString();
        String contenidoCompleto = metadatos + contenido;

        String nombreArchivo = "nota_" + notaId + ".txt";

        // Guardar la nota en el archivo
        try (FileOutputStream fos = openFileOutput(nombreArchivo, MODE_PRIVATE)) {
            fos.write(contenidoCompleto.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la nota", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarYSalir() {
        // Guardar la nota antes de salir
        handler.removeCallbacks(guardarRunnable);
        guardarNota();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Al pulsar el boton home, guardar y salir
            guardarYSalir();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
