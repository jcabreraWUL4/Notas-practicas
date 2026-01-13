package com.example.notas;

import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotasAdapter extends RecyclerView.Adapter<NotasAdapter.NotaViewHolder> {

    private final List<Nota> notas;
    private final OnNotaListener onNotaListener;

    public interface OnNotaListener {
        void onClickVerNota(int notaId);
        //void onTituloChanged(int notaId, String nuevoTitulo);
        boolean onContextEliminarNota(int notaId);
        boolean onContextRenombrarNota(int notaId);
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

    static class NotaViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        //final EditText editTextTitulo;
        //final Button buttonVerNota;
        final TextView textViewFecha;
        final TextView textViewTitulo;
        final OnNotaListener onNotaListener;
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable guardarRunnable;
        TextWatcher textWatcher;
        LinearLayout notaItem;
        private Nota currentNota;

        public NotaViewHolder(@NonNull View itemView, OnNotaListener onNotaListener) {
            super(itemView);
            this.onNotaListener = onNotaListener;
            textViewTitulo = itemView.findViewById(R.id.tituloNotaRecyclerView);
            textViewFecha = itemView.findViewById(R.id.fechaNotaRecyclerView);
            //buttonVerNota = itemView.findViewById(R.id.buttonVerNota);

            notaItem = itemView.findViewById(R.id.notaItem);
            // Le decimos a la vista del ítem que este ViewHolder se encargará de crear el menú
            itemView.setOnCreateContextMenuListener(this);
        }

        void bind(Nota nota) {
            this.currentNota = nota; // Guardamos la nota actual para usarla en el menú
            /*if (textWatcher != null) {
                editTextTitulo.removeTextChangedListener(textWatcher);
            }*/

            textViewTitulo.setText(nota.getTitulo());
            textViewFecha.setText(nota.getFecha());
            notaItem.setOnClickListener(v -> onNotaListener.onClickVerNota(nota.getId()));
            
            /*textWatcher = new TextWatcher() {
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
                            //onNotaListener.onTituloChanged(nota.getId(), nuevoTitulo);
                            nota.setTitulo(nuevoTitulo);
                        }
                    };
                    handler.postDelayed(guardarRunnable, 400);
                }
            };
            //editTextTitulo.addTextChangedListener(textWatcher);
            */
            /*
            buttonVerNota.setOnClickListener(v -> {
                handler.removeCallbacks(guardarRunnable);
                String nuevoTitulo = editTextTitulo.getText().toString().trim();
                if (!Objects.equals(nota.getTitulo(), nuevoTitulo)) {
                    onNotaListener.onTituloChanged(nota.getId(), nuevoTitulo);
                    nota.setTitulo(nuevoTitulo);
                }
                onNotaListener.onVerNotaClick(nota.getId());
            });
            */
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            // Creamos la opción "Eliminar" en el menú
            MenuItem menuItemEliminar = menu.add(Menu.NONE, 1, 1, "Eliminar");
            MenuItem menuItemRenombrar = menu.add(Menu.NONE, 2, 2, "Renombrar");

            // Le asignamos una acción para que llame al método de la Activity
            menuItemEliminar.setOnMenuItemClickListener(item -> onNotaListener.onContextEliminarNota(currentNota.getId()));
            menuItemRenombrar.setOnMenuItemClickListener(item -> onNotaListener.onContextRenombrarNota(currentNota.getId()));
        }
    }
}
