package Modelo;

public class TableroMatriz {

    private Casillas [][] matriz;
    private int fila;
    private int columna;


    public TableroMatriz(Casillas[][] matriz, int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
        this.matriz = matriz;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public Casillas obtenerCasillas(int fila, int columna){
        if (posicionValida(fila, columna)) {
            return matriz[fila][columna];
        }
        return null;
    }

    public void iniciarTablero(){
    }

    public void mezclarCasillas(){
        //falta el random
    }

    public boolean posicionValida(int fila, int columna){
        return fila >= 0 && fila < this.fila && columna >= 0 && columna < this.columna;    }
}
