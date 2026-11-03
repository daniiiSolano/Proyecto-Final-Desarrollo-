package modelo;

public class TableroMatriz {

    public static final int FILAS = 6;
    public static final int COLUMNAS = 6;
    public static final int PAREJAS = FILAS * COLUMNAS / 2;

    private final Casillas[][] matriz = new Casillas[FILAS][COLUMNAS];

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
