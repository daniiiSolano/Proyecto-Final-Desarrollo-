package vista;

import apoyo.BotonImagen;
import apoyo.CampoTexto;
import apoyo.Imagenes;
import apoyo.PanelConFondo;
import apoyo.PanelImagen;
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

    private static final int ESPERA_ANTES_DE_MOSTRAR_MS = 800;
    private static final int CASILLA_ENCENDIDA_MS = 700;
    private static final int PAUSA_ENTRE_CASILLAS_MS = 300;
    private static final int DESTELLO_CLIC_MS = 250;
    private static final int PAUSA_ENTRE_NIVELES_MS = 1200;
    private static final int INTERVALO_CRONOMETRO_MS = 50;

    private static final int DURACION_DERROTA_MS = 4200;
    private static final int DESVANECER_DERROTA_MS = 450;
    private static final int CUADRO_DERROTA_MS = 30;
    private static final String SONIDO_DERROTA = Recursos.CARPETA_SONIDOS + "perdi\u00f3Juego.mp3";

    private static final String TEXTO_INICIAL = "Memoriza el patrón de casillas y repítelo en el mismo orden.";
    private static final String FONDO_VENTANA_NOMBRES = "clasico/ventana_nombres.png";
    private static final int LARGO_MAXIMO_NOMBRE = 15;

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

    private Timer reproductor;
    private Timer cronometro;
    private Timer pausaNivel;
    private long ultimoTick;
    private boolean turnoJugador = false;

    private CapaDerrota capaDerrota;
    private volatile Player sonidoDerrota;
    private volatile Object turnoSonido;

    public PanelModoPatron(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();

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
        lblEstado = (JLabel) subtitulo.getComponent(0);
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

    private void iniciarPartida() {
        if (!controller.isPartidaPatronEnCurso() && !pedirNombre()) {
            return;
        }
        detenerAnimacionDerrota();
        detenerTemporizadores();
        controller.iniciarPartidaPatron();
        btnIniciar.setText("Reiniciar");
        actualizarDatosNivel();
        mostrarPatron();
    }

    private void mostrarPatron() {
        turnoJugador = false;
        tablero.mostrarFrente(false);
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
                tablero.marcarCasilla(casilla[0], casilla[1], true);
                encendida[0] = true;
                reproductor.setDelay(CASILLA_ENCENDIDA_MS);
            } else {
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

    private void casillaPresionada(int fila, int columna) {
        if (!turnoJugador) {
            return;
        }
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
            default:
                break;
        }
    }

    private void terminarPartida() {
        turnoJugador = false;
        detenerTemporizadores();
        actualizarBarras();

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
        mostrarAnimacionDerrota(superados, () -> Recursos.mostrarAviso(this, "Fin del juego", mensaje));
    }

    private void detenerPartida() {
        detenerAnimacionDerrota();
        turnoJugador = false;
        detenerTemporizadores();
        controller.cancelarPartidaPatron();
        tablero.mostrarFrente(false);
        lblEstado.setText(TEXTO_INICIAL);
        btnIniciar.setText("Iniciar partida");
        actualizarDatosNivel();
        reiniciarBarras();
    }

    private boolean pedirNombre() {
        Window dueno = SwingUtilities.getWindowAncestor(this);
        JDialog ventana = new JDialog(dueno, "Nombre del jugador", Dialog.ModalityType.APPLICATION_MODAL);
        ventana.setUndecorated(true);

        String rutaFondo = Imagenes.cargar(FONDO_VENTANA_NOMBRES) != null ? FONDO_VENTANA_NOMBRES : "fondo.png";
        PanelImagen fondo = new PanelImagen(rutaFondo, new GridBagLayout(), new Dimension(640, 340))
                .respaldo(Recursos.CAFE);
        fondo.setPreferredSize(new Dimension(640, 340));
        fondo.setBorder(BorderFactory.createLineBorder(Recursos.CAFE_OSCURO, 4));

        PanelImagen banner = new PanelImagen(RutasLogin.BANNER, new GridBagLayout(), new Dimension(430, 64))
                .respaldo(Recursos.CAFE);
        banner.setPreferredSize(new Dimension(430, 64));
        banner.add(Recursos.crearEtiqueta("Ingrese su nombre", 30, true, Recursos.CREMA));

        CampoTexto campo = crearCampoNombre();
        PanelImagen tarjeta = new PanelImagen(RutasLogin.TARJETA, new GridBagLayout(), new Dimension(470, 100))
                .respaldo(Recursos.CREMA);
        tarjeta.setPreferredSize(new Dimension(470, 100));
        GridBagConstraints t = new GridBagConstraints();
        t.insets = new Insets(6, 8, 6, 8);
        t.anchor = GridBagConstraints.LINE_END;
        t.gridx = 0;
        t.gridy = 0;
        tarjeta.add(Recursos.crearEtiqueta("Jugador:", 26, true, Recursos.TEXTO), t);
        t.anchor = GridBagConstraints.LINE_START;
        t.gridx = 1;
        tarjeta.add(campo, t);

        BotonImagen btnConfirmar = ComponentesLogin.botonVerde("Confirmar", 26);
        btnConfirmar.setPreferredSize(new Dimension(220, 54));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        fondo.add(banner, gc);
        gc.gridy = 1;
        gc.insets = new Insets(18, 0, 18, 0);
        fondo.add(tarjeta, gc);
        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 0, 0);
        fondo.add(btnConfirmar, gc);

        final boolean[] confirmado = {false};
        btnConfirmar.addActionListener(e -> {
            if (recortar(campo.getText()).isEmpty()) {
                Recursos.mostrarAviso(ventana, "Nombre del jugador", "Escribe tu nombre para empezar.");
                return;
            }
            confirmado[0] = true;
            ventana.dispose();
        });
        ventana.getRootPane().setDefaultButton(btnConfirmar);
        ventana.getRootPane().registerKeyboardAction(e -> ventana.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        ventana.setContentPane(fondo);
        ventana.pack();
        ventana.setLocationRelativeTo(dueno);
        SwingUtilities.invokeLater(campo::requestFocusInWindow);
        ventana.setVisible(true);

        if (confirmado[0]) {
            controller.setNombreJugadorPatron(recortar(campo.getText()));
        }
        return confirmado[0];
    }

    private CampoTexto crearCampoNombre() {
        CampoTexto campo = new CampoTexto(RutasLogin.CAMPO);
        campo.setPreferredSize(new Dimension(250, 48));
        campo.setText(controller.getNombreJugadorPatron());
        campo.setToolTipText("Máximo " + LARGO_MAXIMO_NOMBRE + " letras.");
        return campo;
    }

    private String recortar(String texto) {
        texto = texto.trim();
        return texto.length() > LARGO_MAXIMO_NOMBRE ? texto.substring(0, LARGO_MAXIMO_NOMBRE) : texto;
    }

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

    private void detenerAnimacionDerrota() {
        if (capaDerrota != null) {
            capaDerrota.terminar(false);
        }
        detenerSonidoDerrota();
    }

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
                if (turnoSonido != turno) {
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

    private final class CapaDerrota extends JComponent {

        private static final int HOJAS = 22;

        private final int nivelesSuperados;
        private final Runnable alTerminar;
        private final Timer timer;
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
        private long inicioSalida = -1;
        private float opacidad = 1f;
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
                hojaVelocidad[i] = 0.10f + azar.nextFloat() * 0.12f;
                hojaFase[i] = azar.nextFloat() * 6.28f;
                hojaGiro[i] = (azar.nextFloat() - 0.5f) * 4f;
                hojaTamano[i] = 0.7f + azar.nextFloat() * 0.6f;
                hojaImagen[i] = imagenesHojas[azar.nextInt(imagenesHojas.length)];
            }
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

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            long t = transcurrido();
            opacidad = 1f;
            if (inicioSalida >= 0) {
                opacidad = 1f - Math.min(1f, (t - inicioSalida) / (float) DESVANECER_DERROTA_MS);
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ponerTransparencia(g2, 1f);

            float velo = Math.min(1f, t / 400f);
            g2.setColor(new Color(40, 24, 10, (int) (165 * velo)));
            g2.fillRect(0, 0, w, h);

            dibujarHojas(g2, w, h, t / 1000f);

            double escala = Math.min(w / 900.0, h / 620.0);
            escala = Math.max(0.55, Math.min(1.25, escala));
            int cx = w / 2;
            int yBanner = (int) (h * 0.56);

            double caida = rebote(Math.min(1.0, Math.max(0.0, (t - 150) / 850.0)));
            double yDuende = -200 * escala + (yBanner - 100 * escala + 200 * escala) * caida;
            double giro = 0;
            if (t > 1000) {
                double s = (t - 1000) / 1000.0;
                giro = Math.sin(s * 9) * 0.10 * Math.exp(-s * 0.9) + Math.sin(s * 2.2) * 0.025;
            }
            dibujarDuende(g2, cx, yDuende, escala, giro, t);

            double pop = t < 700 ? 0 : Math.min(1.0, (t - 700) / 380.0);
            if (pop > 0) {
                double s = pop < 0.7 ? pop / 0.7 * 1.12 : 1.12 - (pop - 0.7) / 0.3 * 0.12;
                dibujarBanner(g2, cx, yBanner, escala * s);
            }

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

        private void ponerTransparencia(Graphics2D g2, float alfa) {
            float total = Math.max(0f, Math.min(1f, alfa * opacidad));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, total));
        }

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

        private void dibujarBanner(Graphics2D g2, int cx, int cy, double escala) {
            int ancho = (int) (560 * escala);
            int alto = (int) (84 * escala);
            if (banner != null) {
                g2.drawImage(banner, cx - ancho / 2, cy - alto / 2, ancho, alto, null);
            }
            textoCentrado(g2, "¡Oh no, perdiste!", Recursos.fuente(Font.BOLD, Math.max(12, (int) (46 * escala))),
                    Recursos.CREMA, cx, cy);
        }

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

        private void dibujarDuende(Graphics2D g2, double cx, double cy, double escala, double giro, long t) {
            AffineTransform antes = g2.getTransform();
            g2.translate(cx, cy);
            g2.scale(escala, escala);

            dibujarCapa(g2, duendeManos);

            g2.rotate(giro, 0, 40);
            boolean parpadeo = (t / 120) % 25 == 0;
            dibujarCapa(g2, parpadeo ? duendeParpadeo : duendeTriste);

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

        private void dibujarCapa(Graphics2D g2, BufferedImage capa) {
            if (capa != null) {
                g2.drawImage(capa, -130, -160, 265, 250, null);
            }
        }
    }

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
        circulo.setMinimumSize(tamCirculo);
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