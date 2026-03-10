package com.Jhon.myempty.blogdenotasjava;

import java.util.ArrayList;
import java.util.List;

public class Nota {
    private String titulo;
    private String contenido;
    private String uri;
    private int color;
    private List<ItemAdjunto> adjuntos; // Añadido para los adjuntos

    // Constructor sin argumentos (importante para librerías como Gson)
    public Nota() {
        this.adjuntos = new ArrayList<>();
    }

    public Nota(String titulo, String contenido, int color, String uri) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.color = color;
        this.uri = uri;
        this.adjuntos = new ArrayList<>();
    }

    // --- Getters ---
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public String getUri() { return uri; }
    public int getColor() { return color; }
    public List<ItemAdjunto> getAdjuntos() { return adjuntos; }

    // --- Setters ---
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public void setUri(String uri) { this.uri = uri; }
    public void setColor(int color) { this.color = color; }
    public void setAdjuntos(List<ItemAdjunto> adjuntos) { this.adjuntos = adjuntos; }

    // --- Métodos de utilidad para adjuntos ---
    public void addAdjunto(ItemAdjunto adjunto) {
        if (this.adjuntos == null) {
            this.adjuntos = new ArrayList<>();
        }
        this.adjuntos.add(adjunto);
    }
}
