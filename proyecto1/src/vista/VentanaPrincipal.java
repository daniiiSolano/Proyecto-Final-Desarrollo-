package vista;

import apoyo.BotonImagen;
import apoyo.Imagenes;
import apoyo.PanelImagen;
import apoyo.Recursos;
import apoyo.RutasLogin;
import controlador.JuegoController;
import hilos.HiloAnimacion;
import hilos.HiloSonido;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class VentanaPrincipal extends JFrame {

    public static final String LOGIN = "login";
    public static final String REGISTRO = "registro";
    public static final String HISTORIA = "historia";
    public static final String CLASICO = "clasico";
    public static final String PATRON = "patron";
    public static final String PREMIOS = "premios";

    // Animación de victoria: los primeros fotogramas son la entrada del trofeo y el resto
    // es un ciclo que se repite VUELTAS_VICTORIA veces (dura lo mismo que la música).
    private static final int FOTOGRAMAS_ENTRADA = 8;
    private static final int VUELTAS_VICTORIA = 2;
    private static final int RETRASO_VICTORIA_MS = 120;
    private static final String SONIDO_VICTORIA = Recursos.CARPETA_SONIDOS + "victoria.mp3";

    // ===== Animación nueva del duende: el duende estampa el sello del rango obtenido =====
    // Todas las imágenes están en recursos/imagenes/rangos/ y miden 1400 x 800 px.
    // Las zonas de texto están medidas sobre ese tamaño y se escalan igual que la imagen.
    private static final String CARPETA_RANGOS = "rangos/";
    private static final int ANCHO_TARJETA = 1400;
    private static final int ALTO_TARJETA = 800;
    private static final Rectangle ZONA_NOMBRE = new Rectangle(1045, 254, 320, 46);
    private static final Rectangle ZONA_INTENTOS = new Rectangle(1225, 480, 140, 48);
    private static final Rectangle ZONA_MENSAJE = new Rectangle(895, 560, 470, 92);
    private static final Rectangle ZONA_BOTON = new Rectangle(940, 676, 320, 72);
    // Tiempo de cada cuadro: el duende con el papel en blanco, el sello cayendo y el golpe
    private static final int[] RETRASOS_SELLO_MS = {600, 90, 90, 450};
    private static final String SONIDO_SELLO = Recursos.CARPETA_SONIDOS + "BOINK.wav";

    private static final String[] PANTALLAS = {LOGIN, HISTORIA, CLASICO, PATRON, PREMIOS};
    private static final String[] NOMBRES_PESTANAS =
            {"Registro y Log In", "Historia", "Modo clasico", "Modo patron", "Premios"};

    private final JuegoController controller;

    private PanelIniciarSesion panelIniciarSesion;
    private PanelRegistro panelRegistro;
    private PanelHistoria panelHistoria;
    private PanelModoClasico panelModoClasico;
    private PanelModoPatron panelModoPatron;
    private PanelPremios panelPremios;

    private Recursos.BotonBosque[] pestanas;
    private Recursos.BotonBosque btnMusica;
    private CardLayout cardLayout;
    private JPanel contenedor;

    private JPanel capaVictoria;
    private JLabel lblVictoria;
    private JLabel lblCerrar;           // "Haz clic para cerrar" (solo se ve cuando termina la animación)
    private boolean victoriaTerminada = false;  // true cuando la animación ya se reprodujo completa
    private long momentoFinVictoria;            // hora (ms) en que terminó la animación
    private JLabel lblGanador;          // "¡Ganó el Jugador 1: Ana!"
    private PanelImagen bannerGanador;  // imagen de fondo del mensaje del ganador
    private HiloAnimacion tareaVictoria;
    private Thread hiloVictoria;
    private HiloSonido tareaSonidoVictoria;
    private ImageIcon[] cacheVictoria;      // fotogramas ya escalados (la 2a victoria sale al instante)
    private Dimension tamanoCacheVictoria;

    // ===== Animación nueva del duende (sello del rango) =====
    private PanelImagen capaRango;          // capa oscura (imagen semitransparente) sobre la ventana
    private JPanel tarjetaRango;            // contiene la imagen y, encima, los textos y el botón
    private JLabel lblImagenRango;          // muestra los cuadros de la animación y la tarjeta final
    private JLabel lblNombreRango;
    private JLabel lblIntentosRango;
    private JLabel lblMensajeRango;
    private BotonImagen btnContinuarRango;
    private HiloAnimacion tareaRango;
    private Thread hiloRango;
    private Timer temporizadorSonidoSello;
    private ImageIcon iconoTarjetaRango;    // tarjeta final del rango (ya escalada)
    private boolean hayRangoPendiente = false;
    private String claveRangoPendiente;     // "ancestral", "jefe", "guardian", "archivero", "aprendiz" o "despistado"
    private String nombreRangoPendiente;
    private int intentosRangoPendiente;
    private String mensajeRangoPendiente;

    public VentanaPrincipal(JuegoController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setTitle("Los Duendes Coleccionistas");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        Rectangle area = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int ancho = Math.min(1500, area.width);
        int alto = Math.min(900, area.height);
        setSize(ancho, alto);
        setMinimumSize(new Dimension(Math.min(1100, ancho), Math.min(680, alto)));

        panelIniciarSesion = new PanelIniciarSesion(controller);
        panelRegistro = new PanelRegistro(controller);
        panelHistoria = new PanelHistoria(controller);
        panelModoClasico = new PanelModoClasico(controller);
        panelModoPatron = new PanelModoPatron(controller);
        panelPremios = new PanelPremios(controller);

        // --- Barra superior: pestañas + botón de música ---
        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setBackground(Recursos.CAFE_OSCURO);
        barra.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel panelPestanas = new JPanel(new GridLayout(1, PANTALLAS.length, 6, 0));
        panelPestanas.setOpaque(false);
        pestanas = new Recursos.BotonBosque[PANTALLAS.length];
        for (int i = 0; i < PANTALLAS.length; i++) {
            final String pantalla = PANTALLAS[i];
            pestanas[i] = Recursos.crearBoton(NOMBRES_PESTANAS[i]);
            pestanas[i].setFont(Recursos.fuente(Font.BOLD, 17));
            pestanas[i].setMargin(new Insets(8, 6, 8, 6));
            pestanas[i].addActionListener(evt -> {
                if (!controller.haySesion() && !LOGIN.equals(pantalla)) {
                    mostrarMensaje("Debes iniciar sesión para poder jugar.");
                    return;
                }
                mostrarPantalla(pantalla);
            });
            panelPestanas.add(pestanas[i]);
        }
        btnMusica = Recursos.crearBoton("Música: OFF", Recursos.CAFE);
        btnMusica.setFont(Recursos.fuente(Font.BOLD, 16));
        btnMusica.addActionListener(evt -> controller.alternarMusica());
        barra.add(panelPestanas, BorderLayout.CENTER);
        barra.add(btnMusica, BorderLayout.EAST);

        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);
        contenedor.add(panelIniciarSesion, LOGIN);
        contenedor.add(panelRegistro, REGISTRO);
        contenedor.add(panelHistoria, HISTORIA);
        contenedor.add(panelModoClasico, CLASICO);
        contenedor.add(panelModoPatron, PATRON);
        contenedor.add(panelPremios, PREMIOS);

        add(barra, BorderLayout.NORTH);
        add(contenedor, BorderLayout.CENTER);

        crearCapaVictoria();
        crearCapaRango(); // animación nueva del duende
        precargarFotogramasVictoria();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panelHistoria.detenerAnimacion();
                detenerAnimacionVictoria();
                controller.detenerMusica();
                dispose();
                System.exit(0);
            }
        });

        mostrarPantalla(LOGIN);
        setLocationRelativeTo(null);
    }
    public void mostrarPantalla(String nombrePantalla) {
        cardLayout.show(contenedor, nombrePantalla);
        for (int i = 0; i < PANTALLAS.length; i++) {
            boolean activa = PANTALLAS[i].equals(nombrePantalla)
                    || (i == 0 && REGISTRO.equals(nombrePantalla));
            pestanas[i].setColorFondo(activa ? Recursos.VERDE : Recursos.CREMA_OSCURO);
            pestanas[i].setForeground(activa ? Recursos.CREMA : Recursos.TEXTO);
        }
        if (!HISTORIA.equals(nombrePantalla)) {
            panelHistoria.detenerAnimacion();
        }
        // Cada vez que se abren los premios se consulta el tipo de cambio actual del BCCR
        if (PREMIOS.equals(nombrePantalla)) {
            panelPremios.refrescar();
            controller.consultarTipoCambio();
        }
    }

    public void limpiarFormularios() {
        panelIniciarSesion.limpiar();
        panelRegistro.limpiar();
    }

    public void actualizarPremios() {
        panelPremios.refrescar();
    }

    public void actualizarBotonMusica(boolean sonando) {
        btnMusica.setText(sonando ? "Música: ON" : "Música: OFF");
    }

    public void reiniciarHistoria() {
        panelHistoria.reiniciar();
    }

    public void mostrarMensaje(String mensaje) {
        Recursos.mostrarAviso(this, "Los Duendes Coleccionistas", mensaje);
    }
    private void crearCapaVictoria() {
        capaVictoria = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        capaVictoria.setOpaque(false);
        lblVictoria = new JLabel("", JLabel.CENTER);
        lblCerrar = Recursos.crearEtiqueta("Haz clic para cerrar", 20, false, Recursos.CREMA);

        // Mensaje con el número y el nombre del ganador, sobre la imagen del banner café
        lblGanador = Recursos.crearEtiqueta("", 34, true, Recursos.CREMA);
        bannerGanador = new PanelImagen(RutasLogin.BANNER, new GridBagLayout(), new Dimension(700, 90))
                .respaldo(Recursos.CAFE);
        bannerGanador.setBorder(BorderFactory.createEmptyBorder(6, 40, 10, 40));
        bannerGanador.add(lblGanador);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        capaVictoria.add(lblVictoria, gc);
        gc.gridy = 1;
        gc.insets = new Insets(6, 0, 0, 0);
        capaVictoria.add(bannerGanador, gc);
        gc.gridy = 2;
        gc.insets = new Insets(10, 0, 0, 0);
        capaVictoria.add(lblCerrar, gc);
        // Los clics durante la animación se ignoran (la capa los atrapa para que no lleguen al tablero).
        // Solo se cierra con un clic que empiece DESPUÉS de que la animación terminó; así los clics
        // que el jugador hizo mientras se reproducía no la cierran al final.
        capaVictoria.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (victoriaTerminada && e.getWhen() >= momentoFinVictoria) {
                    detenerAnimacionVictoria();
                    iniciarAnimacionDuende(); // animación nueva del duende (sale después de la original)
                }
            }
        });
        setGlassPane(capaVictoria);
        capaVictoria.setVisible(false);
    }
    public void mostrarAnimacionVictoria() {
        mostrarAnimacionVictoria(null);
    }

    /** Muestra la animación y, debajo, el mensaje del ganador (si es null o vacío no se muestra). */
    public void mostrarAnimacionVictoria(String mensajeGanador) {
        detenerAnimacionVictoria();
        boolean hayMensaje = mensajeGanador != null && !mensajeGanador.isEmpty();
        lblGanador.setText(hayMensaje ? mensajeGanador : "");
        bannerGanador.setVisible(hayMensaje);
        int maxAncho = Math.max(300, Math.min(900, getWidth() - 200));
        int maxAlto = Math.max(200, Math.min(600, getHeight() - (hayMensaje ? 320 : 200)));
        ImageIcon[] cargados = cargarFotogramasVictoria(maxAncho, maxAlto);
        int n = cargados.length;
        // Entrada una vez y luego el ciclo repetido (se reutilizan las mismas imágenes)
        int entrada = Math.min(FOTOGRAMAS_ENTRADA, n - 1);
        int ciclo = n - entrada;
        ImageIcon[] fotogramas = new ImageIcon[entrada + ciclo * VUELTAS_VICTORIA];
        int[] retrasos = new int[fotogramas.length];
        for (int i = 0; i < fotogramas.length; i++) {
            fotogramas[i] = i < entrada ? cargados[i] : cargados[entrada + (i - entrada) % ciclo];
            retrasos[i] = RETRASO_VICTORIA_MS;
        }
        lblVictoria.setIcon(fotogramas[0]);
        victoriaTerminada = false;
        lblCerrar.setForeground(new Color(0, 0, 0, 0)); // oculto (sin mover nada) mientras se reproduce
        capaVictoria.setVisible(true);
        iniciarSonidoVictoria();
        tareaVictoria = new HiloAnimacion(lblVictoria, fotogramas, retrasos, false,
                this::terminarAnimacionVictoria);
        hiloVictoria = new Thread(tareaVictoria, "HiloVictoria");
        hiloVictoria.start();
    }

    /** Lee los PNG de la victoria en segundo plano para que al ganar no se congele la pantalla. */
    private void precargarFotogramasVictoria() {
        Thread hilo = new Thread(() -> {
            int n = Recursos.contarSecuencia(Recursos.CARPETA_ANIMACIONES, Recursos.PREFIJO_VICTORIA,
                    Recursos.FOTOGRAMAS_VICTORIA_MINIMOS);
            for (int i = 0; i < n; i++) {
                Imagenes.cargar(Recursos.rutaSecuencia(Recursos.CARPETA_ANIMACIONES, Recursos.PREFIJO_VICTORIA, i + 1));
            }
        }, "HiloPrecargaVictoria");
        hilo.setDaemon(true);
        hilo.start();
    }

    private ImageIcon[] cargarFotogramasVictoria(int maxAncho, int maxAlto) {
        Dimension tamano = new Dimension(maxAncho, maxAlto);
        if (cacheVictoria != null && tamano.equals(tamanoCacheVictoria)) {
            return cacheVictoria;
        }
        int n = Recursos.contarSecuencia(Recursos.CARPETA_ANIMACIONES, Recursos.PREFIJO_VICTORIA,
                Recursos.FOTOGRAMAS_VICTORIA_MINIMOS);
        ImageIcon[] cargados = new ImageIcon[n];
        for (int i = 0; i < n; i++) {
            String ruta = Recursos.rutaSecuencia(Recursos.CARPETA_ANIMACIONES, Recursos.PREFIJO_VICTORIA, i + 1);
            BufferedImage original = Imagenes.cargar(ruta);
            if (original == null) {
                cargados[i] = Recursos.cargarImagen(ruta, maxAncho, maxAlto, Recursos.CREMA_OSCURO);
                continue;
            }
            // Escalado rápido (getScaledInstance con 48 imágenes congelaba la pantalla unos segundos)
            double factor = Math.min(1.0, Math.min((double) maxAncho / original.getWidth(),
                    (double) maxAlto / original.getHeight()));
            int ancho = Math.max(1, (int) (original.getWidth() * factor));
            int alto = Math.max(1, (int) (original.getHeight() * factor));
            BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = escalada.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(original, 0, 0, ancho, alto, null);
            g2.dispose();
            cargados[i] = new ImageIcon(escalada);
        }
        cacheVictoria = cargados;
        tamanoCacheVictoria = tamano;
        return cargados;
    }

    private void iniciarSonidoVictoria() {
        File archivo = new File(SONIDO_VICTORIA);
        if (!archivo.exists()) {
            System.err.println("No se encontró la música de victoria: " + SONIDO_VICTORIA);
            return;
        }
        try {
            tareaSonidoVictoria = new HiloSonido(archivo);
            Thread hilo = new Thread(tareaSonidoVictoria, "HiloSonidoVictoria");
            hilo.setDaemon(true);
            hilo.start();
        } catch (LinkageError e) {
            System.err.println("Falta la librería JLayer (lib/jlayer-1.0.1.jar): " + e);
        }
    }

    /**
     * Se llama cuando la animación de victoria terminó completa: se queda en el último fotograma,
     * se apaga la música y ya se permite cerrarla con un clic.
     */
    private void terminarAnimacionVictoria() {
        if (tareaSonidoVictoria != null) {
            tareaSonidoVictoria.detener();
            tareaSonidoVictoria = null;
        }
        tareaVictoria = null;
        hiloVictoria = null;
        victoriaTerminada = true;
        momentoFinVictoria = System.currentTimeMillis();
        lblCerrar.setForeground(Recursos.CREMA);
    }

    private void detenerAnimacionVictoria() {
        victoriaTerminada = false;
        if (tareaSonidoVictoria != null) { // la música se apaga junto con la animación
            tareaSonidoVictoria.detener();
            tareaSonidoVictoria = null;
        }
        if (tareaVictoria != null) {
            tareaVictoria.detener();
            hiloVictoria.interrupt();
            tareaVictoria = null;
            hiloVictoria = null;
        }
        capaVictoria.setVisible(false);
    }

    // =====================================================================================
    // ===== Animación nueva del duende: el duende estampa el sello del rango obtenido =====
    // =====================================================================================
    // Sale cuando el jugador cierra la animación de victoria original (esa no se modifica).
    // Todo lo que se ve son imágenes: los cuadros del sello y la tarjeta del rango. Encima de la
    // tarjeta solo se ponen etiquetas con el nombre, los intentos y el mensaje, y el botón
    // "Continuar" con la imagen verde del juego (igual que en las ventanillas de aviso).

    /** El controlador avisa qué rango obtuvo el ganador; se muestra al cerrar la animación de victoria. */
    public void prepararAnimacionDuende(String claveRango, String nombre, int intentos, String mensaje) {
        claveRangoPendiente = claveRango;
        nombreRangoPendiente = nombre;
        intentosRangoPendiente = intentos;
        mensajeRangoPendiente = mensaje;
        hayRangoPendiente = true;
    }

    /** Crea la capa de la animación del duende (una sola vez, al abrir la ventana). */
    private void crearCapaRango() {
        // Fondo oscuro: es una imagen negra semitransparente que se estira a toda la ventana
        capaRango = new PanelImagen(CARPETA_RANGOS + "fondo_oscuro.png", new GridBagLayout(), new Dimension(10, 10));

        // Sin layout: cada texto va en la zona de la tarjeta que le corresponde
        tarjetaRango = new JPanel(null);
        tarjetaRango.setOpaque(false);

        lblImagenRango = new JLabel();
        lblNombreRango = new JLabel("", SwingConstants.CENTER);
        lblNombreRango.setForeground(Recursos.CAFE_OSCURO);
        lblIntentosRango = new JLabel("", SwingConstants.CENTER);
        lblIntentosRango.setForeground(Recursos.ROJO);
        lblMensajeRango = new JLabel("", SwingConstants.CENTER);
        lblMensajeRango.setForeground(Recursos.CAFE);

        btnContinuarRango = new BotonImagen("Continuar", RutasLogin.BOTON_VERDE, RutasLogin.BOTON_VERDE_PRES);
        btnContinuarRango.addActionListener(evt -> cerrarAnimacionDuende());

        // Lo primero que se agrega queda encima: textos y botón arriba, la imagen al fondo
        tarjetaRango.add(lblNombreRango);
        tarjetaRango.add(lblIntentosRango);
        tarjetaRango.add(lblMensajeRango);
        tarjetaRango.add(btnContinuarRango);
        tarjetaRango.add(lblImagenRango);
        capaRango.add(tarjetaRango);

        // Un clic durante la animación la salta y muestra la tarjeta de una vez.
        // La capa también atrapa los clics para que no lleguen a la pantalla de abajo.
        capaRango.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (tareaRango != null) {
                    tareaRango.detener();
                    hiloRango.interrupt();
                    mostrarTarjetaRango();
                }
            }
        });
        capaRango.setVisible(false);
    }

    /** Arranca la animación del sello y al final deja la tarjeta del rango con los datos del ganador. */
    private void iniciarAnimacionDuende() {
        if (!hayRangoPendiente) {
            return;
        }
        hayRangoPendiente = false;

        // Tamaño de la tarjeta: lo más grande posible sin salirse de la ventana (nunca más de 1400 x 800)
        JRootPane raiz = getRootPane();
        double e = Math.min(1.0, Math.min((raiz.getWidth() - 60) / (double) ANCHO_TARJETA,
                (raiz.getHeight() - 60) / (double) ALTO_TARJETA));
        e = Math.max(0.3, e);
        int ancho = (int) Math.round(ANCHO_TARJETA * e);
        int alto = (int) Math.round(ALTO_TARJETA * e);

        // Cuadros: el duende con el papel en blanco y luego el sello del rango cayendo sobre el papel
        String clave = claveRangoPendiente;
        String[] rutas = {
                CARPETA_RANGOS + "inspeccion_01.jpg",
                CARPETA_RANGOS + "sello_" + clave + "_01.jpg",
                CARPETA_RANGOS + "sello_" + clave + "_02.jpg",
                CARPETA_RANGOS + "sello_" + clave + "_03.jpg"
        };
        ImageIcon[] cuadros = new ImageIcon[rutas.length];
        for (int i = 0; i < rutas.length; i++) {
            cuadros[i] = cargarImagenRango(rutas[i], ancho, alto);
        }
        iconoTarjetaRango = cargarImagenRango(CARPETA_RANGOS + "rango_" + clave + ".jpg", ancho, alto);

        // Posición y letra de cada elemento según la escala de la tarjeta
        tarjetaRango.setPreferredSize(new Dimension(ancho, alto));
        lblImagenRango.setBounds(0, 0, ancho, alto);
        lblImagenRango.setIcon(cuadros[0]);
        colocarTextoRango(lblNombreRango, nombreRangoPendiente, ZONA_NOMBRE, Font.BOLD, 36, e);
        colocarTextoRango(lblIntentosRango, String.valueOf(intentosRangoPendiente), ZONA_INTENTOS, Font.BOLD, 44, e);
        colocarMensajeRango(mensajeRangoPendiente, e);
        btnContinuarRango.setBounds(escalarZona(ZONA_BOTON, e));
        btnContinuarRango.setFont(Recursos.fuente(Font.BOLD, (int) Math.round(30 * e)));

        // Mientras corre la animación solo se ve la imagen; los textos aparecen con la tarjeta
        mostrarDatosRango(false);
        setGlassPane(capaRango);
        capaRango.setVisible(true);
        capaRango.revalidate();

        tareaRango = new HiloAnimacion(lblImagenRango, cuadros, RETRASOS_SELLO_MS, false, this::mostrarTarjetaRango);
        hiloRango = new Thread(tareaRango, "HiloAnimacionDuende");
        hiloRango.setDaemon(true);
        hiloRango.start();

        // El golpe del sello suena justo en el cuadro del destello (el último de la animación)
        int momentoGolpe = RETRASOS_SELLO_MS[0] + RETRASOS_SELLO_MS[1] + RETRASOS_SELLO_MS[2];
        temporizadorSonidoSello = new Timer(momentoGolpe, evt -> reproducirSonidoSello());
        temporizadorSonidoSello.setRepeats(false);
        temporizadorSonidoSello.start();
    }

    /** Deja la tarjeta final del rango con el nombre, los intentos, el mensaje y el botón. */
    private void mostrarTarjetaRango() {
        tareaRango = null;
        hiloRango = null;
        if (temporizadorSonidoSello != null) { // si se saltó la animación, ya no suena el golpe
            temporizadorSonidoSello.stop();
            temporizadorSonidoSello = null;
        }
        lblImagenRango.setIcon(iconoTarjetaRango);
        mostrarDatosRango(true);
        btnContinuarRango.requestFocusInWindow();
    }

    /** Cierra la tarjeta del rango y deja todo como estaba (botón "Continuar"). */
    private void cerrarAnimacionDuende() {
        if (tareaRango != null) {
            tareaRango.detener();
            hiloRango.interrupt();
            tareaRango = null;
            hiloRango = null;
        }
        if (temporizadorSonidoSello != null) {
            temporizadorSonidoSello.stop();
            temporizadorSonidoSello = null;
        }
        capaRango.setVisible(false);
        setGlassPane(capaVictoria); // se devuelve la capa de la victoria original (queda oculta)
        lblImagenRango.setIcon(null);
        iconoTarjetaRango = null;
    }

    private void mostrarDatosRango(boolean visibles) {
        lblNombreRango.setVisible(visibles);
        lblIntentosRango.setVisible(visibles);
        lblMensajeRango.setVisible(visibles);
        btnContinuarRango.setVisible(visibles);
    }

    /** Pasa una zona medida en la imagen de 1400 x 800 al tamaño con que se muestra la tarjeta. */
    private Rectangle escalarZona(Rectangle zona, double e) {
        return new Rectangle((int) Math.round(zona.x * e), (int) Math.round(zona.y * e),
                (int) Math.round(zona.width * e), (int) Math.round(zona.height * e));
    }

    /** Coloca un texto de una línea en su zona; si no cabe, se reduce la letra. */
    private void colocarTextoRango(JLabel etiqueta, String texto, Rectangle zona, int estilo, int tamano, double e) {
        Rectangle z = escalarZona(zona, e);
        etiqueta.setText(texto);
        etiqueta.setBounds(z);
        for (int tam = (int) Math.round(tamano * e); tam >= 10; tam--) {
            etiqueta.setFont(Recursos.fuente(estilo, tam));
            if (etiqueta.getFontMetrics(etiqueta.getFont()).stringWidth(texto) <= z.width - 6) {
                break;
            }
        }
    }

    /** Coloca el mensaje ("Te faltaron X intentos...") centrado, en una o dos líneas. */
    private void colocarMensajeRango(String mensaje, double e) {
        Rectangle z = escalarZona(ZONA_MENSAJE, e);
        String texto = mensaje == null ? "" : mensaje.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        lblMensajeRango.setText("<html><div style='text-align:center; width:" + (z.width - 20) + "'>"
                + texto + "</div></html>");
        lblMensajeRango.setBounds(z);
        for (int tam = (int) Math.round(28 * e); tam >= 10; tam--) {
            lblMensajeRango.setFont(Recursos.fuente(Font.BOLD | Font.ITALIC, tam));
            if (lblMensajeRango.getPreferredSize().height <= z.height) {
                break;
            }
        }
    }

    /**
     * Lee una imagen de la animación del duende y la deja del tamaño de la tarjeta.
     * No se guarda en la caché de Imagenes porque son imágenes grandes que se usan una sola vez.
     */
    private ImageIcon cargarImagenRango(String ruta, int ancho, int alto) {
        BufferedImage original = null;
        File archivo = new File(Recursos.CARPETA_IMAGENES + ruta);
        try {
            if (archivo.exists()) {
                original = ImageIO.read(archivo);
            }
        } catch (IOException ex) {
            System.err.println("No se pudo leer " + archivo + ": " + ex.getMessage());
        }
        if (original == null) {
            System.err.println("Falta la imagen de la animación del duende: " + archivo);
            return Recursos.cargarImagen(ruta, ancho, alto, Recursos.CREMA_OSCURO);
        }
        // Mismo escalado rápido que usa la animación de victoria
        BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = escalada.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(original, 0, 0, ancho, alto, null);
        g2.dispose();
        return new ImageIcon(escalada);
    }

    /** Sonido corto del golpe del sello (BOINK.wav). Si no está, la animación sigue sin sonido. */
    private void reproducirSonidoSello() {
        File archivo = new File(SONIDO_SELLO);
        if (!archivo.exists()) {
            return;
        }
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(archivo)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception ex) {
            System.err.println("No se pudo reproducir el sonido del sello: " + ex.getMessage());
        }
    }
}
