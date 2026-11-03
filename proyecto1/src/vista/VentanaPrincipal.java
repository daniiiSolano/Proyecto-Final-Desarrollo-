package vista;

import apoyo.BotonImagen;
import apoyo.Recursos;
import apoyo.RutasLogin;
import controlador.JuegoController;
import hilos.HiloAnimacion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VentanaPrincipal extends JFrame {

    public static final String LOGIN = "login";
    public static final String HISTORIA = "historia";
    public static final String CLASICO = "clasico";
    public static final String PATRON = "patron";
    public static final String PREMIOS = "premios";
    public static final String REGISTRO = "registro";

    private static final String[] PANTALLAS = {LOGIN, HISTORIA, CLASICO, PATRON, PREMIOS};
    private static final String[] NOMBRES_PESTANAS =
            {"Registro y Log In", "Historia", "Modo clasico", "Modo patron", "Premios"};

    private final JuegoController controller;

    private PanelIniciarSesion panelIniciarSesion;
    private PanelHistoria panelHistoria;
    private PanelModoClasico panelModoClasico;
    private PanelModoPatron panelModoPatron;
    private PanelPremios panelPremios;
    private PanelRegistro panelRegistro;

    private Recursos.BotonBosque[] pestanas;
    private Recursos.BotonBosque btnMusica;
    private JLabel lblUsuario;
    private BotonImagen btnCerrarSesion;
    private CardLayout cardLayout;
    private JPanel contenedor;

    private JPanel capaVictoria;
    private JLabel lblVictoria;
    private HiloAnimacion tareaVictoria;
    private Thread hiloVictoria;

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
        panelHistoria = new PanelHistoria(controller);
        panelModoClasico = new PanelModoClasico(controller);
        panelModoPatron = new PanelModoPatron(controller);
        panelPremios = new PanelPremios(controller);
        panelRegistro = new PanelRegistro(controller);

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
            pestanas[i].addActionListener(evt -> controller.irA(pantalla));
            panelPestanas.add(pestanas[i]);
        }
        btnMusica = Recursos.crearBoton("Música: OFF", Recursos.CAFE);
        btnMusica.setFont(Recursos.fuente(Font.BOLD, 16));
        btnMusica.addActionListener(evt -> controller.alternarMusica());
        lblUsuario = Recursos.crearEtiqueta("", 18, true, Recursos.CREMA);
        btnCerrarSesion = new BotonImagen("Cerrar sesión", RutasLogin.BOTON_CAFE, RutasLogin.BOTON_CAFE_PRES);
        btnCerrarSesion.setFont(Recursos.fuente(Font.BOLD, 16));
        btnCerrarSesion.setPreferredSize(new Dimension(160, 40));
        btnCerrarSesion.addActionListener(evt -> controller.cerrarSesion());
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        derecha.add(lblUsuario);
        derecha.add(btnCerrarSesion);
        derecha.add(btnMusica);
        barra.add(panelPestanas, BorderLayout.CENTER);
        barra.add(derecha, BorderLayout.EAST);

        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);
        contenedor.add(panelIniciarSesion, LOGIN);
        contenedor.add(panelHistoria, HISTORIA);
        contenedor.add(panelModoClasico, CLASICO);
        contenedor.add(panelModoPatron, PATRON);
        contenedor.add(panelPremios, PREMIOS);
        contenedor.add(panelRegistro, REGISTRO);

        add(barra, BorderLayout.NORTH);
        add(contenedor, BorderLayout.CENTER);

        crearCapaVictoria();
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

        actualizarSesion(false, "");
        mostrarPantalla(LOGIN);
        setLocationRelativeTo(null);
    }
    public void mostrarPantalla(String nombrePantalla) {
        cardLayout.show(contenedor, nombrePantalla);
        for (int i = 0; i < PANTALLAS.length; i++) {
            boolean activa = PANTALLAS[i].equals(nombrePantalla)
                    || (REGISTRO.equals(nombrePantalla) && LOGIN.equals(PANTALLAS[i]));
            pestanas[i].setColorFondo(activa ? Recursos.VERDE : Recursos.CREMA_OSCURO);
            pestanas[i].setForeground(activa ? Recursos.CREMA : Recursos.TEXTO);
        }
        if (!HISTORIA.equals(nombrePantalla)) {
            panelHistoria.detenerAnimacion();
        }
    }

    /** Sin sesión solo la pestaña de login está disponible; con sesión, todas menos el login. */
    public void actualizarSesion(boolean activa, String usuario) {
        for (int i = 0; i < PANTALLAS.length; i++) {
            pestanas[i].setEnabled(activa != LOGIN.equals(PANTALLAS[i]));
        }
        lblUsuario.setText(activa ? "Jugador: " + usuario : "");
        lblUsuario.setVisible(activa);
        btnCerrarSesion.setVisible(activa);
    }

    public void limpiarFormularios() {
        panelIniciarSesion.limpiar();
        panelRegistro.limpiar();
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
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        capaVictoria.add(lblVictoria, gc);
        gc.gridy = 1;
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
        detenerAnimacionVictoria();
        int maxAncho = Math.max(300, Math.min(900, getWidth() - 200));
        int maxAlto = Math.max(200, Math.min(600, getHeight() - 200));
        int n = Recursos.contarSecuencia(Recursos.CARPETA_ANIMACIONES, Recursos.PREFIJO_VICTORIA,
                Recursos.FOTOGRAMAS_VICTORIA_MINIMOS);
        ImageIcon[] fotogramas = new ImageIcon[n];
        int[] retrasos = new int[n];
        for (int i = 0; i < n; i++) {
            String ruta = Recursos.rutaSecuencia(Recursos.CARPETA_ANIMACIONES, Recursos.PREFIJO_VICTORIA, i + 1);
            fotogramas[i] = Recursos.cargarImagen(ruta, maxAncho, maxAlto, Recursos.CREMA_OSCURO);
            retrasos[i] = 900;
        }
        lblVictoria.setIcon(fotogramas[0]);
        capaVictoria.setVisible(true);
        tareaVictoria = new HiloAnimacion(lblVictoria, fotogramas, retrasos, false,
                this::detenerAnimacionVictoria);
        hiloVictoria = new Thread(tareaVictoria, "HiloVictoria");
        hiloVictoria.start();
    }

    private void detenerAnimacionVictoria() {
        if (tareaVictoria != null) {
            tareaVictoria.detener();
            hiloVictoria.interrupt();
            tareaVictoria = null;
            hiloVictoria = null;
        }
        capaVictoria.setVisible(false);
    }
}