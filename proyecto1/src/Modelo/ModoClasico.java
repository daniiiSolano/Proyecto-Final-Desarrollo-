package Modelo;

public class ModoClasico {

    private int tiempo;
    private boolean juegoFinalizado;
    private int turnoActual;

    private TableroMatriz m1;
    private TableroMatriz m2;
    private Jugador jugador1;
    private Jugador jugador2;


//metodos


    public ModoClasico(int tiempo, boolean juegoFinalizado, int turnoActual, TableroMatriz m1, TableroMatriz m2, Jugador jugador1, Jugador jugador2) {
        this.tiempo = 10;
        this.juegoFinalizado = false; //al inicio debe ir falso y dsps cambia
        this.turnoActual = turnoActual;
        this.m1 = new TableroMatriz(6,6);
        this.m2 = new TableroMatriz(6,6);
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
    }

    //logic
    public void iniciarPartida() {
        this.juegoFinalizado = false; //
    }

    public boolean verificarPareja() {
        return false;
    }

    public void cambiarTurno() {
        this.turnoActual = (this.turnoActual == 1) ? 2 : 1; //Si es el turno 1 pasa al 2, si es el 2 pasa al 1, y reinicia el reloj a 10
        this.tiempo = 10;
    }

    public void iniciarTemporalizador() {
        //todo
    }

    public void actualizarTiempo() {
        // if (tiempo < 10)
    }

    public void detenerTiempo() {
        //todo
    }

    public boolean todasLasParejasEncontradas() {
        if (jugador1 == 1) {
            return m1.todasLasParejasEncontradas();
        } else {
            return m2.todasLasParejasEncontradas();
        }
    public int contarParejas() {
        if (numeroJugador == 1) {
            return m1.contarParejasEncontradas();
        } else {
            return m2.contarParejasEncontradas();
        }
    }

        public boolean hayGanador() {
            if (todasLasParejasEncontradas(1) || todasLasParejasEncontradas(2)) {
                return true;
            }
            return false;
        }

        public void finalizarPartida () {
            this.juegoFinalizado = true;
        }
    }
}
