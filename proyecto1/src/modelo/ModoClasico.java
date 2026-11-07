package modelo;

import modelo.*;

// Clase que tiene las reglas del modo clásico (2 jugadores, cada uno con su tablero de 6x6).
// y cuánto tiempo ha pasado, y esta clase decide qué pasa en el juego.
public class ModoClasico {

    // Constantes del enunciado: 10 segundos por jugada y 7 segundos extra por acierto
    public static final int TIEMPO_JUGADA_SEG = 10;
    public static final int BONO_ACIERTO_SEG = 7;
    // Los mismos tiempos en milisegundos, porque el reloj se descuenta en ms para que sea más exacto
    public static final long TIEMPO_JUGADA_MS = TIEMPO_JUGADA_SEG * 1000L;
    public static final long BONO_ACIERTO_MS = BONO_ACIERTO_SEG * 1000L;

    // Posibles resultados de presionar una carta. La vista revisa este valor para saber qué mostrar
    public enum ResultadoJugada {
        IGNORADO,      // el clic no cuenta (no es su turno, la carta ya estaba volteada, etc.)
        PRIMERA_CARTA, // se volteó la primera carta de la jugada
        ACIERTO,       // las dos cartas son pareja: se suman 7 s y sigue el mismo jugador
        FALLO,         // no son pareja: se muestran un momento y después se llama a resolverFallo()
        TABLERO_COMPLETO, // el jugador completó su tablero primero; ahora el rival juega solo
        FIN_PARTIDA    // con esta jugada se decidió el ganador (o el empate)
    }

    private int tiempo;               // segundos que le quedan al jugador en turno
    private long tiempoRestanteMs;    // lo mismo en milisegundos (es el que se usa para contar)
    private boolean juegoFinalizado;  // true cuando ya hay ganador
    private boolean enCurso;          // true mientras se está jugando
    private boolean esperandoOcultar; // true si hay 2 cartas distintas esperando voltearse
    private final Jugador jugador1;
    private final Jugador jugador2;
    private TableroMatriz tablero1;   // tablero del jugador 1
    private TableroMatriz tablero2;   // tablero del jugador 2
    private int turnoActual = 1;      // 1 o 2, según a quién le toca
    private int ganador = -1;         // -1 mientras no hay ganador; 0 si es empate; luego 1 o 2

    // Enunciado: "Gana el que utilice menos intentos para encontrar las parejas".
    // Cuando un jugador completa su tablero se anotan sus intentos y el rival sigue jugando solo
    // hasta que termine su tablero o hasta que llegue a esa misma cantidad de intentos.
    private int terminoPrimero = 0;   // 0 si nadie ha completado su tablero; luego 1 o 2
    private int intentosPrimero = -1; // intentos con los que terminó el primero

    private int[] primeraCarta;       // {fila, columna} de la primera carta volteada en la jugada
    private int[] segundaCarta;       // {fila, columna} de la segunda carta volteada

    // Constructor que usa el controlador: arranca con 10 s, turno del jugador 1 y tableros aleatorios
    public ModoClasico(Jugador jugador1, Jugador jugador2) {
        this(TIEMPO_JUGADA_SEG, false, 1, null, null, jugador1, jugador2);
        crearTablerosAleatorios();
    }

    // Constructor con todos los atributos
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

    // ---------------------------------------------------------------- getters y setters

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

    // Devuelve -1 si la partida no ha terminado, 0 si fue empate, o 1 / 2 según quién ganó
    public int getGanador() {
        return ganador;
    }

    // 0 si nadie ha completado su tablero, o 1 / 2 según quién lo completó primero
    public int getTerminoPrimero() {
        return terminoPrimero;
    }

    // Intentos con los que el primero completó su tablero (-1 si nadie ha terminado)
    public int getIntentosPrimero() {
        return intentosPrimero;
    }

    // Devuelve el jugador 1 o el 2 según el número que se le pase
    public Jugador getJugador(int numero) {
        return numero == 1 ? jugador1 : jugador2;
    }

    // Devuelve el tablero del jugador 1 o del 2
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

    // ---------------------------------------------------------------- lógica de la partida

    // Prepara una partida nueva: tableros nuevos, intentos en 0, turno del jugador 1 y 10 segundos
    public void iniciarPartida() {
        crearTablerosAleatorios();
        jugador1.reiniciarPartida();
        jugador2.reiniciarPartida();
        this.juegoFinalizado = false;
        this.enCurso = true;
        this.esperandoOcultar = false;
        this.ganador = -1;
        this.terminoPrimero = 0;
        this.intentosPrimero = -1;
        this.turnoActual = 1;
        this.primeraCarta = null;
        this.segundaCarta = null;
        reiniciarTiempo();

        tablero1.imprimirTableroEnConsola(jugador1.getNombre());
        tablero2.imprimirTableroEnConsola(jugador2.getNombre());
    }

    // Detiene la partida sin ganador (se usa cuando el jugador sale de la pantalla)
    public void cancelarPartida() {
        crearTablerosAleatorios();
        jugador1.reiniciarPartida();
        jugador2.reiniciarPartida();
        this.enCurso = false;
        this.juegoFinalizado = false;
        this.esperandoOcultar = false;
        this.ganador = -1;
        this.terminoPrimero = 0;
        this.intentosPrimero = -1;
        this.turnoActual = 1;
        this.primeraCarta = null;
        this.segundaCarta = null;
        reiniciarTiempo();
    }

    // Crea los dos tableros aleatorios. El do-while repite el tablero 2 hasta que
    // quede distinto al tablero 1, porque el enunciado pide que el aleatorio de cada jugador sea distinto
    private void crearTablerosAleatorios() {
        tablero1 = new TableroMatriz();
        do {
            tablero2 = new TableroMatriz();
        } while (tablero2.mismoAcomodo(tablero1));
    }

    // Se llama cada vez que un jugador presiona una carta de su tablero
    public ResultadoJugada seleccionarCasilla(int numeroJugador, int fila, int columna) {
        // Si no hay partida, si se están mostrando las cartas de un fallo
        // o si no es su turno, no se hace nada
        if (!enCurso || juegoFinalizado || esperandoOcultar || numeroJugador != turnoActual) {
            return ResultadoJugada.IGNORADO;
        }
        TableroMatriz tablero = getTablero(numeroJugador);
        Casillas casilla = tablero.getCasilla(fila, columna);
        // Si la carta ya está boca arriba tampoco cuenta
        if (casilla.isVisible() || casilla.isEmparejada()) {
            return ResultadoJugada.IGNORADO;
        }

        casilla.revelar(); // se voltea la carta

        // Si todavía no había primera carta, esta es la primera y se espera la segunda
        if (primeraCarta == null) {
            primeraCarta = new int[]{fila, columna};
            return ResultadoJugada.PRIMERA_CARTA;
        }

        // Si llega aquí es la segunda carta: se cuenta un intento y se comparan las dos
        segundaCarta = new int[]{fila, columna};
        Jugador jugador = getJugador(numeroJugador);
        jugador.incrementarIntentos();
        Casillas c1 = tablero.getCasilla(primeraCarta[0], primeraCarta[1]);

        if (verificarPareja(c1, casilla)) {
            // Es pareja: las dos cartas se quedan boca arriba
            c1.marcarEmparejada();
            casilla.marcarEmparejada();
            jugador.incrementarParejasEncontradas();
            primeraCarta = null;
            segundaCarta = null;
            sumarTiempo(BONO_ACIERTO_MS); // +7 segundos al tiempo que le quedaba

            // ¿Con esta pareja es el primero en completar las 18?
            boolean acabaDeTerminar = terminoPrimero == 0 && tablero.todasLasParejasEncontradas();
            // Se revisa si ya se puede decidir el ganador
            if (evaluarFin()) {
                return ResultadoJugada.FIN_PARTIDA;
            }
            // Terminó primero, pero el rival todavía puede ganarle con menos intentos:
            // se le pasa el turno al rival, que seguirá jugando solo
            if (acabaDeTerminar) {
                cambiarTurno();
                return ResultadoJugada.TABLERO_COMPLETO;
            }
            return ResultadoJugada.ACIERTO; // sigue jugando el mismo jugador
        }

        // No es pareja: las cartas se dejan a la vista hasta que la vista llame a resolverFallo()
        esperandoOcultar = true;
        return ResultadoJugada.FALLO;
    }

    // Después de mostrar el fallo un segundo, la vista llama a este método:
    // voltea de nuevo las dos cartas y le pasa el turno al otro jugador
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

    // La vista llama a este método varias veces por segundo con los milisegundos que pasaron.
    // Devuelve true si al jugador se le acabó el tiempo
    public boolean avanzarTiempo(long milisegundos) {
        // Mientras se muestran las cartas de un fallo el reloj queda en pausa
        if (!enCurso || juegoFinalizado || esperandoOcultar || milisegundos <= 0) {
            return false;
        }
        // Math.max evita que el tiempo quede negativo
        tiempoRestanteMs = Math.max(0, tiempoRestanteMs - milisegundos);
        tiempo = (int) Math.ceil(tiempoRestanteMs / 1000.0); // redondeo hacia arriba
        if (tiempoRestanteMs > 0) {
            return false;
        }
        // Se acabó el tiempo: se voltean las cartas que tuviera,
        // cuenta como intento fallido y pasa el turno
        ocultarCartasDeLaJugada();
        getJugador(turnoActual).incrementarIntentos();
        if (!evaluarFin()) {
            cambiarTurno();
        }
        return true;
    }

    // Revisa si dos casillas forman una pareja
    public boolean verificarPareja(Casillas c1, Casillas c2) {
        if (c1 == null || c2 == null) { // alguna casilla no existe
            return false;
        }
        if (c1 == c2) { // es la misma casilla dos veces
            return false;
        }
        if (!c1.isVisible() || !c2.isVisible()) { // las dos tienen que estar volteadas
            return false;
        }
        if (c1.isEmparejada() || c2.isEmparejada()) { // ya se habían emparejado antes
            return false;
        }
        return c1.getIdObjeto() == c2.getIdObjeto(); // son pareja si tienen la misma imagen (mismo id)
    }

    // Le pasa el turno al otro jugador y le da sus 10 segundos.
    // Si un jugador ya completó su tablero, el turno queda siempre con el rival (juega solo)
    public void cambiarTurno() {
        if (terminoPrimero != 0) {
            turnoActual = (terminoPrimero == 1) ? 2 : 1;
        } else {
            turnoActual = (turnoActual == 1) ? 2 : 1;
        }
        reiniciarTiempo();
    }

    // Vuelve a poner boca abajo las cartas de la jugada actual (si había alguna)
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

    // Pone el reloj otra vez en 10 segundos
    private void reiniciarTiempo() {
        tiempoRestanteMs = TIEMPO_JUGADA_MS;
        tiempo = TIEMPO_JUGADA_SEG;
    }

    // Suma tiempo al que le queda al jugador (se usa para el bono de 7 segundos)
    private void sumarTiempo(long ms) {
        tiempoRestanteMs += ms;
        tiempo = (int) Math.ceil(tiempoRestanteMs / 1000.0);
    }

    // Decide si ya hay ganador. Gana el que usa MENOS intentos para encontrar las 18 parejas:
    // 1. Cuando un jugador completa su tablero se anotan sus intentos.
    // 2. Si el rival ya tiene igual o más intentos, el rival pierde (ya no puede ganarle).
    // 3. Si tiene menos, sigue jugando solo hasta que termine su tablero o llegue a esos intentos.
    // Devuelve true si la partida terminó
    private boolean evaluarFin() {
        // Paso 1: si nadie había terminado, se revisa si alguien acaba de completar su tablero
        if (terminoPrimero == 0) {
            if (todasLasParejasEncontradas(1)) {
                terminoPrimero = 1;
            } else if (todasLasParejasEncontradas(2)) {
                terminoPrimero = 2;
            } else {
                return false; // nadie ha terminado todavía
            }
            intentosPrimero = getJugador(terminoPrimero).getIntentos(); // se anotan sus intentos
        }

        int rival = (terminoPrimero == 1) ? 2 : 1;
        int intentosRival = getJugador(rival).getIntentos();

        if (todasLasParejasEncontradas(rival)) {
            // El rival también completó su tablero: se comparan los intentos
            if (intentosRival < intentosPrimero) {
                ganador = rival;
            } else if (intentosRival == intentosPrimero) {
                ganador = 0; // empate
            } else {
                ganador = terminoPrimero;
            }
        } else if (intentosRival >= intentosPrimero) {
            // Paso 2: el rival ya no puede terminar con menos intentos (le falta al menos uno más)
            ganador = terminoPrimero;
        } else {
            return false; // Paso 3: el rival sigue jugando solo
        }
        finalizarPartida();
        return true;
    }

    // Revisa si un jugador ya encontró las 18 parejas de su tablero
    public boolean todasLasParejasEncontradas(int numeroJugador) {
        if (numeroJugador == 1) {
            return tablero1.todasLasParejasEncontradas();
        } else if (numeroJugador == 2) {
            return tablero2.todasLasParejasEncontradas();
        }
        return false; // número de jugador no válido
    }

    // Cuenta cuántas parejas lleva un jugador
    public int contarParejas(int numeroJugador) {
        if (numeroJugador == 1) {
            return tablero1.contarParejasEncontradas();
        } else if (numeroJugador == 2) {
            return tablero2.contarParejasEncontradas();
        }
        return 0;
    }

    // true si la partida ya tiene ganador
    public boolean hayGanador() {
        return ganador == 1 || ganador == 2;
    }

    // Termina la partida y le suma una partida ganada al ganador
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





