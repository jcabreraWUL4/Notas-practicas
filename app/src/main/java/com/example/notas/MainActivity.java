package com.example.notas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editTextNota;
    private Button buttonGuardar;
    private TextView textViewNotaGuardada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referencias a la UI
        editTextNota = findViewById(R.id.editTextNota);
        buttonGuardar = findViewById(R.id.buttonGuardar);
        textViewNotaGuardada = findViewById(R.id.textViewNotaGuardada);

        // Acción del botón
        buttonGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String texto = editTextNota.getText().toString();

                if (!texto.isEmpty()) {
                    textViewNotaGuardada.setText(texto);
                    editTextNota.setText("");
                }
            }
        });
    }
}

