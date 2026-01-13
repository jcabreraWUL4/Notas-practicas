package com.example.notas;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

public class NotasAdapter extends RecyclerView.Adapter<NotasAdapter.NotaViewHolder> {

    private final List<Nota> notas;
    private final OnNotaListener onNotaListener;

    public interface OnNotaListener {
        void onVerNotaClick(int notaId);
        void onTituloChanged(int notaId, String nuevoTitulo);
    }

    public NotasAdapter(List<Nota> notas, OnNotaListener onNotaListener) {
        this.notas = notas;
        this.onNotaListener = onNotaListener;
    }

    @NonNull
    @Override
    public NotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.nota_item, parent, false);
        return new NotaViewHolder(view, onNotaListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NotaViewHolder holder, int position) {
        holder.bind(notas.get(position));
    }

    @Override
    public int getItemCount() {
        return notas.size();
    }

    static class NotaViewHolder extends RecyclerView.ViewHolder {
        final EditText editTextTitulo;
        final Button buttonVerNota;
        final TextView textViewFecha;
        final OnNotaListener onNotaListener;
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable guardarRunnable;
        TextWatcher textWatcher;

        public NotaViewHolder(@NonNull View itemView, OnNotaListener onNotaListener) {
            super(itemView);
            this.onNotaListener = onNotaListener;
            editTextTitulo = itemView.findViewById(R.id.tituloNotaRecyclerView);
            buttonVerNota = itemView.findViewById(R.id.buttonVerNota);
            textViewFecha = itemView.findViewById(R.id.fechaNotaRecyclerView);
        }

        void bind(Nota nota) {
            if (textWatcher != null) {
                editTextTitulo.removeTextChangedListener(textWatcher);
            }

            editTextTitulo.setText(nota.getTitulo());
            textViewFecha.setText(nota.getFecha());

            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handler.removeCallbacks(guardarRunnable);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    guardarRunnable = () -> {
                        String nuevoTitulo = s.toString().trim();
                        if (!Objects.equals(nota.getTitulo(), nuevoTitulo)) {
                            onNotaListener.onTituloChanged(nota.getId(), nuevoTitulo);
                            nota.setTitulo(nuevoTitulo);
                        }
                    };
                    handler.postDelayed(guardarRunnable, 400);
                }
            };
            editTextTitulo.addTextChangedListener(textWatcher);

            buttonVerNota.setOnClickListener(v -> {
                handler.removeCallbacks(guardarRunnable);
                String nuevoTitulo = editTextTitulo.getText().toString().trim();
                if (!Objects.equals(nota.getTitulo(), nuevoTitulo)) {
                    onNotaListener.onTituloChanged(nota.getId(), nuevoTitulo);
                    nota.setTitulo(nuevoTitulo);
                }
                onNotaListener.onVerNotaClick(nota.getId());
            });
        }
    }
}
