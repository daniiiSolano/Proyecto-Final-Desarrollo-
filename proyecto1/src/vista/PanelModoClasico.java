package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;
import modelo.ModoClasico;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class PanelModoClasico extends PanelConFondo {

    private static final int MOSTRAR_FALLO_MS = 1000;      // tiempo que se ven las 2 cartas del fallo
    private static final int INTERVALO_CRONOMETRO_MS = 100;
    private static final String TEXTO_INICIAL = "¡Encuentra todas las parejas!";

    private final JuegoController controller;
    private final PanelTablero[] tableros = new PanelTablero[2];
    private final JLabel[] lblIntentos = new JLabel[2];
    private final JLabel[] lblRelojes = new JLabel[2];
    private JLabel lblEstado;
    private JLabel lblTurno;
    private JLabel lblJugadorTurno;
    private Recursos.BotonBosque btnIniciar;

    // Temporizadores de Swing (corren en el hilo de eventos, pueden tocar la interfaz)
    private Timer cronometro;   // descuenta los 10 s de la jugada
    private Timer pausaFallo;   // deja ver las dos cartas del fallo antes de voltearlas
    private long ultimoTick;

    public PanelModoClasico(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();
        refrescarTodo();

        // CardLayout oculta este panel al cambiar de pestaña: se detiene la partida
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
        norte.add(Recursos.crearBanner("Modo clásico", 36), gc);
        gc.gridy = 1;
        gc.insets = new Insets(6, 0, 0, 0);
        PanelRedondeado subtitulo = Recursos.crearSubtitulo(TEXTO_INICIAL);
        lblEstado = (JLabel) subtitulo.getComponent(0); // se usa para los mensajes del juego
        norte.add(subtitulo, gc);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.insets = new Insets(4, 0, 2, 0);

        c.gridx = 0;
        c.weightx = 1;
        centro.add(crearColumnaJugador(1), c);
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(4, 14, 2, 14);
        centro.add(crearColumnaCentral(), c);
        c.gridx = 2;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 2, 0);
        centro.add(crearColumnaJugador(2), c);
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

    private void iniciarPartida() {
        detenerTemporizadores();
        controller.iniciarPartidaClasico();
        // Cada partida trae tableros aleatorios nuevos: se recargan las imágenes
        for (int j = 1; j <= 2; j++) {
            tableros[j - 1].setImagenesFrente(controller.getImagenesTableroClasico(j));
        }
        btnIniciar.setText("Reiniciar");
        refrescarTodo();
        iniciarCronometro();
    }

    private void iniciarCronometro() {
        ultimoTick = System.nanoTime();
        cronometro = new Timer(INTERVALO_CRONOMETRO_MS, e -> {
            long ahora = System.nanoTime();
            long transcurrido = (ahora - ultimoTick) / 1_000_000;
            ultimoTick = ahora;
            boolean seAcabo = controller.actualizarTiempoClasico(transcurrido);
            if (seAcabo) {
                if (controller.isPartidaClasicoFinalizada()) {
                    terminarPartida();
                    return;
                }
                lblEstado.setText("¡Se acabó el tiempo! Turno de " + nombre(controller.getTurnoClasico()) + ".");
            }
            refrescarTodo();
        });
        cronometro.start();
    }

    /** Oyente de cada tablero: el jugador {@code numero} presionó una de sus cartas. */
    private void casillaPresionada(int numero, int fila, int columna) {
        ModoClasico.ResultadoJugada resultado = controller.seleccionarCasillaClasico(numero, fila, columna);
        switch (resultado) {
            case PRIMERA_CARTA:
                lblEstado.setText(nombre(numero) + ": elige la segunda carta.");
                break;
            case ACIERTO:
                lblEstado.setText("¡Pareja! +" + ModoClasico.BONO_ACIERTO_SEG + " segundos para "
                        + nombre(numero) + ". ¡Sigue jugando!");
                break;
            case FALLO:
                lblEstado.setText("No son pareja. Turno de " + nombre(numero == 1 ? 2 : 1) + ".");
                pausaFallo = new Timer(MOSTRAR_FALLO_MS, e -> {
                    boolean termino = controller.resolverFalloClasico();
                    ultimoTick = System.nanoTime(); // el reloj estuvo en pausa mientras se veían las cartas
                    if (termino) {
                        terminarPartida();
                    } else {
                        refrescarTodo();
                    }
                });
                pausaFallo.setRepeats(false);
                pausaFallo.start();
                break;
            case FIN_PARTIDA:
                terminarPartida();
                return;
            default: // IGNORADO
                return;
        }
        refrescarTodo();
    }

    /** Termina la partida: la animación de victoria la lanza el controlador automáticamente. */
    private void terminarPartida() {
        detenerTemporizadores();
        refrescarTodo();
        btnIniciar.setText("Jugar de nuevo");

        int ganador = controller.getGanadorClasico();
        if (ganador == 1 || ganador == 2) {
            lblEstado.setText("¡Ganó " + nombre(ganador) + "! Encontró las 18 parejas en "
                    + controller.getIntentosJugador(ganador) + " intentos.");
            lblTurno.setText("Ganador:");
            lblJugadorTurno.setText(nombre(ganador));
        }
    }

    /** Detiene todo y deja la pantalla como al inicio (al salir de la pantalla). */
    private void detenerPartida() {
        detenerTemporizadores();
        controller.cancelarPartidaClasico();
        for (int j = 1; j <= 2; j++) {
            tableros[j - 1].setImagenesFrente(controller.getImagenesTableroClasico(j));
        }
        btnIniciar.setText("Iniciar partida");
        lblEstado.setText(TEXTO_INICIAL);
        refrescarTodo();
    }

    // =================================================================== utilidades

    /** Pone toda la pantalla de acuerdo con el estado del modelo. */
    private void refrescarTodo() {
        boolean enCurso = controller.isPartidaClasicoEnCurso();
        int turno = controller.getTurnoClasico();
        for (int j = 1; j <= 2; j++) {
            tableros[j - 1].mostrarCasillas(controller.getCasillasDescubiertasClasico(j));
            String texto = "Intentos: " + controller.getIntentosJugador(j)
                    + "  ·  Parejas: " + controller.getParejasJugador(j);
            lblIntentos[j - 1].setText(texto);
            boolean esSuTurno = enCurso && turno == j;
            lblRelojes[j - 1].setText(esSuTurno ? controller.getTextoTiempoClasico()
                    : controller.getTextoTiempoInicial());
        }
        if (enCurso) {
            lblTurno.setText("Turno de:");
            lblJugadorTurno.setText(nombre(turno));
        } else if (!controller.isPartidaClasicoFinalizada()) {
            lblTurno.setText("Turno de:");
            lblJugadorTurno.setText("—");
        }
    }

    private void detenerTemporizadores() {
        if (cronometro != null) {
            cronometro.stop();
            cronometro = null;
        }
        if (pausaFallo != null) {
            pausaFallo.stop();
            pausaFallo = null;
        }
    }

    private String nombre(int numero) {
        return controller.getNombreJugador(numero);
    }

    // =================================================================== construcción de la vista

    private JPanel crearColumnaJugador(int numero) {
        JPanel columna = new JPanel(new BorderLayout(0, 4));
        columna.setOpaque(false);

        // El panel del jugador es más angosto y queda centrado sobre su tablero
        JPanel envolturaJugador = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        envolturaJugador.setOpaque(false);
        envolturaJugador.add(crearPanelJugador(numero));
        columna.add(envolturaJugador, BorderLayout.NORTH);

        final PanelTablero tablero = new PanelTablero(controller.getFilasTablero(),
                controller.getColumnasTablero(), Recursos.VERDE);
        tablero.setImagenBase(Recursos.CARTA_REVERSO);
        tablero.setImagenesFrente(controller.getImagenesTableroClasico(numero));
        tablero.setOyenteCasillas((f, col) -> casillaPresionada(numero, f, col));
        tableros[numero - 1] = tablero;
        columna.add(tablero, BorderLayout.CENTER);
        return columna;
    }

    private JPanel crearPanelJugador(int numero) {
        PanelRedondeado panel = new PanelRedondeado(new BorderLayout(14, 0), Recursos.CREMA, Recursos.CAFE, 30);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        panel.setPreferredSize(new Dimension(450, 78));

        JLabel avatar = new JLabel(Recursos.cargarImagen(Recursos.DUENDES[numero == 1 ? 0 : 1], 56, 56,
                Recursos.CREMA_OSCURO));
        panel.add(avatar, BorderLayout.WEST);

        JPanel datos = new JPanel(new GridLayout(2, 1));
        datos.setOpaque(false);
        datos.add(Recursos.crearEtiqueta(controller.getNombreJugador(numero), 24, true, Recursos.TEXTO));
        lblIntentos[numero - 1] = Recursos.crearEtiqueta("Intentos: " + controller.getIntentosJugador(numero),
                15, false, Recursos.TEXTO);
        datos.add(lblIntentos[numero - 1]);
        panel.add(datos, BorderLayout.CENTER);

        PanelRedondeado tiempo = new PanelRedondeado(new GridLayout(2, 1),
                numero == 1 ? Recursos.VERDE : Recursos.ROJO,
                Recursos.CAFE_OSCURO, 20);
        tiempo.setBorder(BorderFactory.createEmptyBorder(1, 12, 3, 12));
        JLabel lblTiempo = Recursos.crearEtiqueta("Tiempo:", 15, false, Recursos.CREMA);
        lblTiempo.setHorizontalAlignment(JLabel.CENTER);
        JLabel lblReloj = Recursos.crearEtiqueta(controller.getTextoTiempoInicial(), 26, true, Recursos.CREMA);
        lblReloj.setHorizontalAlignment(JLabel.CENTER);
        lblRelojes[numero - 1] = lblReloj;
        tiempo.add(lblTiempo);
        tiempo.add(lblReloj);
        panel.add(tiempo, BorderLayout.EAST);
        return panel;
    }

    private JPanel crearColumnaCentral() {
        JPanel central = new JPanel(new GridBagLayout());
        central.setOpaque(false);
        central.setPreferredSize(new Dimension(190, 100));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weighty = 1;

        PanelRedondeado vs = new PanelRedondeado(new GridBagLayout(), Recursos.CREMA, Recursos.CAFE, 200);
        vs.setPreferredSize(new Dimension(120, 120));
        vs.add(Recursos.crearEtiqueta("VS", 44, true, Recursos.CAFE_OSCURO));
        gc.gridy = 0;
        central.add(vs, gc);

        PanelRedondeado turno = new PanelRedondeado(new GridLayout(2, 1), Recursos.CREMA, Recursos.CAFE, 24);
        turno.setBorder(BorderFactory.createEmptyBorder(8, 14, 10, 14));
        lblTurno = Recursos.crearEtiqueta("Turno de:", 20, false, Recursos.TEXTO);
        lblTurno.setHorizontalAlignment(JLabel.CENTER);
        lblJugadorTurno = Recursos.crearEtiqueta(controller.getTextoTurno(), 24, true, Recursos.VERDE_OSCURO);
        lblJugadorTurno.setHorizontalAlignment(JLabel.CENTER);
        turno.add(lblTurno);
        turno.add(lblJugadorTurno);
        gc.gridy = 1;
        central.add(turno, gc);
        return central;
    }
}

