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

    // Interfaz para los eventos sobre las notas
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
        final TextView textViewFecha;
        final TextView textViewTitulo;
        final OnNotaListener onNotaListener;
        LinearLayout notaItem;
        private Nota currentNota;

        public NotaViewHolder(@NonNull View itemView, OnNotaListener onNotaListener) {
            super(itemView);
            this.onNotaListener = onNotaListener;
            textViewTitulo = itemView.findViewById(R.id.tituloNotaRecyclerView);
            textViewFecha = itemView.findViewById(R.id.fechaNotaRecyclerView);

            notaItem = itemView.findViewById(R.id.notaItem);
            itemView.setOnCreateContextMenuListener(this);
        }

        // Cada item de nota tendra esto
        void bind(Nota nota) {
            this.currentNota = nota;

            textViewTitulo.setText(nota.getTitulo());
            textViewFecha.setText(nota.getFecha());
            notaItem.setOnClickListener(v -> onNotaListener.onClickVerNota(nota.getId()));
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuItem menuItemRenombrar = menu.add(Menu.NONE, 1, 1, "Renombrar");
            MenuItem menuItemEliminar = menu.add(Menu.NONE, 2, 2, "Eliminar");

            menuItemEliminar.setOnMenuItemClickListener(item -> onNotaListener.onContextEliminarNota(currentNota.getId()));
            menuItemRenombrar.setOnMenuItemClickListener(item -> onNotaListener.onContextRenombrarNota(currentNota.getId()));
        }
    }
}
