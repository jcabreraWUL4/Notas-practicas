package com.example.notas;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements NotasAdapter.OnNotaListener {

    private EditText editTextCrearNota;
    private ImageButton buttonCrearNota;
    private RecyclerView recyclerView;

    private ArrayList<Nota> notas;
    private NotasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        recyclerView = findViewById(R.id.recyclerViewNotas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        editTextCrearNota = findViewById(R.id.editTextCrearNota);
        buttonCrearNota = findViewById(R.id.buttonCrearNota);

        notas = new ArrayList<>();
        adapter = new NotasAdapter(notas, this);
        recyclerView.setAdapter(adapter);

        buttonCrearNota.setOnClickListener(v -> crearNuevaNota());

        editTextCrearNota.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                crearNuevaNota();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarNotasExistentes();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void crearNuevaNota() {
        String titulo = editTextCrearNota.getText().toString().trim();
        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        int nuevaNotaId = generarIdUnico();
        String fechaActual = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String metadatos = titulo + "//;;" + fechaActual + "\n";
        try (FileOutputStream fos = openFileOutput("nota_" + nuevaNotaId + ".txt", MODE_PRIVATE)) {
            fos.write(metadatos.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error mientras se creaba la nota", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, DentroDeNotaActivity.class);
        intent.putExtra("NOTA_ID", nuevaNotaId);
        startActivity(intent);
        editTextCrearNota.setText("");
    }

    private void cargarNotasExistentes() {
        notas.clear();
        File[] archivos = getFilesDir().listFiles();
        if (archivos == null) return;

        Arrays.sort(archivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        for (File archivo : archivos) {
            if (archivo.getName().startsWith("nota_") && archivo.getName().endsWith(".txt")) {
                try {
                    int id = Integer.parseInt(archivo.getName().substring(5, archivo.getName().lastIndexOf(".")));
                    String primeraLinea = leerPrimeraLinea(archivo.getName());
                    if (primeraLinea != null && primeraLinea.contains("//;;")) {
                        String[] partes = primeraLinea.split("//;;");
                        notas.add(new Nota(id, partes[0].trim(), partes.length > 1 ? partes[1].trim() : ""));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClickVerNota(int notaId) {
        Intent intent = new Intent(this, DentroDeNotaActivity.class);
        intent.putExtra("NOTA_ID", notaId);
        //Toast.makeText(this, "Nota seleccionada: " + notaId, Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    @Override
    public boolean onContextEliminarNota(int notaId) {
        eliminarNota(notaId);

        return true;
    }

    @Override
    public boolean onContextRenombrarNota(int notaId) {
        String nombreArchivo = "nota_" + notaId + ".txt";
        String contenidoCompleto = leerContenidoCompleto(nombreArchivo);
        if (contenidoCompleto == null) return false;

        String[] lineas = contenidoCompleto.split("\n", 2);
        String cuerpoNota = (lineas.length > 1) ? lineas[1] : "";
        String titulo = lineas[0].split("//;;")[0].trim();

        editTextCrearNota.setText(titulo);
        editTextCrearNota.setHint("Renombra la nota");
        editTextCrearNota.requestFocus();

        editTextCrearNota.setSelection(editTextCrearNota.getText().length());
        buttonCrearNota.setColorFilter(200);

        editTextCrearNota.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                renombrarNota(titulo, cuerpoNota, nombreArchivo);
                return true;
            }
            return false;
        });

        buttonCrearNota.setOnClickListener(v -> {
            renombrarNota(titulo, cuerpoNota, nombreArchivo);
        });
        return true;
    }

    /*@Override
    public void onTituloChanged(int notaId, String nuevoTitulo) {
        String nombreArchivo = "nota_" + notaId + ".txt";
        String contenidoCompleto = leerContenidoCompleto(nombreArchivo);
        if (contenidoCompleto == null) return;

        String[] lineas = contenidoCompleto.split("\n", 2);
        String cuerpoNota = (lineas.length > 1) ? lineas[1] : "";
        
        // Actualizamos la fecha y reconstruimos los metadatos
        String fechaActual = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String nuevosMetadatos = nuevoTitulo.trim() + "//;;" + fechaActual + "\n";
        String nuevoContenidoCompleto = nuevosMetadatos + cuerpoNota;

        try (FileOutputStream fos = openFileOutput(nombreArchivo, MODE_PRIVATE)) {
            fos.write(nuevoContenidoCompleto.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }*/

    private String leerPrimeraLinea(String nombreArchivo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(nombreArchivo)))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private String leerContenidoCompleto(String nombreArchivo) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(nombreArchivo)))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                sb.append(linea).append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return sb.toString();
    }

    private int generarIdUnico() {
        int id;
        boolean esUnico;
        do {
            id = new Random().nextInt(Integer.MAX_VALUE);
            esUnico = true;
            for (File file : getFilesDir().listFiles()) {
                if (file.getName().equals("nota_" + id + ".txt")) {
                    esUnico = false;
                    break;
                }
            }
        } while (!esUnico);
        return id;
    }

    private boolean eliminarNota(int notaId) {
        String nombreArchivo = "nota_" + notaId + ".txt";
        File archivo = new File(getFilesDir(), nombreArchivo);
        if (archivo.exists()) {
            archivo.delete();
            ArrayList<Nota> aEliminar = new ArrayList<>();
            for (Nota nota : notas) {
                if (nota.getId() == notaId) {
                    aEliminar.add(nota);
                }
            }
            notas.removeAll(aEliminar);

            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "La nota no existe en el dispositivo", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    private boolean renombrarNota(String titulo, String cuerpoNota, String nombreArchivo) {
        String nuevoTitulo = editTextCrearNota.getText().toString().trim();
        if (!nuevoTitulo.equals(titulo)) {
            String fechaActual = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            String nuevosMetadatos = nuevoTitulo + "//;;" + fechaActual + "\n";
            String nuevoContenidoCompleto = nuevosMetadatos + cuerpoNota;

            try (FileOutputStream fos = openFileOutput(nombreArchivo, MODE_PRIVATE)) {
                fos.write(nuevoContenidoCompleto.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            cargarNotasExistentes();
            editTextCrearNota.setText("");
            buttonCrearNota.setColorFilter(null);
            editTextCrearNota.setHint("Crea una nota");
            buttonCrearNota.setOnClickListener(v1 -> {
                crearNuevaNota();
            });
            editTextCrearNota.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    crearNuevaNota();
                    return true;
                }
                return false;
            });
        }
        return true;
    }
}
