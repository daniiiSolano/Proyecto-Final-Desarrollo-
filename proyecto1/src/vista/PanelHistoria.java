package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import controlador.JuegoController;
import hilos.HiloAnimacion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
public class PanelHistoria extends PanelConFondo {

    private static final int RETRASO_PAGINA_MS = 2500;
    private static final String TEXTO_COMENZAR = "Comenzar aventura";

    private final JuegoController controller;
    private JLabel lblLibro;
    private JLabel lblTexto;
    private Recursos.BotonBosque btnComenzar;
    private Recursos.BotonBosque btnSiguiente;

    private HiloAnimacion tareaAnimacion;
    private Thread hiloAnimacion;

    public PanelHistoria(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 40, 14, 40));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        norte.setOpaque(false);
        norte.add(Recursos.crearBanner("Historia", 38));
        add(norte, BorderLayout.NORTH);

        lblLibro = new JLabel("", JLabel.CENTER);
        lblLibro.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (tareaAnimacion == null) {
                    mostrarPaginaActual();
                }
            }
        });
        lblTexto = new JLabel("", JLabel.CENTER);
        lblTexto.setFont(Recursos.fuente(Font.PLAIN, 20));
        lblTexto.setForeground(Recursos.CREMA);
        lblTexto.setVisible(!Recursos.TEXTO_EN_IMAGEN_HISTORIA);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(lblLibro, BorderLayout.CENTER);
        centro.add(lblTexto, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        btnComenzar = Recursos.crearBoton(TEXTO_COMENZAR);
        btnComenzar.setFont(Recursos.fuente(Font.BOLD, 28));
        btnComenzar.addActionListener(evt -> comenzarAventura());

        btnSiguiente = Recursos.crearBoton("Siguiente", Recursos.CAFE);
        btnSiguiente.addActionListener(evt -> siguiente());

        Recursos.BotonBosque btnVolver = Recursos.crearBoton("Volver al menú", Recursos.CAFE);
        btnVolver.addActionListener(evt -> {
            detenerAnimacion();
            controller.volverAlInicio();
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        botones.setOpaque(false);
        botones.add(btnVolver);
        botones.add(btnComenzar);
        botones.add(btnSiguiente);
        add(botones, BorderLayout.SOUTH);
    }
    private void mostrarPaginaActual() {
        int[] tam = tamanoDisponible();
        lblLibro.setIcon(Recursos.cargarImagen(controller.getRutaImagenPagina(controller.getPaginaActual()),
                tam[0], tam[1], Recursos.CREMA_OSCURO));

        lblTexto.setText("<html><div style='text-align:center;width:900px'>"
                + controller.getTextoPaginaActual() + "</div></html>");
    }

    private int[] tamanoDisponible() {
        int ancho = lblLibro.getWidth() > 0 ? lblLibro.getWidth() - 10 : 1000;
        int alto = lblLibro.getHeight() > 0 ? lblLibro.getHeight() - 10 : 560;
        return new int[]{ancho, alto};
    }
    private void comenzarAventura() {
        detenerAnimacion();
        if (controller.getPaginaActual() >= controller.getTotalPaginas() - 1) {
            controller.reiniciarLibro();
            mostrarPaginaActual();
        }
        int primera = controller.getPaginaActual() + 1;
        int n = controller.getTotalPaginas() - primera;
        if (n <= 0) {
            return;
        }
        int[] tam = tamanoDisponible();
        ImageIcon[] fotogramas = new ImageIcon[n];
        int[] retrasos = new int[n];
        for (int i = 0; i < n; i++) {
            fotogramas[i] = Recursos.cargarImagen(controller.getRutaImagenPagina(primera + i),
                    tam[0], tam[1], Recursos.CREMA_OSCURO);
            retrasos[i] = RETRASO_PAGINA_MS;
        }
        btnComenzar.setEnabled(false);
        btnSiguiente.setEnabled(false);
        tareaAnimacion = new HiloAnimacion(lblLibro, fotogramas, retrasos, false, () -> {
            controller.irAUltimaPagina();
            tareaAnimacion = null;
            hiloAnimacion = null;
            btnComenzar.setText("Ver historia de nuevo");
            btnComenzar.setEnabled(true);
            btnSiguiente.setEnabled(true);
        });
        hiloAnimacion = new Thread(tareaAnimacion, "HiloAnimacionLibro");
        hiloAnimacion.start();
    }

    private void siguiente() {
        detenerAnimacion();
        if (controller.siguientePagina()) {
            mostrarPaginaActual();
        }
    }

    public void detenerAnimacion() {
        if (tareaAnimacion != null) {
            tareaAnimacion.detener();
            hiloAnimacion.interrupt();
            tareaAnimacion = null;
            hiloAnimacion = null;
        }
        btnComenzar.setEnabled(true);
        btnSiguiente.setEnabled(true);
    }
    public void reiniciar() {
        detenerAnimacion();
        btnComenzar.setText(TEXTO_COMENZAR);
        mostrarPaginaActual();
    }
}
