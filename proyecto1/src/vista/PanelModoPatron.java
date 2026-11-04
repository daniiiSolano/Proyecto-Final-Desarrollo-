package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;
import modelo.ModoPatron;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Locale;

public class PanelModoPatron extends PanelConFondo {

    private static final String[] INSTRUCCIONES = {
            "Observa el patrón que se mostrará al inicio.",
            "Recuerda el orden de las casillas.",
            "Presiona las mismas casillas en el mismo orden.",
            "Si lo logras, pasarás al siguiente nivel con un patrón más largo.",
            "Si fallas, el juego termina."
    };

    // Tiempos de la animación del patrón (milisegundos)
    private static final int ESPERA_ANTES_DE_MOSTRAR_MS = 800;
    private static final int CASILLA_ENCENDIDA_MS = 700;
    private static final int PAUSA_ENTRE_CASILLAS_MS = 300;
    private static final int DESTELLO_CLIC_MS = 250;
    private static final int PAUSA_ENTRE_NIVELES_MS = 1200;
    private static final int INTERVALO_CRONOMETRO_MS = 50;

    private static final String TEXTO_INICIAL = "Memoriza el patrón de casillas y repítelo en el mismo orden.";

    private final JuegoController controller;

    private PanelTablero tablero;
    private JLabel lblEstado;
    private JLabel lblNivel;
    private JLabel lblLongitud;
    private JLabel lblProgreso;
    private JProgressBar barraTotal;
    private JLabel lblTiempoTotal;
    private JProgressBar barraEntre;
    private JLabel lblTiempoEntre;
    private Recursos.BotonBosque btnIniciar;

    // Temporizadores de Swing: corren en el hilo de eventos, así que pueden tocar la interfaz sin problemas
    private Timer reproductor;     // muestra el patrón casilla por casilla
    private Timer cronometro;      // descuenta los 12 s y los 2 s mientras el jugador responde
    private Timer pausaNivel;      // espera breve antes de mostrar el patrón del siguiente nivel
    private long ultimoTick;
    private boolean turnoJugador = false;

    public PanelModoPatron(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();

        // CardLayout oculta este panel cuando se cambia de pestaña: en ese momento se detiene la partida
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                detenerPartida();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(4, 30, 8, 30));

        JPanel norte = new JPanel(new GridBagLayout());
        norte.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        norte.add(Recursos.crearBanner("Modo patrones", 36), gc);
        gc.gridy = 1;
        gc.insets = new Insets(6, 0, 0, 0);
        PanelRedondeado subtitulo = Recursos.crearSubtitulo(TEXTO_INICIAL);
        lblEstado = (JLabel) subtitulo.getComponent(0); // se usa para mostrar mensajes del juego
        norte.add(subtitulo, gc);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.insets = new Insets(6, 8, 4, 8);

        c.gridx = 0;
        c.weightx = 0;
        centro.add(crearPanelIzquierdo(), c);

        tablero = new PanelTablero(controller.getFilasTablero(), controller.getColumnasTablero(),
                Recursos.VERDE);
        tablero.setImagenBase(Recursos.CASILLA_PATRON);
        tablero.setImagenAlterna(Recursos.CASILLA_PATRON_ACTIVA);
        tablero.setOyenteCasillas(this::casillaPresionada);
        c.gridx = 1;
        c.weightx = 1;
        centro.add(tablero, c);

        c.gridx = 2;
        c.weightx = 0;
        centro.add(crearPanelInstrucciones(), c);
        add(centro, BorderLayout.CENTER);

        btnIniciar = Recursos.crearBoton("Iniciar partida");
        btnIniciar.setFont(Recursos.fuente(Font.BOLD, 24));
        btnIniciar.addActionListener(evt -> iniciarPartida());

        Recursos.BotonBosque btnVolver = Recursos.crearBoton("Volver al menú", Recursos.CAFE);
        btnVolver.setFont(Recursos.fuente(Font.BOLD, 24));
        btnVolver.addActionListener(evt -> {
            detenerPartida();
            controller.volverAlMenu();
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        botones.setOpaque(false);
        botones.add(btnIniciar);
        botones.add(btnVolver);
        add(botones, BorderLayout.SOUTH);
    }

    // =================================================================== flujo del juego

    /** Botón "Iniciar partida" / "Jugar de nuevo". */
    private void iniciarPartida() {
        detenerTemporizadores();
        controller.iniciarPartidaPatron();
        btnIniciar.setText("Reiniciar");
        actualizarDatosNivel();
        mostrarPatron();
    }

    /** Enciende una por una las casillas del patrón. Mientras tanto no se aceptan clics. */
    private void mostrarPatron() {
        turnoJugador = false;
        tablero.mostrarFrente(false); // apaga todas las casillas
        reiniciarBarras();
        lblEstado.setText("Nivel " + controller.getNivelPatron() + ": observa el patrón...");

        final List<int[]> secuencia = controller.getSecuenciaPatron();
        final int[] indice = {0};
        final boolean[] encendida = {false};

        reproductor = new Timer(CASILLA_ENCENDIDA_MS, null);
        reproductor.setInitialDelay(ESPERA_ANTES_DE_MOSTRAR_MS);
        reproductor.addActionListener(e -> {
            int[] casilla = secuencia.get(indice[0]);
            if (!encendida[0]) {
                // Enciende la casilla actual
                tablero.marcarCasilla(casilla[0], casilla[1], true);
                encendida[0] = true;
                reproductor.setDelay(CASILLA_ENCENDIDA_MS);
            } else {
                // La apaga y pasa a la siguiente
                tablero.marcarCasilla(casilla[0], casilla[1], false);
                encendida[0] = false;
                indice[0]++;
                if (indice[0] >= secuencia.size()) {
                    reproductor.stop();
                    comenzarTurnoJugador();
                } else {
                    reproductor.setDelay(PAUSA_ENTRE_CASILLAS_MS);
                }
            }
        });
        reproductor.start();
    }

    /** Terminó de mostrarse el patrón: el jugador tiene 12 s en total y 2 s entre casillas. */
    private void comenzarTurnoJugador() {
        controller.comenzarTurnoPatron();
        turnoJugador = true;
        lblEstado.setText("¡Tu turno! Repite el patrón.");
        actualizarProgreso();
        actualizarBarras();

        ultimoTick = System.nanoTime();
        cronometro = new Timer(INTERVALO_CRONOMETRO_MS, e -> {
            long ahora = System.nanoTime();
            long transcurrido = (ahora - ultimoTick) / 1_000_000;
            ultimoTick = ahora;
            boolean seAcaboElTiempo = controller.actualizarTiempoPatron(transcurrido);
            actualizarBarras();
            if (seAcaboElTiempo) {
                terminarPartida();
            }
        });
        cronometro.start();
    }

    /** Oyente del tablero: se llama cada vez que el jugador presiona una casilla. */
    private void casillaPresionada(int fila, int columna) {
        if (!turnoJugador) {
            return; // se ignoran los clics mientras se muestra el patrón o si no hay partida
        }
        // Descuenta el tiempo transcurrido hasta este clic antes de verificarlo
        long ahora = System.nanoTime();
        if (controller.actualizarTiempoPatron((ahora - ultimoTick) / 1_000_000)) {
            ultimoTick = ahora;
            terminarPartida();
            return;
        }
        ultimoTick = ahora;

        ModoPatron.ResultadoPaso resultado = controller.seleccionarCasillaPatron(fila, columna);
        switch (resultado) {
            case CORRECTO:
                destellar(fila, columna);
                actualizarProgreso();
                actualizarBarras();
                break;
            case NIVEL_COMPLETADO:
                turnoJugador = false;
                detenerCronometro();
                destellar(fila, columna);
                lblEstado.setText("¡Muy bien! Pasas al nivel " + controller.getNivelPatron() + ".");
                actualizarDatosNivel();
                pausaNivel = new Timer(PAUSA_ENTRE_NIVELES_MS, e -> mostrarPatron());
                pausaNivel.setRepeats(false);
                pausaNivel.start();
                break;
            case INCORRECTO:
                terminarPartida();
                break;
            default: // IGNORADO
                break;
        }
    }

    /** Fin de la partida por error o por tiempo. */
    private void terminarPartida() {
        turnoJugador = false;
        detenerTemporizadores();
        actualizarBarras();

        // Se enciende la casilla que tocaba para que el jugador vea en qué se equivocó
        tablero.mostrarFrente(false);
        int[] esperado = controller.getPasoEsperadoPatron();
        if (esperado != null) {
            tablero.marcarCasilla(esperado[0], esperado[1], true);
        }

        int superados = controller.getNivelesSuperadosPatron();
        lblEstado.setText("Fin del juego. Niveles superados: " + superados);
        btnIniciar.setText("Jugar de nuevo");

        String mensaje = controller.getMotivoFinPatron()
                + "\n\nLlegaste al nivel " + controller.getNivelPatron()
                + " (patrón de " + controller.getLongitudPatron() + " casillas)."
                + "\nNiveles superados: " + superados
                + "\nMejor resultado de la sesión: " + controller.getMejorNivelPatron() + " niveles"
                + "\n\nLa casilla iluminada es la que debías presionar.";
        // invokeLater para que el tablero se repinte antes de que aparezca el diálogo
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, mensaje,
                "Fin del juego", JOptionPane.INFORMATION_MESSAGE));
    }

    /** Detiene todo y deja la pantalla como al inicio (se usa al salir de la pantalla). */
    private void detenerPartida() {
        turnoJugador = false;
        detenerTemporizadores();
        controller.cancelarPartidaPatron(); // vuelve al nivel 1 (se conserva el mejor resultado)
        tablero.mostrarFrente(false);
        lblEstado.setText(TEXTO_INICIAL);
        btnIniciar.setText("Iniciar partida");
        actualizarDatosNivel();
        reiniciarBarras();
    }

    // =================================================================== utilidades

    private void destellar(int fila, int columna) {
        tablero.marcarCasilla(fila, columna, true);
        Timer apagar = new Timer(DESTELLO_CLIC_MS, e -> tablero.marcarCasilla(fila, columna, false));
        apagar.setRepeats(false);
        apagar.start();
    }

    private void detenerCronometro() {
        if (cronometro != null) {
            cronometro.stop();
            cronometro = null;
        }
    }

    private void detenerTemporizadores() {
        if (reproductor != null) {
            reproductor.stop();
            reproductor = null;
        }
        if (pausaNivel != null) {
            pausaNivel.stop();
            pausaNivel = null;
        }
        detenerCronometro();
    }

    private void actualizarDatosNivel() {
        lblNivel.setText(String.valueOf(controller.getNivelPatron()));
        lblLongitud.setText(controller.getLongitudPatron() + " casillas");
        lblProgreso.setText(" ");
    }

    private void actualizarProgreso() {
        lblProgreso.setText("Llevas " + controller.getPasoActualPatron() + " de "
                + controller.getLongitudPatron());
    }

    private void reiniciarBarras() {
        pintarBarra(barraTotal, lblTiempoTotal, controller.getTiempoTotalPatron() * 1000L);
        pintarBarra(barraEntre, lblTiempoEntre, controller.getTiempoEntreCasillas() * 1000L);
    }

    private void actualizarBarras() {
        pintarBarra(barraTotal, lblTiempoTotal, controller.getTiempoRestantePatronMs());
        pintarBarra(barraEntre, lblTiempoEntre, controller.getTiempoEntreCasillasRestanteMs());
    }

    private void pintarBarra(JProgressBar barra, JLabel etiqueta, long restanteMs) {
        barra.setValue((int) restanteMs);
        boolean poco = restanteMs <= barra.getMaximum() * 0.3;
        barra.setForeground(poco ? Recursos.ROJO : Recursos.VERDE);
        etiqueta.setText(String.format(Locale.US, "%.1f segundos", restanteMs / 1000.0));
    }

    // =================================================================== construcción de la vista

    /** Nivel actual, longitud del patrón y los dos indicadores de tiempo. */
    private JPanel crearPanelIzquierdo() {
        PanelRedondeado panel = new PanelRedondeado(new GridBagLayout(), Recursos.CREMA, Recursos.CAFE, 30);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        panel.setPreferredSize(new Dimension(360, 100));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(2, 0, 2, 0);
        int fila = 0;

        gc.gridy = fila++;
        panel.add(centrada("Nivel actual", 22, true), gc);
        PanelRedondeado circulo = new PanelRedondeado(new GridBagLayout(), Recursos.CAFE, Recursos.CAFE_OSCURO, 90);
        Dimension tamCirculo = new Dimension(58, 58);
        circulo.setPreferredSize(tamCirculo);
        circulo.setMinimumSize(tamCirculo);   // evita que el círculo se encoja y corte el número
        lblNivel = Recursos.crearEtiqueta(String.valueOf(controller.getNivelPatron()), 30, true, Recursos.CREMA);
        circulo.add(lblNivel);
        JPanel envoltorio = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        envoltorio.setOpaque(false);
        envoltorio.add(circulo);
        gc.gridy = fila++;
        panel.add(envoltorio, gc);

        gc.gridy = fila++;
        gc.insets = new Insets(12, 0, 2, 0);
        panel.add(centrada("Longitud del patrón", 20, true), gc);
        gc.gridy = fila++;
        gc.insets = new Insets(0, 0, 2, 0);
        lblLongitud = centrada(controller.getLongitudPatron() + " casillas", 24, false);
        panel.add(lblLongitud, gc);
        gc.gridy = fila++;
        lblProgreso = centrada(" ", 16, false);
        panel.add(lblProgreso, gc);

        gc.gridy = fila++;
        gc.insets = new Insets(8, 0, 2, 0);
        panel.add(centrada("Tiempo restante", 20, true), gc);
        gc.gridy = fila++;
        gc.insets = new Insets(0, 0, 2, 0);
        barraTotal = crearBarra(controller.getTiempoTotalPatron());
        panel.add(barraTotal, gc);
        gc.gridy = fila++;
        lblTiempoTotal = centrada(controller.getTiempoTotalPatron() + " segundos", 18, false);
        panel.add(lblTiempoTotal, gc);

        gc.gridy = fila++;
        gc.insets = new Insets(12, 0, 2, 0);
        panel.add(centrada("Tiempo entre casillas", 20, true), gc);
        gc.gridy = fila++;
        gc.insets = new Insets(0, 0, 2, 0);
        barraEntre = crearBarra(controller.getTiempoEntreCasillas());
        panel.add(barraEntre, gc);
        gc.gridy = fila++;
        lblTiempoEntre = centrada(controller.getTiempoEntreCasillas() + " segundos", 18, false);
        panel.add(lblTiempoEntre, gc);

        gc.gridy = fila;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        panel.add(relleno, gc);
        return panel;
    }

    private JPanel crearPanelInstrucciones() {
        PanelRedondeado panel = new PanelRedondeado(new BorderLayout(0, 10), Recursos.CREMA, Recursos.CAFE, 30);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setPreferredSize(new Dimension(360, 100));
        panel.add(centrada("Instrucciones", 26, true), BorderLayout.NORTH);

        JPanel lista = new JPanel(new GridLayout(INSTRUCCIONES.length, 1, 0, 6));
        lista.setOpaque(false);
        for (int i = 0; i < INSTRUCCIONES.length; i++) {
            JPanel fila = new JPanel(new BorderLayout(10, 0));
            fila.setOpaque(false);

            PanelRedondeado numero = new PanelRedondeado(new GridBagLayout(), Recursos.VERDE,
                    Recursos.VERDE_OSCURO, 44);
            numero.setPreferredSize(new Dimension(40, 40));
            numero.add(Recursos.crearEtiqueta(String.valueOf(i + 1), 20, true, Recursos.CREMA));
            JPanel marco = new JPanel(new GridBagLayout());
            marco.setOpaque(false);
            marco.add(numero);
            fila.add(marco, BorderLayout.WEST);

            JTextArea texto = new JTextArea(INSTRUCCIONES[i]);
            texto.setEditable(false);
            texto.setFocusable(false);
            texto.setOpaque(false);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            texto.setFont(Recursos.fuente(Font.PLAIN, 18));
            texto.setForeground(Recursos.TEXTO);
            // Se centra verticalmente respecto al círculo numerado
            JPanel marcoTexto = new JPanel(new GridBagLayout());
            marcoTexto.setOpaque(false);
            GridBagConstraints gt = new GridBagConstraints();
            gt.weightx = 1;
            gt.fill = GridBagConstraints.HORIZONTAL;
            marcoTexto.add(texto, gt);
            fila.add(marcoTexto, BorderLayout.CENTER);
            lista.add(fila);
        }
        panel.add(lista, BorderLayout.CENTER);
        return panel;
    }

    private JLabel centrada(String texto, int tamano, boolean negrita) {
        JLabel etiqueta = Recursos.crearEtiqueta(texto, tamano, negrita, Recursos.TEXTO);
        etiqueta.setHorizontalAlignment(JLabel.CENTER);
        return etiqueta;
    }

    /** Barra de tiempo; trabaja en milisegundos para que avance de forma fluida. */
    private JProgressBar crearBarra(int maximoSegundos) {
        JProgressBar barra = new JProgressBar(0, maximoSegundos * 1000);
        barra.setValue(maximoSegundos * 1000);
        barra.setForeground(Recursos.VERDE);
        barra.setBackground(Recursos.CREMA_OSCURO);
        barra.setBorder(BorderFactory.createLineBorder(Recursos.CAFE, 2));
        barra.setPreferredSize(new Dimension(100, 22));
        return barra;
    }
}

