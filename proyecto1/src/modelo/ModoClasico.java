package Modelo;

import Modelo.Jugador;
import Modelo.TableroMatriz;

import java.util.Timer;

public class ModoClasico {

    private static final int TIEMPO_JUGADA_SEG = 10;
    private static final int BONO_ACIERTO_SEG = 7;
    private int tiempo;
    private boolean juegoFinalizado;
    private final Jugador jugador1;
    private final Jugador jugador2;
    private TableroMatriz tablero1 = new TableroMatriz(0);
    private TableroMatriz tablero2 = new TableroMatriz(7);
    private int turnoActual = 1; // 1 o 2

    private Timer temporizador; // Temporizador para controlar el tiempo de cada turno library

    //
    public ModoClasico(int tiempo, boolean juegoFinalizado, int turnoActual, TableroMatriz tablero1, TableroMatriz tabblero2, Jugador jugador1, Jugador jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.tiempo = 10;
        this.juegoFinalizado = false; // al inicio debe ir falso y después cambia
        this.turnoActual = turnoActual;
        this.tablero1 = tablero1;
        this.tablero2 = tabblero2;
        this.temporizador = null;
    }

    // get n set
    public int getTiempo() {
        return tiempo;
    }

    public void setTiempo(int tiempo) {
        this.tiempo = tiempo;
    }

    public boolean isJuegoFinalizado() {
        return juegoFinalizado;
    }

    public int getTurnoActual() {
        return turnoActual;
    }

    public Jugador getJugador1() {
        return jugador1;
    }

    public Jugador getJugador2() {
        return jugador2;
    }

    public TableroMatriz getM1() {
        return tablero1;
    }

    public TableroMatriz getM2() {
        return tablero2;
    }

// LOGIC

    public void iniciarPartida() {// Prepara una nueva partida con el tiempo y el turno


        this.juegoFinalizado = false; // Indica que la partida está activa
        this.tiempo = 10; // Establece el tiempo inicial en 10 segundos
        this.turnoActual = 1; // El jugador 1 comienza la partida
    }


    // Verifica si dos casillas forman una pareja y tecibe las dos casillas que fueron seleccionadas
    public boolean verificarPareja(Casillas c1, Casillas c2) {

        if (c1 == null || c2 == null) { // Comprueba que ninguna de las casillas esté vacía

            return false; // Si alguna es null, no existe una pareja válida
        }

        if (c1 == c2) { // Comprueba que no se haya seleccionado dos veces la misma casilla

            return false;
        }

        if (!c1.isVisible() || !c2.isVisible()) { // Comprueba que ambas casillas estén visibles

            return false;
        }

        if (c1.isEmparejada() || c2.isEmparejada()) { // Comprueba que ninguna casilla ya esté emparejada

            return false;
        }

        // Compara si son iguales, las casillas forman una pareja
        return c1.getIcon() != null
                && c1.getIcon().equals(c2.getIcon());
    }


    public void cambiarTurno() {// Cambia el turno del jugador actual


        // Si el turno es 1, cambia a 2 y si es 2, cambia a 1
        this.turnoActual = (this.turnoActual == 1) ? 2 : 1;

        // Reinicia el reloj a 10 segundos para el siguiente jugador
        this.tiempo = 10;

        detenerTiempo(); // Detiene el temporizador anterior
        iniciarTemporalizador(); // Inicia el temporizador del nuevo turno
    }


    // Inicia el temporizador que controla los 10 segundos de cada turno
    public void iniciarTemporalizador() {

        detenerTiempo(); // Evita que existan dos temporizadores funcionando

        // Comprueba que la partida todavía esta activada
        if (juegoFinalizado) {
            return; // Sale del método si la partida ya termina
        }

        // Crea un temporizador que ejecuta actualizarTiempo() cada segundo.
        // 1000 milisegundos equivalen a 1 segundo.
        temporizador = new Timer(1000, e -> actualizarTiempo());

        temporizador.start(); // Pone en funcionamiento el temporizador.
    }


    // Actualiza el tiempo restante cada segundo
    public void actualizarTiempo() {

        if (tiempo > 0) { // Comprueba que todavía quede tiempo
            tiempo--; // Reduce el tiempo en un Segundo
        }


        if (tiempo <= 0) {// Comprueba si el tiempo llegó a cero

            detenerTiempo(); // Detiene el reloj actual

            // Cuando termina el tiempo, se cambia el turno
            cambiarTurno();
        }
    }


    public void detenerTiempo() {

        // Comprueba queel temporizador activo
        if (temporizador != null) {

            temporizador.stop(); // Detiene el temporizador
            temporizador = null; // Libera la referencia al temporizado
        }
    }


    // Comprueba si todas las parejas de un jugador fueron encontradas.
    public boolean todasLasParejasEncontradas(int numeroJugador) {

        // Si el número es 1, consulta el tablero del jugador 1
        if (numeroJugador == 1) {
            return tablero1.todasLasParejasEncontradas();

            // Si el número es 2, consulta el tablero del jugador 2
        } else if (numeroJugador == 2) {
            return tablero2.todasLasParejasEncontradas();
        }

        // Si el número no corresponde a ningun jugador, devuelve false
        return false;
    }


    // Cuenta las parejas encontradas por cada jugador
    public int contarParejas(int numeroJugador) {

        // Consulta cuántas parejas encontró el jugador 1
        if (numeroJugador == 1) {
            return tablero1.contarParejasEncontradas();

            // Consulta cuántas parejas encontró el jugador 2
        } else if (numeroJugador == 2) {
            return tablero2.contarParejasEncontradas();
        }

        // Si el número no es válido, devuelve cero
        return 0;
    }

    public boolean hayGanador() {
        // Devuelve true si el jugador 1 o el jugador 2 encontro todas las parejas.
        return todasLasParejasEncontradas(1) || todasLasParejasEncontradas(2);
    }


    // Finaliza la partida y detiene el temporizador
    public void finalizarPartida() {
        this.juegoFinalizado = true; // Indica que la partida termino
        detenerTiempo(); // Evita que el reloj siga corriendo
    }

}

