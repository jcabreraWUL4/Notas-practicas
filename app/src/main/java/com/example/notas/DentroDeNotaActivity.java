package com.example.notas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
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
            @Override
            public void handleOnBackPressed() {
                guardarYSalir();
            }
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editTextContenido = findViewById(R.id.editTextNota);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("NOTA_ID")) {
            notaId = intent.getIntExtra("NOTA_ID", -1);
        }

        if (notaId != -1) {
            cargarNota();
            getSupportActionBar().setTitle(notaTitulo);
        } else {
            getSupportActionBar().setTitle("Nueva Nota (Error)");
            Toast.makeText(this, "Error al cargar la nota", Toast.LENGTH_SHORT).show();
//            finish();
        }

        setupAutoGuardado();
    }

    private void setupAutoGuardado() {
        guardarRunnable = () -> {
            guardarNota();
        };

        editTextContenido.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                handler.removeCallbacks(guardarRunnable);
                handler.postDelayed(guardarRunnable, 1000);
            }
        });

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

        try (FileInputStream fis = openFileInput(nombreArchivo);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(isr)) {

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

            String linea;
            while ((linea = bufferedReader.readLine()) != null) {
                contenidoParaEditor.append(linea).append('\n');
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        editTextContenido.setText(contenidoParaEditor.toString().trim());
        editTextContenido.setSelection(editTextContenido.getText().length());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(notaTitulo);
            getSupportActionBar().setSubtitle(notaFecha);
        }
    }

    private void guardarNota() {
        if (notaId == -1) return; 

        // Actualizamos la fecha a la actual cada vez que guardamos
        String fechaActual = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String metadatos = this.notaTitulo + "//;;" + fechaActual + "\n";
        String contenido = editTextContenido.getText().toString();
        String contenidoCompleto = metadatos + contenido;

        String nombreArchivo = "nota_" + notaId + ".txt";

        try (FileOutputStream fos = openFileOutput(nombreArchivo, MODE_PRIVATE)) {
            fos.write(contenidoCompleto.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la nota", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarYSalir() {
        handler.removeCallbacks(guardarRunnable);
        guardarNota();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            guardarYSalir();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
