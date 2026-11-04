package modelo;

public class Jugador {

    private String nombre;
    private int intentos;
    private double premioAcumulado;
    private double puntos;
    private int partidasGanadas;
    private int parejasEncontradas;

    public Jugador(String nombre) {
        this(nombre, 0, 0.0, 0, 0, 0.0);
    }

    public Jugador(String nombre, int intentos, double premio) {
        this(nombre, intentos, 0.0, 0, 0, premio);
    }

    public Jugador(String nombre, int intentos, double puntos,
                   int partidasGanadas, int parejasEncontradas,
                   double premioAcumulado) {
        this.nombre = nombre;
        this.intentos = intentos;
        this.puntos = puntos;
        this.partidasGanadas = partidasGanadas;
        this.parejasEncontradas = parejasEncontradas;
        this.premioAcumulado = premioAcumulado;
    }

    public String getNombre() {
        return nombre;
    }

    /** Cambia el nombre (se usa en el modo clásico para los nombres que escriben los jugadores). */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUser() {
        return nombre;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public double getPuntos() {
        return puntos;
    }

    public void setPuntos(double puntos) {
        this.puntos = puntos;
    }

    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    public void setPartidasGanadas(int partidasGanadas) {
        this.partidasGanadas = partidasGanadas;
    }

    public int getParejasEncontradas() {
        return parejasEncontradas;
    }

    public void setParejasEncontradas(int parejasEncontradas) {
        this.parejasEncontradas = parejasEncontradas;
    }

    public double getPremioAcumulado() {
        return premioAcumulado;
    }

    public void setPremioAcumulado(double premioAcumulado) {
        this.premioAcumulado = premioAcumulado;
    }

    public double getPremio() {
        return premioAcumulado;
    }

    public void setPremio(double premio) {
        this.premioAcumulado = premio;
    }

    public void incrementarIntentos() {
        this.intentos++;
    }

    public void incrementarPartidasGanadas() {
        this.partidasGanadas++;
    }

    public void incrementarParejasEncontradas() {
        this.parejasEncontradas++;
    }

    public void agregarPremio(double monto) {
        this.premioAcumulado += monto;
    }

    public void reiniciarPartida() {
        this.intentos = 0;
        this.puntos = 0;
        this.parejasEncontradas = 0;
    }
}
