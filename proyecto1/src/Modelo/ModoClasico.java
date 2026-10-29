package Modelo;

import Modelo.Jugador;
import Modelo.TableroMatriz;

public class ModoClasico {

    public static final int TIEMPO_JUGADA_SEG = 10;
    public static final int BONO_ACIERTO_SEG = 7;

    private final Jugador jugador1;
    private final Jugador jugador2;
    private final TableroMatriz tablero1 = new TableroMatriz(0);
    private final TableroMatriz tablero2 = new TableroMatriz(7);
    private int turnoActual = 1; // 1 o 2

    public ModoClasico(Jugador jugador1, Jugador jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
    }

    public Jugador getJugador(int numero) {
        return numero == 1 ? jugador1 : jugador2;
    }

    public TableroMatriz getTablero(int numero) {
        return numero == 1 ? tablero1 : tablero2;
    }

    public int getTurnoActual() {
        return turnoActual;
    }
}