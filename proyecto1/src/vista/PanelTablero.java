package vista;

import apoyo.Recursos;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class PanelTablero extends JPanel {

    private static final int SEPARACION = 6;
    private static final int MARGEN = 12;
    private static final int BASE = 0, ALTERNA = 1, FRENTE = 2;

    private final int filas;
    private final int columnas;
    private final JButton[][] botones;
    private final int[][] estado;
    private final Color colorMarcador;
    private final Map<String, ImageIcon> cache = new HashMap<>();

    private String rutaBase;
    private String rutaAlterna;
    private String[][] rutasFrente;
    private int tamanoCasilla = 64;
    private int xTablero, yTablero, anchoTablero, altoTablero;
    private BiConsumer<Integer, Integer> oyente;

    public PanelTablero(int filas, int columnas, Color colorMarcador) {
        super(null);
        this.filas = filas;
        this.columnas = columnas;
        this.colorMarcador = colorMarcador;
        this.botones = new JButton[filas][columnas];
        this.estado = new int[filas][columnas];
        setOpaque(false);

        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                final int fila = f;
                final int columna = c;
                JButton boton = new JButton();
                boton.setContentAreaFilled(false);
                boton.setBorderPainted(false);
                boton.setFocusPainted(false);
                boton.setMargin(new Insets(0, 0, 0, 0));
                boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                boton.addActionListener(e -> {
                    if (oyente != null) {
                        oyente.accept(fila, columna);
                    }
                });
                botones[f][c] = boton;
                add(boton);
            }
        }
    }
    public void setImagenBase(String ruta) {
        this.rutaBase = ruta;
        refrescar();
    }

    public void setImagenAlterna(String ruta) {
        this.rutaAlterna = ruta;
        refrescar();
    }
    public void setImagenesFrente(String[][] rutas) {
        this.rutasFrente = rutas;
        refrescar();
    }
    public void mostrarFrente(boolean visible) {
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                estado[f][c] = visible ? FRENTE : BASE;
            }
        }
        refrescar();
    }

    public void alternarFrente(int fila, int columna) {
        estado[fila][columna] = (estado[fila][columna] == FRENTE) ? BASE : FRENTE;
        botones[fila][columna].setIcon(iconoDe(fila, columna));
    }

    public void marcarCasilla(int fila, int columna, boolean marcada) {
        estado[fila][columna] = marcada ? ALTERNA : BASE;
        botones[fila][columna].setIcon(iconoDe(fila, columna));
    }

    public boolean estaMarcada(int fila, int columna) {
        return estado[fila][columna] == ALTERNA;
    }
    public void setOyenteCasillas(BiConsumer<Integer, Integer> oyente) {
        this.oyente = oyente;
    }
    private void refrescar() {
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                botones[f][c].setIcon(iconoDe(f, c));
            }
        }
    }

    private ImageIcon iconoDe(int f, int c) {
        String ruta = rutaBase;
        Color marcador = colorMarcador;
        if (estado[f][c] == ALTERNA && rutaAlterna != null) {
            ruta = rutaAlterna;
        } else if (estado[f][c] == FRENTE && rutasFrente != null) {
            ruta = rutasFrente[f][c];
            marcador = Recursos.CREMA_OSCURO;
        }
        if (ruta == null) {
            return null;
        }
        final Color color = marcador;
        return cache.computeIfAbsent(ruta, r -> Recursos.cargarImagen(r, tamanoCasilla, tamanoCasilla, color));
    }

    @Override
    public void doLayout() {
        int ancho = getWidth() - 2 * MARGEN;
        int alto = getHeight() - 2 * MARGEN;
        int celda = Math.min((ancho - (columnas - 1) * SEPARACION) / columnas,
                (alto - (filas - 1) * SEPARACION) / filas);
        celda = Math.max(celda, 8);
        anchoTablero = columnas * celda + (columnas - 1) * SEPARACION;
        altoTablero = filas * celda + (filas - 1) * SEPARACION;
        xTablero = (getWidth() - anchoTablero) / 2;
        yTablero = (getHeight() - altoTablero) / 2;
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                botones[f][c].setBounds(xTablero + c * (celda + SEPARACION),
                        yTablero + f * (celda + SEPARACION), celda, celda);
            }
        }
        if (celda != tamanoCasilla) {
            tamanoCasilla = celda;
            cache.clear();
            refrescar();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Recursos.CAFE);
        g2.fillRoundRect(xTablero - MARGEN, yTablero - MARGEN, anchoTablero + 2 * MARGEN,
                altoTablero + 2 * MARGEN, 28, 28);
        g2.setColor(Recursos.CAFE_OSCURO);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(xTablero - MARGEN, yTablero - MARGEN, anchoTablero + 2 * MARGEN,
                altoTablero + 2 * MARGEN, 28, 28);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(columnas * 60 + (columnas - 1) * SEPARACION + 2 * MARGEN,
                filas * 60 + (filas - 1) * SEPARACION + 2 * MARGEN);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, 200);
    }
}
