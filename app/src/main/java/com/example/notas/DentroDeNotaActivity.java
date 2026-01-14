package com.example.notas;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DentroDeNotaActivity extends AppCompatActivity {

    private EditText editTextContenido;
    private int notaId = -1;
    private String notaTitulo = "";
    private String notaFecha = "";

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable guardarRunnable;
    private SpeechRecognizer speechRecognizer;
    private FloatingActionButton botonVoz;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dentronota_activity);

        botonVoz = findViewById(R.id.fabVoz);
        botonVoz.setOnClickListener(v -> {
            iniciarReconocimientoDeVoz();
        });

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

                // Delay de 400 milisegundos antes de ejecutar el Runnable
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

            // Poner el resto en el editor
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
        if (notaId == -1) return;

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

    private void iniciarReconocimientoDeVoz() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconocimiento de voz no disponible", Toast.LENGTH_LONG).show();
            return;
        }

        // Inicialización perezosa: solo creamos el recognizer si no existe
        if (speechRecognizer == null) {
            iniciarSpeechRecognizer();
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        speechRecognizer.startListening(recognizerIntent);

        editTextContenido.requestFocus();
        editTextContenido.setSelection(editTextContenido.getText().length());

        botonVoz.setEnabled(false);
        botonVoz.animate().scaleX(0.8f).scaleY(0.8f);
    }

    private void iniciarSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            private String textoPrevio = "";

            @Override public void onReadyForSpeech(Bundle params) {
                Toast.makeText(getApplicationContext(), "Escuchando...", Toast.LENGTH_SHORT).show();
                textoPrevio = editTextContenido.getText().toString();
                if (!textoPrevio.isEmpty() && !textoPrevio.endsWith(" ")) {
                    textoPrevio += " "; // Asegurar un espacio antes de añadir texto nuevo
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> texto = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texto != null && !texto.isEmpty()) {
                    editTextContenido.setText(textoPrevio + texto.get(0));
                    editTextContenido.setSelection(editTextContenido.getText().length());
                }
            }

            @Override public void onResults(Bundle results) {
                botonVoz.setEnabled(true);
                botonVoz.animate().scaleX(1f).scaleY(1f);
            }

            @Override public void onError(int error) {
                String errorMessage = getErrorText(error);
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                botonVoz.setEnabled(true);
                botonVoz.animate().scaleX(1f).scaleY(1f);
            }
            
            @Override public void onBeginningOfSpeech() {}
            @Override public void onEndOfSpeech() {
                 botonVoz.setEnabled(true);
                 botonVoz.animate().scaleX(1f).scaleY(1f);
            }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onBufferReceived(byte[] buffer) {}
        });
    }

    // Helper para traducir códigos de error a texto
    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Error de audio";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Error del cliente";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Permisos insuficientes - Revisa los permisos de la app de Google";
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:com.google.android.tts"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "No se pudo abrir la config de permisos reconocimiento de voz de Google", Toast.LENGTH_SHORT).show();
                }
                //Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                //intent.setData(Uri.parse("package:com.google.android.googlequicksearchbox")); // App de Google
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Error de red";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Timeout de red";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No se ha podido reconocer";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "El reconocedor está ocupado";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Error del servidor";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No se ha detectado voz";
                break;
            default:
                message = "Error desconocido";
                break;
        }
        return message;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // No es necesario llamar a iniciarReconocimientoDeVoz aquí, 
            // el usuario pulsará el botón de nuevo.
            Toast.makeText(this, "Permiso concedido. Pulsa el micrófono para grabar.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Permiso denegado para usar el micrófono", Toast.LENGTH_SHORT).show();
        }
    }
}
