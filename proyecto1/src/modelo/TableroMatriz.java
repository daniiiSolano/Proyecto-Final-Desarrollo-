package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TableroMatriz {

    public static final int FILAS = 6;
    public static final int COLUMNAS = 6;
    public static final int PAREJAS = FILAS * COLUMNAS / 2;

    private static final Random RANDOM = new Random();

    private final Casillas[][] matriz = new Casillas[FILAS][COLUMNAS];

    /**
     * Tablero ALEATORIO: las 18 imágenes (cada una dos veces) se revuelven y se
     * acomodan en la matriz de 6x6. Es el que se usa para jugar.
     */
    public TableroMatriz() {
        List<Integer> objetos = new ArrayList<>();
        for (int id = 1; id <= PAREJAS; id++) {
            objetos.add(id); // primera carta de la pareja
            objetos.add(id); // segunda carta de la pareja
        }
        Collections.shuffle(objetos, RANDOM);

        int indice = 0;
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                matriz[f][c] = new Casillas(objetos.get(indice++));
            }
        }
    }

    /** Tablero ordenado (sin aleatorio). Se conserva por compatibilidad. */
    public TableroMatriz(int desplazamiento) {
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                int posicion = f * COLUMNAS + c;
                matriz[f][c] = new Casillas((posicion + desplazamiento) % PAREJAS + 1);
            }
        }
    }

    public Casillas getCasilla(int fila, int columna) {
        return matriz[fila][columna];
    }

    public String[][] getRutasImagenes() {
        String[][] rutas = new String[FILAS][COLUMNAS];
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                rutas[f][c] = matriz[f][c].getRutaImagen();
            }
        }
        return rutas;
    }

    /** true en las casillas que deben verse boca arriba (volteadas o ya emparejadas). */
    public boolean[][] getCasillasDescubiertas() {
        boolean[][] descubiertas = new boolean[FILAS][COLUMNAS];
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                descubiertas[f][c] = matriz[f][c].isVisible() || matriz[f][c].isEmparejada();
            }
        }
        return descubiertas;
    }

    /** Compara si dos tableros tienen exactamente el mismo acomodo. */
    public boolean mismoAcomodo(TableroMatriz otro) {
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                if (matriz[f][c].getIdObjeto() != otro.matriz[f][c].getIdObjeto()) {
                    return false;
                }
            }
        }
        return true;
    }

    public int contarParejasEncontradas() {
        int emparejadas = 0;
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                if (matriz[f][c].isEmparejada()) {
                    emparejadas++;
                }
            }
        }
        return emparejadas / 2;
    }

    public boolean todasLasParejasEncontradas() {
        return contarParejasEncontradas() == PAREJAS;
    }
}

