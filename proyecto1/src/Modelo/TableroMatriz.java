package Modelo;

public class TableroMatriz {

    public static final int filas = 6;
    public static final int columnas = 6;
    public static final int parejas = filas * columnas / 2; // 18

    private final Casillas[][] matriz = new Casillas[filas][columnas];

    public TableroMatriz(int desplazamiento) {
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                int posicion = f * columnas + c;
                matriz[f][c] = new Casillas((posicion + desplazamiento) % parejas + 1);
            }
        }
    }

    public Casillas getCasilla(int fila, int columna) {
        return matriz[fila][columna];
    }
    public String[][] getRutasImagenes() {
        String[][] rutas = new String[filas][columnas];
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                rutas[f][c] = matriz[f][c].getRutaImagen();
            }
        }
        return rutas;
    }
}
