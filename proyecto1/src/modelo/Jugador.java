
package Modelo;

public class Jugador {

//atributos

    private final String nombre;
    private int intentos;
    private double premioAcumulado;
    private double puntos;
    private int partidasGanadas;
    private int parejasEncontradas;

//metodos

    public Jugador(String nombre, int intentos, double puntos,
                   int partidasGanadas, int parejasEncontradas,
                   double premioAcumulado) {

        this.nombre = nombre;
        this.intentos = intentos;
        this.puntos = puntos;
        this.partidasGanadas = 0; // los 3 inician en cero en cada partida
        this.parejasEncontradas = 0;
        this.premioAcumulado = 0;
    }
// Gests y set

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

    //
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

    public void reiniciarPartida() {    // Reinicia los datos, conserva las partidas ganadas y los premios
        this.intentos = 0;
        this.puntos = 0;
        this.parejasEncontradas = 0;
    }

}