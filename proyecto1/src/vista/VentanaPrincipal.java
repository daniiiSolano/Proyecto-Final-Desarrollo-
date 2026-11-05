package vista;

import apoyo.Imagenes;
import apoyo.PanelImagen;
import apoyo.Recursos;
import apoyo.RutasLogin;
import controlador.JuegoController;
import hilos.HiloAnimacion;
import hilos.HiloSonido;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

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
    private JLabel lblGanador;          // "¡Ganó el Jugador 1: Ana!"
    private PanelImagen bannerGanador;  // imagen de fondo del mensaje del ganador
    private HiloAnimacion tareaVictoria;
    private Thread hiloVictoria;
    private HiloSonido tareaSonidoVictoria;
    private ImageIcon[] cacheVictoria;      // fotogramas ya escalados (la 2a victoria sale al instante)
    private Dimension tamanoCacheVictoria;

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
        JOptionPane.showMessageDialog(this, mensaje, "Los Duendes Coleccionistas",
                JOptionPane.INFORMATION_MESSAGE);
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
        JLabel lblCerrar = Recursos.crearEtiqueta("Haz clic para cerrar", 20, false, Recursos.CREMA);

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
        capaVictoria.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detenerAnimacionVictoria();
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
        capaVictoria.setVisible(true);
        iniciarSonidoVictoria();
        tareaVictoria = new HiloAnimacion(lblVictoria, fotogramas, retrasos, false,
                this::detenerAnimacionVictoria);
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

    private void detenerAnimacionVictoria() {
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
}
