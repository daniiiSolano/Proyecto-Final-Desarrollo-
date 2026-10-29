package Modelo;

import java.util.ArrayList;
import java.util.Random;

public class ModoPatron {

    private TableroMatriz tablero;
    private int nivel;
    private boolean juegoFinalizado;
    private ArrayList<int[]> secuenciaCorrecta; //guarda las coordenadas
    private int pasoActual; //en q paso de la secuencia va

//metodos
    public ModoPatron(TableroMatriz tablero, int nivel, boolean juegoFinalizado, ArrayList<int[]> secuenciaCorrecta, int pasoActual) {
        this.tablero = tablero;
        this.nivel = nivel;
        this.juegoFinalizado = juegoFinalizado;
        this.secuenciaCorrecta = secuenciaCorrecta;
        this.pasoActual = pasoActual;
    }

// get
    public int getNivel() {
        return nivel;
    }

    public boolean isJuegoFinalizado() {
        return juegoFinalizado;
    }

    public TableroMatriz getTablero() {
        return tablero;
    }

    public ArrayList<int[]> getSecuenciaCorrecta() {
        return secuenciaCorrecta;
    }

// Lógica
    public void iniciarPartida() {
        this.nivel = 1;
        this.juegoFinalizado = false;
        this.secuenciaCorrecta.clear();
        this.pasoActual = 0;
        generarSecuencia();
    }

    public void generarSecuencia() {
        Random random = new Random(); // Genera aleatoriamente
        secuenciaCorrecta.clear();

        int cantidadPasos = nivel + 2;
        for (int i = 0; i < cantidadPasos; i++) {
            int fila = random.nextInt(6); // Entre 0 y 5 para la matriz 6x6
            int columna = random.nextInt(6);
            secuenciaCorrecta.add(new int[]{fila, columna});
        }
        pasoActual = 0;
    }

    public boolean verificarPaso(int filaSeleccionada, int columnaSeleccionada) {    // Verifica si la casilla presionada por el usuario coincide con la secuencia
        if (juegoFinalizado) return false;

        int[] pasoEsperado = secuenciaCorrecta.get(pasoActual);

        if (pasoEsperado[0] == filaSeleccionada && pasoEsperado[1] == columnaSeleccionada) {
            pasoActual++;
            if (pasoActual >= secuenciaCorrecta.size()) { // Si ya completó toda la secuencia del nivel actual

                subirNivel();
            }
            return true; // El paso fue correcto
        } else {
            finalizarPartida();
            return false;
        }
    }

    public void subirNivel() {
        this.nivel++;
        this.pasoActual = 0;
        generarSecuencia(); // Genera una nueva secuencia más larga
    }

    public void finalizarPartida() {
        this.juegoFinalizado = true;
    }

}