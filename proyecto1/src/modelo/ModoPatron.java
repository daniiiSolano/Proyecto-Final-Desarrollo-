package modelo;

import java.util.ArrayList;
import java.util.Random;

// Clase que tiene las reglas del modo patrones (un solo jugador).
// No usa nada de Swing: la pantalla (PanelModoPatron) le pasa las casillas que se presionan
// y le avisa cuánto tiempo ha pasado, y esta clase decide si el jugador acierta, sube de nivel o pierde.
public class ModoPatron {

    // Constantes del enunciado
    public static final int LONGITUD_INICIAL = 3;           // el primer patrón tiene 3 casillas
    public static final int TIEMPO_TOTAL_SEG = 12;          // tiempo total para repetir el patrón
    public static final int TIEMPO_ENTRE_CASILLAS_SEG = 2;  // máximo entre una casilla y otra

    // Los mismos tiempos en milisegundos (el reloj se descuenta en ms para que sea más exacto)
    public static final long TIEMPO_TOTAL_MS = TIEMPO_TOTAL_SEG * 1000L;
    public static final long TIEMPO_ENTRE_CASILLAS_MS = TIEMPO_ENTRE_CASILLAS_SEG * 1000L;

    // Etapas de una partida. Sirve para saber si se aceptan clics o no
    public enum Fase {
        SIN_INICIAR,      // todavía no se presiona "Iniciar partida"
        MOSTRANDO_PATRON, // se está mostrando la secuencia, los clics no cuentan
        TURNO_JUGADOR,    // el jugador está repitiendo el patrón y corren los tiempos
        FINALIZADA        // el jugador falló o se le acabó el tiempo
    }

    // Posibles resultados de presionar una casilla
    public enum ResultadoPaso {
        CORRECTO,         // casilla correcta, pero todavía faltan casillas del patrón
        NIVEL_COMPLETADO, // casilla correcta y era la última: sube de nivel
        INCORRECTO,       // casilla equivocada: termina la partida
        IGNORADO          // el clic llegó cuando no era el turno del jugador
    }

    private TableroMatriz tablero;
    private int nivel;
    private boolean juegoFinalizado;
    private ArrayList<int[]> secuenciaCorrecta; // lista con las coordenadas {fila, columna} del patrón
    private int pasoActual;                     // posición del patrón que toca presionar

    private final Random random = new Random();
    private Fase fase = Fase.SIN_INICIAR;
    private long tiempoRestanteMs = TIEMPO_TOTAL_MS;                      // cuenta regresiva de 12 s
    private long tiempoEntreCasillasRestanteMs = TIEMPO_ENTRE_CASILLAS_MS; // cuenta de 2 s
    private String motivoFin = "";  // por qué perdió (se muestra en el mensaje final)
    private int mejorNivel = 0;     // mayor cantidad de niveles superados en la sesión

    // ---------------------------------------------------------------- constructores

    public ModoPatron(TableroMatriz tablero, int nivel, boolean juegoFinalizado,
                      ArrayList<int[]> secuenciaCorrecta, int pasoActual) {
        this.tablero = tablero;
        this.nivel = nivel;
        this.juegoFinalizado = juegoFinalizado;
        this.secuenciaCorrecta = secuenciaCorrecta;
        this.pasoActual = pasoActual;
    }

    // Constructor que usa el controlador: empieza en el nivel 1 con la lista vacía
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

    // Devuelve una copia de la secuencia para que la vista no pueda cambiar la original
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

    // Niveles que completó en la partida actual (si está en el nivel 4, superó 3)
    public int getNivelesSuperados() {
        return nivel - 1;
    }

    public int getMejorNivel() {
        return mejorNivel;
    }

    // Cantidad de casillas del patrón: nivel 1 = 3, nivel 2 = 4, nivel 3 = 5...
    public int getLongitudPatron() {
        return nivel + LONGITUD_INICIAL - 1;
    }

    // Casilla que el jugador tenía que presionar (se usa para enseñarla cuando pierde)
    public int[] getPasoEsperado() {
        if (secuenciaCorrecta.isEmpty() || pasoActual >= secuenciaCorrecta.size()) {
            return null;
        }
        int[] paso = secuenciaCorrecta.get(pasoActual);
        return new int[]{paso[0], paso[1]};
    }

    // ---------------------------------------------------------------- lógica

    // Empieza una partida nueva desde el nivel 1 (patrón de 3 casillas)
    public void iniciarPartida() {
        this.nivel = 1;
        this.juegoFinalizado = false;
        this.motivoFin = "";
        this.secuenciaCorrecta.clear();
        generarSecuencia();
    }

    // Genera un patrón aleatorio con la cantidad de casillas del nivel actual.
    // El do-while evita que la misma casilla salga dos veces seguidas,
    // porque al mostrarse se vería como si fuera una sola
    public void generarSecuencia() {
        secuenciaCorrecta.clear();
        int cantidadPasos = getLongitudPatron();
        int[] anterior = null;
        for (int i = 0; i < cantidadPasos; i++) {
            int fila;
            int columna;
            do {
                fila = random.nextInt(TableroMatriz.FILAS);       // número entre 0 y 5
                columna = random.nextInt(TableroMatriz.COLUMNAS); // número entre 0 y 5
            } while (anterior != null && anterior[0] == fila && anterior[1] == columna);
            anterior = new int[]{fila, columna};
            secuenciaCorrecta.add(anterior);
        }
        pasoActual = 0;
        fase = Fase.MOSTRANDO_PATRON; // ahora la vista tiene que mostrar el patrón
        reiniciarTiempos();
    }

    // La vista llama a este método cuando termina de mostrar el patrón.
    // Desde aquí empiezan a correr los 12 segundos y los 2 segundos
    public void comenzarTurnoJugador() {
        if (fase != Fase.MOSTRANDO_PATRON) {
            return;
        }
        pasoActual = 0;
        reiniciarTiempos();
        fase = Fase.TURNO_JUGADOR;
    }

    // La vista llama a este método varias veces por segundo con los milisegundos que pasaron.
    // Se descuentan de los dos relojes; devuelve true si alguno llegó a 0 (el jugador pierde)
    public boolean avanzarTiempo(long milisegundos) {
        if (fase != Fase.TURNO_JUGADOR || milisegundos <= 0) {
            return false; // solo cuenta el tiempo durante el turno del jugador
        }
        // Math.max evita que los tiempos queden negativos
        tiempoRestanteMs = Math.max(0, tiempoRestanteMs - milisegundos);
        tiempoEntreCasillasRestanteMs = Math.max(0, tiempoEntreCasillasRestanteMs - milisegundos);

        if (tiempoRestanteMs == 0) { // se acabaron los 12 segundos
            finalizarPartida("Se acabaron los " + TIEMPO_TOTAL_SEG + " segundos para repetir el patrón.");
            return true;
        }
        if (tiempoEntreCasillasRestanteMs == 0) { // pasaron más de 2 segundos sin presionar casilla
            finalizarPartida("Tardaste más de " + TIEMPO_ENTRE_CASILLAS_SEG
                    + " segundos en elegir la siguiente casilla.");
            return true;
        }
        return false;
    }

    // Revisa si la casilla que presionó el jugador es la que sigue en el patrón
    public ResultadoPaso verificarPaso(int filaSeleccionada, int columnaSeleccionada) {
        // Si no es el turno del jugador, el clic no cuenta
        if (fase != Fase.TURNO_JUGADOR || juegoFinalizado || secuenciaCorrecta.isEmpty()) {
            return ResultadoPaso.IGNORADO;
        }

        int[] pasoEsperado = secuenciaCorrecta.get(pasoActual); // casilla que tocaba presionar
        if (pasoEsperado[0] != filaSeleccionada || pasoEsperado[1] != columnaSeleccionada) {
            finalizarPartida("Esa no era la casilla correcta.");
            return ResultadoPaso.INCORRECTO;
        }

        pasoActual++; // acertó: ahora le toca la siguiente casilla del patrón
        tiempoEntreCasillasRestanteMs = TIEMPO_ENTRE_CASILLAS_MS; // los 2 segundos vuelven a empezar

        if (pasoActual >= secuenciaCorrecta.size()) { // ya repitió todo el patrón
            subirNivel();
            return ResultadoPaso.NIVEL_COMPLETADO;
        }
        return ResultadoPaso.CORRECTO;
    }

    // Pasa al siguiente nivel y genera un patrón con una casilla más
    public void subirNivel() {
        this.nivel++;
        mejorNivel = Math.max(mejorNivel, getNivelesSuperados()); // se guarda el mejor resultado
        generarSecuencia(); // la fase queda otra vez en MOSTRANDO_PATRON
    }

    public void finalizarPartida() {
        finalizarPartida("Partida terminada.");
    }

    // Termina la partida y guarda el motivo para mostrarlo en el mensaje final
    public void finalizarPartida(String motivo) {
        this.juegoFinalizado = true;
        this.fase = Fase.FINALIZADA;
        this.motivoFin = motivo;
        mejorNivel = Math.max(mejorNivel, getNivelesSuperados());
    }

    // Detiene la partida sin mostrar mensaje (cuando el jugador sale de la pantalla).
    // El mejor nivel no se borra
    public void cancelarPartida() {
        this.nivel = 1;
        this.juegoFinalizado = false;
        this.motivoFin = "";
        this.secuenciaCorrecta.clear();
        this.pasoActual = 0;
        this.fase = Fase.SIN_INICIAR;
        reiniciarTiempos();
    }

    // Pone los dos relojes otra vez en 12 s y 2 s
    private void reiniciarTiempos() {
        tiempoRestanteMs = TIEMPO_TOTAL_MS;
        tiempoEntreCasillasRestanteMs = TIEMPO_ENTRE_CASILLAS_MS;
    }
}



