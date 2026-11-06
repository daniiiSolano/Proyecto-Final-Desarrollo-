package vista;

import apoyo.Imagenes;
import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import apoyo.RutasLogin;
import controlador.JuegoController;
import javazoom.jl.player.Player;
import modelo.ModoPatron;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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

    // Animación de derrota: el duende triste cae sobre el tablero mientras caen hojas del bosque
    private static final int DURACION_DERROTA_MS = 4200;
    private static final int DESVANECER_DERROTA_MS = 450;
    private static final int CUADRO_DERROTA_MS = 30;
    /** Sonido de derrota: recursos/sonidos/perdióJuego.mp3 (se escribe con \u00f3 para que no dependa de la codificación). */
    private static final String SONIDO_DERROTA = Recursos.CARPETA_SONIDOS + "perdi\u00f3Juego.mp3";

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

    private CapaDerrota capaDerrota;          // animación de derrota en curso (o null)
    private volatile Player sonidoDerrota;    // reproductor del sonido de derrota (o null)
    private volatile Object turnoSonido;      // identifica la reproducción vigente (para cancelarla a tiempo)

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
        detenerAnimacionDerrota();
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
        // Primero la animación de derrota (con su sonido); al terminar aparece el mensaje de siempre
        mostrarAnimacionDerrota(superados, () -> Recursos.mostrarAviso(this, "Fin del juego", mensaje));
    }

    /** Detiene todo y deja la pantalla como al inicio (se usa al salir de la pantalla). */
    private void detenerPartida() {
        detenerAnimacionDerrota();
        turnoJugador = false;
        detenerTemporizadores();
        controller.cancelarPartidaPatron(); // vuelve al nivel 1 (se conserva el mejor resultado)
        tablero.mostrarFrente(false);
        lblEstado.setText(TEXTO_INICIAL);
        btnIniciar.setText("Iniciar partida");
        actualizarDatosNivel();
        reiniciarBarras();
    }

    // =================================================================== animación de derrota

    /**
     * Muestra la animación de derrota encima de esta pantalla (sin bloquear la aplicación: la
     * mueve un Timer de Swing) y reproduce perdióJuego.mp3. Al terminar, o si el jugador hace
     * clic, se quita la animación y se ejecuta alTerminar (el mensaje "Fin del juego").
     */
    private void mostrarAnimacionDerrota(int nivelesSuperados, Runnable alTerminar) {
        detenerAnimacionDerrota();
        JRootPane raiz = getRootPane();
        if (raiz == null || !isShowing()) {
            SwingUtilities.invokeLater(alTerminar);
            return;
        }
        capaDerrota = new CapaDerrota(nivelesSuperados, alTerminar);
        raiz.getLayeredPane().add(capaDerrota, JLayeredPane.MODAL_LAYER);
        capaDerrota.iniciar();
        reproducirSonidoDerrota();
    }

    /** Quita la animación sin mostrar el mensaje final (al salir de la pantalla o reiniciar). */
    private void detenerAnimacionDerrota() {
        if (capaDerrota != null) {
            capaDerrota.terminar(false);
        }
        detenerSonidoDerrota();
    }

    /** Reproduce una sola vez el sonido de derrota en un hilo aparte. */
    private void reproducirSonidoDerrota() {
        detenerSonidoDerrota();
        File archivo = buscarSonidoDerrota();
        if (archivo == null) {
            System.err.println("No se encontró el sonido de derrota: " + SONIDO_DERROTA);
            return;
        }
        final Object turno = new Object();
        turnoSonido = turno;
        Thread hilo = new Thread(() -> {
            try (BufferedInputStream flujo = new BufferedInputStream(new FileInputStream(archivo))) {
                Player reproductor = new Player(flujo);
                sonidoDerrota = reproductor;
                if (turnoSonido != turno) { // se canceló mientras se abría el archivo
                    reproductor.close();
                    return;
                }
                reproductor.play();
            } catch (Throwable e) {
                System.err.println("No se pudo reproducir el sonido de derrota: " + e);
            }
        }, "HiloSonidoDerrota");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void detenerSonidoDerrota() {
        turnoSonido = null;
        Player reproductor = sonidoDerrota;
        sonidoDerrota = null;
        if (reproductor != null) {
            reproductor.close();
        }
    }

    /**
     * Busca recursos/sonidos/perdióJuego.mp3. Si no está con ese nombre exacto, revisa la carpeta
     * por si la "ó" quedó guardada de otra forma o cambian mayúsculas y minúsculas.
     */
    private static File buscarSonidoDerrota() {
        File archivo = new File(SONIDO_DERROTA);
        if (archivo.exists()) {
            return archivo;
        }
        String buscado = Normalizer.normalize(archivo.getName(), Normalizer.Form.NFC);
        File[] archivos = new File(Recursos.CARPETA_SONIDOS).listFiles();
        if (archivos != null) {
            for (File f : archivos) {
                if (Normalizer.normalize(f.getName(), Normalizer.Form.NFC).equalsIgnoreCase(buscado)) {
                    return f;
                }
            }
        }
        return null;
    }

    /** Capa transparente que se dibuja sobre esta pantalla con la animación del duende triste. */
    private final class CapaDerrota extends JComponent {

        private static final int HOJAS = 22;

        private final int nivelesSuperados;
        private final Runnable alTerminar;
        private final Timer timer;
        // Imágenes de la animación (carpeta recursos/imagenes/derrota/)
        private final BufferedImage banner = Imagenes.cargar(RutasLogin.BANNER);
        private final BufferedImage duendeTriste = Imagenes.cargar("derrota/duende_triste.png");
        private final BufferedImage duendeParpadeo = Imagenes.cargar("derrota/duende_parpadeo.png");
        private final BufferedImage duendeManos = Imagenes.cargar("derrota/duende_manos.png");
        private final BufferedImage lagrima = Imagenes.cargar("derrota/lagrima.png");
        private final BufferedImage[] imagenesHojas = {
                Imagenes.cargar("derrota/hoja_1.png"), Imagenes.cargar("derrota/hoja_2.png"),
                Imagenes.cargar("derrota/hoja_3.png"), Imagenes.cargar("derrota/hoja_4.png"),
                Imagenes.cargar("derrota/hoja_5.png")
        };
        private final float[] hojaX = new float[HOJAS];
        private final float[] hojaY = new float[HOJAS];
        private final float[] hojaVelocidad = new float[HOJAS];
        private final float[] hojaFase = new float[HOJAS];
        private final float[] hojaGiro = new float[HOJAS];
        private final float[] hojaTamano = new float[HOJAS];
        private final BufferedImage[] hojaImagen = new BufferedImage[HOJAS];
        private long inicio;
        private long inicioSalida = -1;   // momento en que empezó a desvanecerse
        private float opacidad = 1f;      // 1 = visible, 0 = invisible (al desvanecerse)
        private boolean terminada = false;

        CapaDerrota(int nivelesSuperados, Runnable alTerminar) {
            this.nivelesSuperados = nivelesSuperados;
            this.alTerminar = alTerminar;
            setOpaque(false);
            setFocusable(true);
            Random azar = new Random();
            for (int i = 0; i < HOJAS; i++) {
                hojaX[i] = azar.nextFloat();
                hojaY[i] = -azar.nextFloat() * 1.2f;
                hojaVelocidad[i] = 0.10f + azar.nextFloat() * 0.12f;   // fracción del alto por segundo
                hojaFase[i] = azar.nextFloat() * 6.28f;
                hojaGiro[i] = (azar.nextFloat() - 0.5f) * 4f;
                hojaTamano[i] = 0.7f + azar.nextFloat() * 0.6f;
                hojaImagen[i] = imagenesHojas[azar.nextInt(imagenesHojas.length)];
            }
            // Mientras se ve la animación no se puede tocar el tablero; un clic la adelanta
            MouseAdapter raton = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (transcurrido() > 700) {
                        salir();
                    }
                }
            };
            addMouseListener(raton);
            addMouseMotionListener(raton);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    e.consume();
                    if ((e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE
                            || e.getKeyCode() == KeyEvent.VK_SPACE) && transcurrido() > 700) {
                        salir();
                    }
                }
            });
            timer = new Timer(CUADRO_DERROTA_MS, e -> avanzar());
        }

        void iniciar() {
            inicio = System.nanoTime();
            ajustarTamano();
            timer.start();
            requestFocusInWindow();
        }

        private long transcurrido() {
            return (System.nanoTime() - inicio) / 1_000_000;
        }

        /** Empieza a desvanecerse (al terminar el tiempo o con un clic). */
        private void salir() {
            if (inicioSalida < 0) {
                inicioSalida = transcurrido();
            }
        }

        private void avanzar() {
            if (!PanelModoPatron.this.isShowing()) {
                terminar(false);
                return;
            }
            long t = transcurrido();
            if (t >= DURACION_DERROTA_MS - DESVANECER_DERROTA_MS) {
                salir();
            }
            if (inicioSalida >= 0 && t - inicioSalida >= DESVANECER_DERROTA_MS) {
                terminar(true);
                return;
            }
            ajustarTamano();
            repaint();
        }

        /** La capa cubre exactamente esta pantalla (también si se cambia el tamaño de la ventana). */
        private void ajustarTamano() {
            Container padre = getParent();
            if (padre == null) {
                return;
            }
            Rectangle zona = SwingUtilities.convertRectangle(PanelModoPatron.this,
                    new Rectangle(0, 0, PanelModoPatron.this.getWidth(), PanelModoPatron.this.getHeight()), padre);
            if (!zona.equals(getBounds())) {
                setBounds(zona);
            }
        }

        /** Quita la capa; si continuar es true se muestra después el mensaje final. */
        void terminar(boolean continuar) {
            if (terminada) {
                return;
            }
            terminada = true;
            timer.stop();
            Container padre = getParent();
            if (padre != null) {
                Rectangle zona = getBounds();
                padre.remove(this);
                padre.repaint(zona.x, zona.y, zona.width, zona.height);
            }
            if (capaDerrota == this) {
                capaDerrota = null;
            }
            if (continuar && alTerminar != null) {
                SwingUtilities.invokeLater(alTerminar);
            }
        }

        // ------------------------------------------------------------- dibujo (con imágenes)

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            long t = transcurrido();   // milisegundos desde que empezó la animación
            opacidad = 1f;
            if (inicioSalida >= 0) {
                opacidad = 1f - Math.min(1f, (t - inicioSalida) / (float) DESVANECER_DERROTA_MS);
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ponerTransparencia(g2, 1f);

            // 1) Velo café oscuro que aparece poco a poco (0.4 s)
            float velo = Math.min(1f, t / 400f);
            g2.setColor(new Color(40, 24, 10, (int) (165 * velo)));
            g2.fillRect(0, 0, w, h);

            // 2) Hojas que caen
            dibujarHojas(g2, w, h, t / 1000f);

            // Tamaño según la ventana (todo se agranda o se achica junto)
            double escala = Math.min(w / 900.0, h / 620.0);
            escala = Math.max(0.55, Math.min(1.25, escala));
            int cx = w / 2;
            int yBanner = (int) (h * 0.56);

            // 3) El duende cae desde arriba con un rebote y luego niega con la cabeza
            double caida = rebote(Math.min(1.0, Math.max(0.0, (t - 150) / 850.0)));
            double yDuende = -200 * escala + (yBanner - 100 * escala + 200 * escala) * caida;
            double giro = 0;
            if (t > 1000) {
                double s = (t - 1000) / 1000.0;
                giro = Math.sin(s * 9) * 0.10 * Math.exp(-s * 0.9) + Math.sin(s * 2.2) * 0.025;
            }
            dibujarDuende(g2, cx, yDuende, escala, giro, t);

            // 4) Banner "¡Oh no, perdiste!" que aparece con un pequeño salto
            double pop = t < 700 ? 0 : Math.min(1.0, (t - 700) / 380.0);
            if (pop > 0) {
                double s = pop < 0.7 ? pop / 0.7 * 1.12 : 1.12 - (pop - 0.7) / 0.3 * 0.12;
                dibujarBanner(g2, cx, yBanner, escala * s);
            }

            // 5) Textos de abajo, que aparecen poco a poco
            if (t > 1100) {
                ponerTransparencia(g2, Math.min(1f, (t - 1100) / 400f));
                String sub = nivelesSuperados == 1 ? "Superaste 1 nivel" : "Niveles superados: " + nivelesSuperados;
                textoCentrado(g2, sub, Recursos.fuente(Font.BOLD, (int) (30 * escala)), Recursos.CREMA,
                        cx, (int) (yBanner + 85 * escala));
                if (t > 1600) {
                    textoCentrado(g2, "Los duendes te esperan para intentarlo otra vez",
                            Recursos.fuente(Font.ITALIC, (int) (22 * escala)), Recursos.CREMA_OSCURO,
                            cx, (int) (yBanner + 125 * escala));
                    Font chica = Recursos.fuente(Font.PLAIN, (int) (18 * escala));
                    int anchoAviso = g2.getFontMetrics(chica).stringWidth("Haz clic para continuar") + (int) (36 * escala);
                    int altoAviso = (int) (32 * escala);
                    int yAviso = (int) (yBanner + 170 * escala);
                    g2.setColor(new Color(40, 24, 10, 200));
                    g2.fill(new RoundRectangle2D.Double(cx - anchoAviso / 2.0, yAviso - altoAviso / 2.0,
                            anchoAviso, altoAviso, altoAviso, altoAviso));
                    textoCentrado(g2, "Haz clic para continuar", chica, Recursos.CREMA, cx, yAviso);
                }
            }
            g2.dispose();
        }

        /** Transparencia de lo que se dibuje después (se combina con el desvanecimiento final). */
        private void ponerTransparencia(Graphics2D g2, float alfa) {
            float total = Math.max(0f, Math.min(1f, alfa * opacidad));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, total));
        }

        /** Curva de caída con rebote (0 → 1). */
        private double rebote(double x) {
            if (x < 1 / 2.75) {
                return 7.5625 * x * x;
            } else if (x < 2 / 2.75) {
                x -= 1.5 / 2.75;
                return 7.5625 * x * x + 0.75;
            } else if (x < 2.5 / 2.75) {
                x -= 2.25 / 2.75;
                return 7.5625 * x * x + 0.9375;
            }
            x -= 2.625 / 2.75;
            return 7.5625 * x * x + 0.984375;
        }

        /** Cada hoja es una imagen que baja, se mece de lado a lado y gira. */
        private void dibujarHojas(Graphics2D g2, int w, int h, float seg) {
            for (int i = 0; i < HOJAS; i++) {
                if (hojaImagen[i] == null) {
                    continue;
                }
                float y = (hojaY[i] + seg * hojaVelocidad[i] * 1.6f) % 1.25f;
                double x = hojaX[i] * w + Math.sin(seg * 1.7 + hojaFase[i]) * 40;
                double py = (y - 0.12) * h;
                int ancho = (int) (36 * hojaTamano[i]);
                int alto = (int) (26 * hojaTamano[i]);
                AffineTransform antes = g2.getTransform();
                g2.translate(x, py);
                g2.rotate(hojaFase[i] + seg * hojaGiro[i]);
                g2.drawImage(hojaImagen[i], -ancho / 2, -alto / 2, ancho, alto, null);
                g2.setTransform(antes);
            }
        }

        /** Imagen del banner café con el texto encima. */
        private void dibujarBanner(Graphics2D g2, int cx, int cy, double escala) {
            int ancho = (int) (560 * escala);
            int alto = (int) (84 * escala);
            if (banner != null) {
                g2.drawImage(banner, cx - ancho / 2, cy - alto / 2, ancho, alto, null);
            }
            textoCentrado(g2, "¡Oh no, perdiste!", Recursos.fuente(Font.BOLD, Math.max(12, (int) (46 * escala))),
                    Recursos.CREMA, cx, cy);
        }

        /** Dibuja un texto centrado en (cx, cy) con una sombra suave. */
        private void textoCentrado(Graphics2D g2, String texto, Font fuente, Color color, int cx, int cy) {
            g2.setFont(fuente);
            FontMetrics fm = g2.getFontMetrics();
            int x = cx - fm.stringWidth(texto) / 2;
            int y = cy + (fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(texto, x + 2, y + 2);
            g2.setColor(color);
            g2.drawString(texto, x, y);
        }

        /**
         * El duende se arma con tres imágenes del mismo tamaño (265 x 250), una encima de otra:
         * las manos (quietas), la cabeza (que gira) y las lágrimas (que resbalan por las mejillas).
         */
        private void dibujarDuende(Graphics2D g2, double cx, double cy, double escala, double giro, long t) {
            AffineTransform antes = g2.getTransform();
            g2.translate(cx, cy);         // (cx, cy) es el centro de la cara
            g2.scale(escala, escala);

            dibujarCapa(g2, duendeManos);                 // las manos no giran

            g2.rotate(giro, 0, 40);                       // la cabeza niega (gira sobre el cuello)
            boolean parpadeo = (t / 120) % 25 == 0;       // de vez en cuando cierra los ojos
            dibujarCapa(g2, parpadeo ? duendeParpadeo : duendeTriste);

            // Lágrimas: bajan desde los ojos y se van haciendo transparentes
            if (t > 1000 && lagrima != null) {
                for (int lado = -1; lado <= 1; lado += 2) {
                    double ciclo = ((t - 1000 + (lado > 0 ? 450 : 0)) % 900) / 900.0;
                    int x = lado * 30 - 8;
                    int y = (int) (18 + ciclo * 46) - 9;
                    ponerTransparencia(g2, (float) (1 - ciclo));
                    g2.drawImage(lagrima, x, y, 16, 18, null);
                }
                ponerTransparencia(g2, 1f);
            }
            g2.setTransform(antes);
        }

        /** Dibuja una capa del duende; (130, 160) de la imagen es el centro de la cara. */
        private void dibujarCapa(Graphics2D g2, BufferedImage capa) {
            if (capa != null) {
                g2.drawImage(capa, -130, -160, 265, 250, null);
            }
        }
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

