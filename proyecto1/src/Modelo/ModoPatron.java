package Modelo;
public class ModoPatron {

    public static final int longitudInicial = 3;
    public static final int tiempo = 12;
    public static final int tiempoEntreCasillas = 2;

    private int nivel = 1;
    private int longitudPatron = longitudInicial;

    public int getNivel() {
        return nivel;
    }

    public int getLongitudPatron() {
        return longitudPatron;
    }
}
