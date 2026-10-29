package Modelo;

public class Jugador {

    private final String nombre;
    private int intentos;
    private double premio;

    public Jugador(String nombre) {
        this(nombre, 0, 0.0);
    }

    public Jugador(String nombre, int intentos, double premio) {
        this.nombre = nombre;
        this.intentos = intentos;
        this.premio = premio;
    }

    public String getNombre() {
        return nombre;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public double getPremio() {
        return premio;
    }

    public void setPremio(double premio) {
        this.premio = premio;
    }
}