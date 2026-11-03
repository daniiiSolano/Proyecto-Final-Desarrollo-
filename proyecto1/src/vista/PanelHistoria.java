package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import controlador.JuegoController;
import hilos.HiloAnimacion;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PanelHistoria extends PanelConFondo {

    private static final int RETRASO_PAGINA_MS = 4000;
    private static final int RETRASO_FOTOGRAMA_MS = 65;
    private static final String TEXTO_COMENZAR = "Comenzar aventura";
    private static final String TEXTO_REPETIR = "Ver historia de nuevo";
    private static final String TEXTO_SIGUIENTE = "Siguiente";
    private static final String TEXTO_CERRAR = "Cerrar libro";

    private final JuegoController controller;
    private JLabel lblLibro;
    private JLabel lblTexto;
    private Recursos.BotonBosque btnComenzar;
    private Recursos.BotonBosque btnSiguiente;

    private HiloAnimacion tareaAnimacion;
    private Thread hiloAnimacion;

    private final Map<String, ImageIcon> cache = new ConcurrentHashMap<>();
    private volatile boolean precargando = false;
    private final Timer temporizadorTamano;

    public PanelHistoria(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        temporizadorTamano = new Timer(250, e -> {
            int[] tam = tamanoDisponible();
            String sufijo = "@" + tam[0] + "x" + tam[1];
            cache.keySet().removeIf(k -> !k.endsWith(sufijo));
            if (tareaAnimacion == null) {
                mostrarPaginaActual();
            }
            precargar();
        });
        temporizadorTamano.setRepeats(false);
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
                if (lblLibro.getIcon() == null && tareaAnimacion == null) {
                    mostrarPaginaActual();
                }
                temporizadorTamano.restart();
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

        btnSiguiente = Recursos.crearBoton(TEXTO_SIGUIENTE, Recursos.CAFE);
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
        lblLibro.setIcon(icono(controller.getRutaImagenPagina(controller.getPaginaActual()), tam[0], tam[1]));
        lblTexto.setText("<html><div style='text-align:center;width:900px'>"
                + controller.getTextoPaginaActual() + "</div></html>");
        if (btnSiguiente != null) {
            boolean ultima = controller.getPaginaActual() >= controller.getTotalPaginas() - 1;
            btnSiguiente.setText(ultima ? TEXTO_CERRAR : TEXTO_SIGUIENTE);
        }
    }

    private int[] tamanoDisponible() {
        int ancho = lblLibro.getWidth() > 0 ? lblLibro.getWidth() - 10 : 1000;
        int alto = lblLibro.getHeight() > 0 ? lblLibro.getHeight() - 10 : 560;
        return new int[]{ancho, alto};
    }

    private ImageIcon icono(String ruta, int ancho, int alto) {
        String clave = ruta + "@" + ancho + "x" + alto;
        ImageIcon icono = cache.get(clave);
        if (icono == null) {
            icono = cargarEscalada(ruta, ancho, alto);
            cache.put(clave, icono);
        }
        return icono;
    }

    private ImageIcon cargarEscalada(String ruta, int maxAncho, int maxAlto) {
        File archivo = new File(Recursos.CARPETA_IMAGENES + ruta);
        if (archivo.exists()) {
            try {
                BufferedImage original = ImageIO.read(archivo);
                double factor = Math.min((double) maxAncho / original.getWidth(),
                        (double) maxAlto / original.getHeight());
                factor = Math.min(factor, 1.0);
                int ancho = Math.max(1, (int) (original.getWidth() * factor));
                int alto = Math.max(1, (int) (original.getHeight() * factor));
                BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = destino.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(original, 0, 0, ancho, alto, null);
                g.dispose();
                return new ImageIcon(destino);
            } catch (IOException | RuntimeException e) {
            }
        }
        return Recursos.cargarImagen(ruta, maxAncho, maxAlto, Recursos.CREMA_OSCURO);
    }

    private void precargar() {
        if (precargando) {
            return;
        }
        precargando = true;
        final int[] tam = tamanoDisponible();
        Thread hilo = new Thread(() -> {
            try {
                for (String ruta : controller.getRutasCierre()) {
                    icono(ruta, tam[0], tam[1]);
                }
                for (int p = 0; p < controller.getTotalPaginas(); p++) {
                    icono(controller.getRutaImagenPagina(p), tam[0], tam[1]);
                    for (String ruta : controller.getRutasTransicion(p)) {
                        icono(ruta, tam[0], tam[1]);
                    }
                }
            } finally {
                precargando = false;
            }
        }, "PrecargaLibro");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void comenzarAventura() {
        detenerAnimacion();
        if (controller.getPaginaActual() >= controller.getTotalPaginas() - 1) {
            controller.reiniciarLibro();
            mostrarPaginaActual();
        }
        reproducir(controller.getPaginaActual(), 1);
    }

    private void siguiente() {
        detenerAnimacion();
        if (controller.getPaginaActual() < controller.getTotalPaginas() - 1) {
            reproducir(controller.getPaginaActual(), 1);
        } else {
            cerrarLibro();
        }
    }

    private void reproducir(int desde, int pasos) {
        if (pasos <= 0) {
            return;
        }
        int[] tam = tamanoDisponible();
        List<ImageIcon> fotos = new ArrayList<>();
        List<Integer> retrasos = new ArrayList<>();
        for (int p = desde; p < desde + pasos; p++) {
            for (String ruta : controller.getRutasTransicion(p)) {
                fotos.add(icono(ruta, tam[0], tam[1]));
                retrasos.add(RETRASO_FOTOGRAMA_MS);
            }
            fotos.add(icono(controller.getRutaImagenPagina(p + 1), tam[0], tam[1]));
            boolean ultima = (p == desde + pasos - 1);
            retrasos.add(ultima ? 0 : RETRASO_PAGINA_MS);
        }
        ejecutar(fotos, retrasos, () -> {
            for (int i = 0; i < pasos; i++) {
                controller.siguientePagina();
            }
            if (controller.getPaginaActual() >= controller.getTotalPaginas() - 1) {
                btnComenzar.setText(TEXTO_REPETIR);
            }
        });
    }

    private void cerrarLibro() {
        int[] tam = tamanoDisponible();
        List<ImageIcon> fotos = new ArrayList<>();
        List<Integer> retrasos = new ArrayList<>();
        for (String ruta : controller.getRutasCierre()) {
            fotos.add(icono(ruta, tam[0], tam[1]));
            retrasos.add(RETRASO_FOTOGRAMA_MS);
        }
        fotos.add(icono(controller.getRutaImagenPagina(0), tam[0], tam[1]));
        retrasos.add(0);
        ejecutar(fotos, retrasos, () -> {
            controller.reiniciarLibro();
            btnComenzar.setText(TEXTO_COMENZAR);
        });
    }

    private void ejecutar(List<ImageIcon> fotos, List<Integer> retrasos, Runnable alActualizarModelo) {
        ImageIcon[] fotogramas = fotos.toArray(new ImageIcon[0]);
        int[] pausas = new int[retrasos.size()];
        for (int i = 0; i < pausas.length; i++) {
            pausas[i] = retrasos.get(i);
        }
        btnComenzar.setEnabled(false);
        btnSiguiente.setEnabled(false);
        tareaAnimacion = new HiloAnimacion(lblLibro, fotogramas, pausas, false, () -> {
            alActualizarModelo.run();
            tareaAnimacion = null;
            hiloAnimacion = null;
            btnComenzar.setEnabled(true);
            btnSiguiente.setEnabled(true);
            mostrarPaginaActual();
        });
        hiloAnimacion = new Thread(tareaAnimacion, "HiloAnimacionLibro");
        hiloAnimacion.start();
    }

    public void detenerAnimacion() {
        boolean habiaAnimacion = tareaAnimacion != null;
        if (habiaAnimacion) {
            tareaAnimacion.detener();
            hiloAnimacion.interrupt();
            tareaAnimacion = null;
            hiloAnimacion = null;
        }
        btnComenzar.setEnabled(true);
        btnSiguiente.setEnabled(true);
        if (habiaAnimacion) {
            mostrarPaginaActual();
        }
    }

    public void reiniciar() {
        detenerAnimacion();
        btnComenzar.setText(TEXTO_COMENZAR);
        mostrarPaginaActual();
    }
}