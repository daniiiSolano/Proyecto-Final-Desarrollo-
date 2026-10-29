package Modelo;

public class Jugador {

//atributos
    private String user;
    private int intentos;
    private double puntos;
    private int partidasGanadas;
    private int parejasEncontradas;
    private double premioAcumulado;

//metodos

    public Jugador(String user, int intentos, double puntos, int partidasGanadas, int parejasEncontradas, double premioAcumulado) {
        this.user = user;
        this.intentos = intentos;
        this.puntos = puntos;
        this.partidasGanadas = 0; // los 3 inician en cero en cada partida
        this.parejasEncontradas = 0;
        this.premioAcumulado = 0;
    }


//GETYSET


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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


//logca
    public void incrementarIntentos(){
        this.intentos++;
    }

    public void incrementarPartidasGanadas(){
        this.partidasGanadas++;
    }

    public void incrementarParejasEncontradas(){
        this.parejasEncontradas++;
    }

    public void agregarPremio(double monto){
        this.premioAcumulado +=monto;
    }

    public void reiniciarPartida(){
        this.intentos = 0;
    }
}
