package com.example.notas;

public class Nota {
    int id;
    String titulo;
    String fecha;

    public Nota(int id, String titulo, String fecha) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
