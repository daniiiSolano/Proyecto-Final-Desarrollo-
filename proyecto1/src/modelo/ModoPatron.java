package modelo;

import java.util.ArrayList;
import java.util.Random;

/**
 * Lógica del modo patrones (un solo jugador).
 *
 * Reglas (según el enunciado):
 *  - Al inicio el patrón tiene 3 casillas; cada nivel superado agrega 1 casilla.
 *  - Una vez mostrado el patrón, el jugador tiene 12 segundos en total para replicarlo.
 *  - Entre una casilla y la siguiente no pueden pasar más de 2 segundos.
 *  - Si se equivoca o se le acaba alguno de los dos tiempos, la partida termina.
 *
 * El modelo no conoce nada de Swing: la vista le avisa cuánto tiempo ha pasado
 * con {@link #avanzarTiempo(long)} y le pasa las casillas presionadas con
 * {@link #verificarPaso(int, int)}.
 */
public class ModoPatron {

    public static final int LONGITUD_INICIAL = 3;
    public static final int TIEMPO_TOTAL_SEG = 12;
    public static final int TIEMPO_ENTRE_CASILLAS_SEG = 2;

    public static final long TIEMPO_TOTAL_MS = TIEMPO_TOTAL_SEG * 1000L;
    public static final long TIEMPO_ENTRE_CASILLAS_MS = TIEMPO_ENTRE_CASILLAS_SEG * 1000L;

    /** Etapas por las que pasa una partida. */
    public enum Fase {
        SIN_INICIAR,      // todavía no se presiona "Iniciar partida"
        MOSTRANDO_PATRON, // la vista está enseñando la secuencia; no se aceptan clics
        TURNO_JUGADOR,    // el jugador está repitiendo el patrón; corren los tiempos
        FINALIZADA        // el jugador falló o se le acabó el tiempo
    }

    /** Resultado de presionar una casilla. */
    public enum ResultadoPaso {
        CORRECTO,          // casilla correcta, falta completar el patrón
        NIVEL_COMPLETADO,  // casilla correcta y era la última del patrón
        INCORRECTO,        // casilla equivocada: la partida termina
        IGNORADO           // el clic llegó fuera del turno del jugador
    }

    private TableroMatriz tablero;
    private int nivel;
    private boolean juegoFinalizado;
    private ArrayList<int[]> secuenciaCorrecta; // guarda las coordenadas {fila, columna}
    private int pasoActual;                     // en qué paso de la secuencia va

    private final Random random = new Random();
    private Fase fase = Fase.SIN_INICIAR;
    private long tiempoRestanteMs = TIEMPO_TOTAL_MS;
    private long tiempoEntreCasillasRestanteMs = TIEMPO_ENTRE_CASILLAS_MS;
    private String motivoFin = "";
    private int mejorNivel = 0; // mayor cantidad de niveles superados en la sesión

    // ---------------------------------------------------------------- constructores

    public ModoPatron(TableroMatriz tablero, int nivel, boolean juegoFinalizado,
                      ArrayList<int[]> secuenciaCorrecta, int pasoActual) {
        this.tablero = tablero;
        this.nivel = nivel;
        this.juegoFinalizado = juegoFinalizado;
        this.secuenciaCorrecta = secuenciaCorrecta;
        this.pasoActual = pasoActual;
    }

    public ModoPatron() {
        this.nivel = 1;
        this.juegoFinalizado = false;
        this.secuenciaCorrecta = new ArrayList<>();
        this.pasoActual = 0;
    }

    // ---------------------------------------------------------------- getters

    public int getNivel() {
        return nivel;
    }

    public boolean isJuegoFinalizado() {
        return juegoFinalizado;
    }

    public TableroMatriz getTablero() {
        return tablero;
    }

    /** Copia de la secuencia, para que la vista no pueda modificar la original. */
    public ArrayList<int[]> getSecuenciaCorrecta() {
        ArrayList<int[]> copia = new ArrayList<>();
        for (int[] paso : secuenciaCorrecta) {
            copia.add(new int[]{paso[0], paso[1]});
        }
        return copia;
    }

    public int getPasoActual() {
        return pasoActual;
    }

    public Fase getFase() {
        return fase;
    }

    public long getTiempoRestanteMs() {
        return tiempoRestanteMs;
    }

    public long getTiempoEntreCasillasRestanteMs() {
        return tiempoEntreCasillasRestanteMs;
    }

    public String getMotivoFin() {
        return motivoFin;
    }

    /** Niveles completados en la partida actual. */
    public int getNivelesSuperados() {
        return nivel - 1;
    }

    public int getMejorNivel() {
        return mejorNivel;
    }

    public int getLongitudPatron() {
        return nivel + LONGITUD_INICIAL - 1;
    }

    /** Casilla que el jugador debía presionar (útil para mostrarla cuando falla). */
    public int[] getPasoEsperado() {
        if (secuenciaCorrecta.isEmpty() || pasoActual >= secuenciaCorrecta.size()) {
            return null;
        }
        int[] paso = secuenciaCorrecta.get(pasoActual);
        return new int[]{paso[0], paso[1]};
    }

    // ---------------------------------------------------------------- lógica

    /** Reinicia todo y genera el primer patrón (3 casillas). */
    public void iniciarPartida() {
        this.nivel = 1;
        this.juegoFinalizado = false;
        this.motivoFin = "";
        this.secuenciaCorrecta.clear();
        generarSecuencia();
    }

    /**
     * Genera un patrón aleatorio con la longitud del nivel actual. Se evita que
     * la misma casilla salga dos veces seguidas, porque al mostrarse se vería
     * como una sola.
     */
    public void generarSecuencia() {
        secuenciaCorrecta.clear();
        int cantidadPasos = getLongitudPatron();
        int[] anterior = null;
        for (int i = 0; i < cantidadPasos; i++) {
            int fila;
            int columna;
            do {
                fila = random.nextInt(TableroMatriz.FILAS);
                columna = random.nextInt(TableroMatriz.COLUMNAS);
            } while (anterior != null && anterior[0] == fila && anterior[1] == columna);
            anterior = new int[]{fila, columna};
            secuenciaCorrecta.add(anterior);
        }
        pasoActual = 0;
        fase = Fase.MOSTRANDO_PATRON;
        reiniciarTiempos();
    }

    /** La vista terminó de mostrar el patrón: empieza a correr el tiempo del jugador. */
    public void comenzarTurnoJugador() {
        if (fase != Fase.MOSTRANDO_PATRON) {
            return;
        }
        pasoActual = 0;
        reiniciarTiempos();
        fase = Fase.TURNO_JUGADOR;
    }

    /**
     * Descuenta tiempo a los dos cronómetros. La vista lo llama periódicamente.
     *
     * @return true si con este descuento se acabó algún tiempo y la partida terminó.
     */
    public boolean avanzarTiempo(long milisegundos) {
        if (fase != Fase.TURNO_JUGADOR || milisegundos <= 0) {
            return false;
        }
        tiempoRestanteMs = Math.max(0, tiempoRestanteMs - milisegundos);
        tiempoEntreCasillasRestanteMs = Math.max(0, tiempoEntreCasillasRestanteMs - milisegundos);

        if (tiempoRestanteMs == 0) {
            finalizarPartida("Se acabaron los " + TIEMPO_TOTAL_SEG + " segundos para repetir el patrón.");
            return true;
        }
        if (tiempoEntreCasillasRestanteMs == 0) {
            finalizarPartida("Tardaste más de " + TIEMPO_ENTRE_CASILLAS_SEG
                    + " segundos en elegir la siguiente casilla.");
            return true;
        }
        return false;
    }

    /** Verifica si la casilla presionada por el usuario coincide con la secuencia. */
    public ResultadoPaso verificarPaso(int filaSeleccionada, int columnaSeleccionada) {
        if (fase != Fase.TURNO_JUGADOR || juegoFinalizado || secuenciaCorrecta.isEmpty()) {
            return ResultadoPaso.IGNORADO;
        }

        int[] pasoEsperado = secuenciaCorrecta.get(pasoActual);
        if (pasoEsperado[0] != filaSeleccionada || pasoEsperado[1] != columnaSeleccionada) {
            finalizarPartida("Esa no era la casilla correcta.");
            return ResultadoPaso.INCORRECTO;
        }

        pasoActual++;
        tiempoEntreCasillasRestanteMs = TIEMPO_ENTRE_CASILLAS_MS; // se reinicia el tiempo entre casillas

        if (pasoActual >= secuenciaCorrecta.size()) { // completó toda la secuencia del nivel
            subirNivel();
            return ResultadoPaso.NIVEL_COMPLETADO;
        }
        return ResultadoPaso.CORRECTO;
    }

    /** Pasa al siguiente nivel con un patrón 1 casilla más largo. */
    public void subirNivel() {
        this.nivel++;
        mejorNivel = Math.max(mejorNivel, getNivelesSuperados());
        generarSecuencia(); // deja la fase en MOSTRANDO_PATRON
    }

    public void finalizarPartida() {
        finalizarPartida("Partida terminada.");
    }

    public void finalizarPartida(String motivo) {
        this.juegoFinalizado = true;
        this.fase = Fase.FINALIZADA;
        this.motivoFin = motivo;
        mejorNivel = Math.max(mejorNivel, getNivelesSuperados());
    }

    /** Detiene la partida sin mensaje (por ejemplo, cuando el jugador sale de la pantalla). */
    public void cancelarPartida() {
        this.nivel = 1;
        this.juegoFinalizado = false;
        this.motivoFin = "";
        this.secuenciaCorrecta.clear();
        this.pasoActual = 0;
        this.fase = Fase.SIN_INICIAR;
        reiniciarTiempos();
    }

    private void reiniciarTiempos() {
        tiempoRestanteMs = TIEMPO_TOTAL_MS;
        tiempoEntreCasillasRestanteMs = TIEMPO_ENTRE_CASILLAS_MS;
    }
}



