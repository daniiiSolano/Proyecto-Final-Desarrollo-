package modelo;

public class ModoPatron {

    public static final int LONGITUD_INICIAL = 3;
    public static final int TIEMPO_TOTAL_SEG = 12;
    public static final int TIEMPO_ENTRE_CASILLAS_SEG = 2;

    private int nivel = 1;
    private int longitudPatron = LONGITUD_INICIAL;

    public int getNivel() {
        return nivel;
    }

    public int getLongitudPatron() {
        return longitudPatron;
    }
}
