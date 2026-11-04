package modelo;

/**
 * Lógica del modo clásico (2 jugadores, cada uno con su propio tablero aleatorio de 6x6).
 *
 * Reglas (según el enunciado):
 *  - Cada jugador tiene 10 segundos para realizar cada jugada (voltear 2 cartas).
 *  - Si acierta, se le suman 7 segundos al tiempo que le quedaba (el reloj
 *    sigue corriendo, no se reinicia) y sigue jugando.
 *  - Si falla, se le cede el turno al contrincante.
 *  - Gana el jugador que encuentra primero todas las parejas de su tablero:
 *    en ese momento termina la partida y se muestra la animación de victoria.
 *
 * Decisiones tomadas donde el enunciado no especifica:
 *  - Un intento = voltear dos cartas (acierto o fallo). Si se acaba el tiempo
 *    de la jugada, cuenta como intento fallido y pasa el turno.
 *
 * El modelo no usa Swing: la vista le avisa cuánto tiempo pasó con
 * {@link #avanzarTiempo(long)}.
 */
public class ModoClasico {

    public static final int TIEMPO_JUGADA_SEG = 10;
    public static final int BONO_ACIERTO_SEG = 7;
    public static final long TIEMPO_JUGADA_MS = TIEMPO_JUGADA_SEG * 1000L;
    public static final long BONO_ACIERTO_MS = BONO_ACIERTO_SEG * 1000L;

    /** Lo que pasó al presionar una carta. */
    public enum ResultadoJugada {
        IGNORADO,       // clic no válido (no es su turno, carta ya volteada, partida sin iniciar...)
        PRIMERA_CARTA,  // se volteó la primera carta de la jugada
        ACIERTO,        // las dos cartas son pareja: +7 s y sigue el mismo jugador
        FALLO,          // no son pareja: la vista las muestra un momento y luego llama a resolverFallo()
        FIN_PARTIDA     // con esta jugada terminó la partida
    }

    private int tiempo;               // segundos restantes (se conserva por compatibilidad)
    private long tiempoRestanteMs;
    private boolean juegoFinalizado;
    private boolean enCurso;
    private boolean esperandoOcultar; // hay dos cartas distintas a la vista esperando voltearse
    private final Jugador jugador1;
    private final Jugador jugador2;
    private TableroMatriz tablero1;
    private TableroMatriz tablero2;
    private int turnoActual = 1;      // 1 o 2
    private int ganador = -1;         // -1 sin decidir, 1 o 2 según quién ganó

    private int[] primeraCarta;       // {fila, columna} de la primera carta de la jugada
    private int[] segundaCarta;

    public ModoClasico(Jugador jugador1, Jugador jugador2) {
        this(TIEMPO_JUGADA_SEG, false, 1, null, null, jugador1, jugador2);
        crearTablerosAleatorios();
    }

    public ModoClasico(int tiempo, boolean juegoFinalizado, int turnoActual, TableroMatriz tablero1,
                       TableroMatriz tabblero2, Jugador jugador1, Jugador jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.tiempo = tiempo;
        this.tiempoRestanteMs = tiempo * 1000L;
        this.juegoFinalizado = juegoFinalizado;
        this.turnoActual = turnoActual;
        this.tablero1 = tablero1;
        this.tablero2 = tabblero2;
    }

    // ---------------------------------------------------------------- getters

    public int getTiempo() {
        return tiempo;
    }

    public void setTiempo(int tiempo) {
        this.tiempo = tiempo;
        this.tiempoRestanteMs = tiempo * 1000L;
    }

    public long getTiempoRestanteMs() {
        return tiempoRestanteMs;
    }

    public boolean isJuegoFinalizado() {
        return juegoFinalizado;
    }

    public boolean isEnCurso() {
        return enCurso;
    }

    public boolean isEsperandoOcultar() {
        return esperandoOcultar;
    }

    public int getTurnoActual() {
        return turnoActual;
    }

    /** -1 si aún no termina, 1 o 2 según el jugador que ganó. */
    public int getGanador() {
        return ganador;
    }

    public Jugador getJugador(int numero) {
        return numero == 1 ? jugador1 : jugador2;
    }

    public TableroMatriz getTablero(int numero) {
        return numero == 1 ? tablero1 : tablero2;
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

    // ---------------------------------------------------------------- partida

    /** Tableros nuevos (aleatorios y distintos entre sí), contadores en cero y turno al jugador 1. */
    public void iniciarPartida() {
        crearTablerosAleatorios();
        jugador1.reiniciarPartida();
        jugador2.reiniciarPartida();
        this.juegoFinalizado = false;
        this.enCurso = true;
        this.esperandoOcultar = false;
        this.ganador = -1;
        this.turnoActual = 1;
        this.primeraCarta = null;
        this.segundaCarta = null;
        reiniciarTiempo();
    }

    /** Detiene la partida sin ganador (por ejemplo, si el jugador sale de la pantalla). */
    public void cancelarPartida() {
        crearTablerosAleatorios();
        jugador1.reiniciarPartida();
        jugador2.reiniciarPartida();
        this.enCurso = false;
        this.juegoFinalizado = false;
        this.esperandoOcultar = false;
        this.ganador = -1;
        this.turnoActual = 1;
        this.primeraCarta = null;
        this.segundaCarta = null;
        reiniciarTiempo();
    }

    private void crearTablerosAleatorios() {
        tablero1 = new TableroMatriz();
        do {
            tablero2 = new TableroMatriz();
        } while (tablero2.mismoAcomodo(tablero1)); // el aleatorio de cada jugador debe ser distinto
    }

    /**
     * El jugador {@code numeroJugador} presionó la carta (fila, columna) de SU tablero.
     */
    public ResultadoJugada seleccionarCasilla(int numeroJugador, int fila, int columna) {
        if (!enCurso || juegoFinalizado || esperandoOcultar || numeroJugador != turnoActual) {
            return ResultadoJugada.IGNORADO;
        }
        TableroMatriz tablero = getTablero(numeroJugador);
        Casillas casilla = tablero.getCasilla(fila, columna);
        if (casilla.isVisible() || casilla.isEmparejada()) {
            return ResultadoJugada.IGNORADO; // ya está boca arriba
        }

        casilla.revelar();

        if (primeraCarta == null) {
            primeraCarta = new int[]{fila, columna};
            return ResultadoJugada.PRIMERA_CARTA;
        }

        // Segunda carta: se compara la pareja
        segundaCarta = new int[]{fila, columna};
        Jugador jugador = getJugador(numeroJugador);
        jugador.incrementarIntentos();
        Casillas c1 = tablero.getCasilla(primeraCarta[0], primeraCarta[1]);

        if (verificarPareja(c1, casilla)) {
            c1.marcarEmparejada();
            casilla.marcarEmparejada();
            jugador.incrementarParejasEncontradas();
            primeraCarta = null;
            segundaCarta = null;
            sumarTiempo(BONO_ACIERTO_MS);

            if (evaluarFin()) { // encontró todas sus parejas: gana
                return ResultadoJugada.FIN_PARTIDA;
            }
            return ResultadoJugada.ACIERTO;
        }

        // No son pareja: se quedan a la vista hasta que la vista llame a resolverFallo()
        esperandoOcultar = true;
        return ResultadoJugada.FALLO;
    }

    /**
     * Voltea de nuevo las dos cartas del fallo y cede el turno.
     *
     * @return true si con este fallo terminó la partida.
     */
    public boolean resolverFallo() {
        if (!esperandoOcultar) {
            return juegoFinalizado;
        }
        ocultarCartasDeLaJugada();
        esperandoOcultar = false;
        if (evaluarFin()) {
            return true;
        }
        cambiarTurno();
        return false;
    }

    /**
     * Descuenta tiempo al jugador en turno. Si se le acaba, la jugada cuenta como
     * intento fallido y el turno pasa al contrincante.
     *
     * @return true si se acabó el tiempo (cambió el turno o terminó la partida).
     */
    public boolean avanzarTiempo(long milisegundos) {
        if (!enCurso || juegoFinalizado || esperandoOcultar || milisegundos <= 0) {
            return false; // mientras se muestran las cartas del fallo el reloj se pausa
        }
        tiempoRestanteMs = Math.max(0, tiempoRestanteMs - milisegundos);
        tiempo = (int) Math.ceil(tiempoRestanteMs / 1000.0);
        if (tiempoRestanteMs > 0) {
            return false;
        }
        // Se acabó el tiempo de la jugada
        ocultarCartasDeLaJugada();
        getJugador(turnoActual).incrementarIntentos();
        if (!evaluarFin()) {
            cambiarTurno();
        }
        return true;
    }

    // Verifica si dos casillas forman una pareja
    public boolean verificarPareja(Casillas c1, Casillas c2) {
        if (c1 == null || c2 == null) {
            return false;
        }
        if (c1 == c2) { // la misma casilla dos veces
            return false;
        }
        if (!c1.isVisible() || !c2.isVisible()) {
            return false;
        }
        if (c1.isEmparejada() || c2.isEmparejada()) {
            return false;
        }
        return c1.getIdObjeto() == c2.getIdObjeto();
    }

    /** Cede el turno al contrincante, con 10 segundos nuevos. */
    public void cambiarTurno() {
        turnoActual = (turnoActual == 1) ? 2 : 1;
        reiniciarTiempo();
    }

    private void ocultarCartasDeLaJugada() {
        TableroMatriz tablero = getTablero(turnoActual);
        if (primeraCarta != null) {
            tablero.getCasilla(primeraCarta[0], primeraCarta[1]).ocultar();
        }
        if (segundaCarta != null) {
            tablero.getCasilla(segundaCarta[0], segundaCarta[1]).ocultar();
        }
        primeraCarta = null;
        segundaCarta = null;
    }

    private void reiniciarTiempo() {
        tiempoRestanteMs = TIEMPO_JUGADA_MS;
        tiempo = TIEMPO_JUGADA_SEG;
    }

    private void sumarTiempo(long ms) {
        tiempoRestanteMs += ms;
        tiempo = (int) Math.ceil(tiempoRestanteMs / 1000.0);
    }

    /**
     * Revisa si algún jugador ya encontró todas sus parejas. El primero que
     * completa su tablero gana la partida.
     */
    private boolean evaluarFin() {
        if (todasLasParejasEncontradas(1)) {
            ganador = 1;
        } else if (todasLasParejasEncontradas(2)) {
            ganador = 2;
        } else {
            return false;
        }
        finalizarPartida();
        return true;
    }

    // Comprueba si todas las parejas de un jugador fueron encontradas.
    public boolean todasLasParejasEncontradas(int numeroJugador) {
        if (numeroJugador == 1) {
            return tablero1.todasLasParejasEncontradas();
        } else if (numeroJugador == 2) {
            return tablero2.todasLasParejasEncontradas();
        }
        return false;
    }

    // Cuenta las parejas encontradas por cada jugador
    public int contarParejas(int numeroJugador) {
        if (numeroJugador == 1) {
            return tablero1.contarParejasEncontradas();
        } else if (numeroJugador == 2) {
            return tablero2.contarParejasEncontradas();
        }
        return 0;
    }

    public boolean hayGanador() {
        return ganador == 1 || ganador == 2;
    }

    // Finaliza la partida
    public void finalizarPartida() {
        this.juegoFinalizado = true;
        this.enCurso = false;
        this.esperandoOcultar = false;
        if (ganador == 1) {
            jugador1.incrementarPartidasGanadas();
        } else if (ganador == 2) {
            jugador2.incrementarPartidasGanadas();
        }
    }
}



