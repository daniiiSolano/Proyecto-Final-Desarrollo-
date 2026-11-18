package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;
import modelo.GestorPremios;
import modelo.Jugador;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

public class PanelPremios extends PanelConFondo {

    private static final String[] ENCABEZADOS = {"Posición", "Jugador", "Intentos", "Premio (USD)", "Premio (₡)"};
    private static final String[] ENCABEZADOS_PATRON = {"Posición", "Jugador", "Niveles", "Premio (USD)", "Premio (₡)"};
    private static final double[] PESOS_COLUMNAS = {0.13, 0.31, 0.14, 0.19, 0.23};
    private static final String TEXTO_LIBRE = "¡Gana una partida para aparecer aquí!";
    private static final String TEXTO_LIBRE_PATRON = "¡Supera un nivel para aparecer aquí!";
    private static final String SUBTITULO_CLASICO = "Los 5 mejores jugadores del modo clásico";
    private static final String SUBTITULO_PATRON = "Los 5 mejores jugadores del modo patrón";
    private static final int TAMANO_ICONO = 56;

    private final JuegoController controller;
    private PanelRedondeado tarjeta;
    private JPanel contenedorTabla;
    private JLabel lblCambio;
    private JLabel lblSubtitulo;
    private Recursos.BotonBosque btnClasico;
    private Recursos.BotonBosque btnPatron;
    private boolean modoPatron = false;

    public PanelPremios(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 120, 12, 120));

        JPanel norte = new JPanel(new GridBagLayout());
        norte.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        norte.add(Recursos.crearBanner("Premios", 42), gc);
        gc.gridy = 1;
        gc.insets = new Insets(6, 0, 0, 0);
        PanelRedondeado subtitulo = Recursos.crearSubtitulo(SUBTITULO_CLASICO);
        lblSubtitulo = (JLabel) subtitulo.getComponent(0);
        norte.add(subtitulo, gc);
        gc.gridy = 2;
        gc.insets = new Insets(8, 0, 0, 0);
        norte.add(crearPestanasModo(), gc);
        add(norte, BorderLayout.NORTH);

        tarjeta = new PanelRedondeado(new BorderLayout(0, 8), Recursos.CREMA, Recursos.VERDE_OSCURO, 36);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        contenedorTabla = new JPanel(new BorderLayout());
        contenedorTabla.setOpaque(false);
        contenedorTabla.add(crearTabla(), BorderLayout.CENTER);
        tarjeta.add(contenedorTabla, BorderLayout.CENTER);
        tarjeta.add(crearPieTipoCambio(), BorderLayout.SOUTH);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));
        envoltorio.add(tarjeta, BorderLayout.CENTER);
        add(envoltorio, BorderLayout.CENTER);

        Recursos.BotonBosque btnVolver = Recursos.crearBoton("Volver al menú");
        btnVolver.setFont(Recursos.fuente(Font.BOLD, 24));
        btnVolver.addActionListener(evt -> controller.volverAlMenu());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        botones.setOpaque(false);
        botones.add(btnVolver);
        add(botones, BorderLayout.SOUTH);

        actualizarPestanas();
    }

    private JPanel crearPestanasModo() {
        btnClasico = crearPestana("Modo Clásico");
        btnClasico.addActionListener(evt -> cambiarModo(false));
        btnPatron = crearPestana("Modo Patrón");
        btnPatron.addActionListener(evt -> cambiarModo(true));

        JPanel pestanas = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        pestanas.setOpaque(false);
        pestanas.add(btnClasico);
        pestanas.add(btnPatron);
        return pestanas;
    }

    private Recursos.BotonBosque crearPestana(String texto) {
        Recursos.BotonBosque pestana = Recursos.crearBoton(texto);
        pestana.setFont(Recursos.fuente(Font.BOLD, 18));
        pestana.setMargin(new Insets(4, 22, 4, 22));
        return pestana;
    }

    private JPanel crearPieTipoCambio() {
        PanelRedondeado cajaCambio = new PanelRedondeado(new GridBagLayout(), Recursos.CREMA_OSCURO, Recursos.CAFE, 24);
        cajaCambio.setBorder(BorderFactory.createEmptyBorder(4, 24, 6, 24));
        lblCambio = Recursos.crearEtiqueta(controller.getTextoTipoCambio(), 18, false, Recursos.TEXTO);
        lblCambio.setHorizontalAlignment(JLabel.CENTER);
        cajaCambio.add(lblCambio);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pie.setOpaque(false);
        pie.add(cajaCambio);
        return pie;
    }

    private void cambiarModo(boolean patron) {
        if (modoPatron == patron) {
            return;
        }
        modoPatron = patron;
        actualizarPestanas();
        refrescar();
    }

    private void actualizarPestanas() {
        pintarPestana(btnClasico, !modoPatron);
        pintarPestana(btnPatron, modoPatron);
        lblSubtitulo.setText(modoPatron ? SUBTITULO_PATRON : SUBTITULO_CLASICO);
    }

    private void pintarPestana(Recursos.BotonBosque pestana, boolean activa) {
        pestana.setColorFondo(activa ? Recursos.VERDE : Recursos.CREMA_OSCURO);
        pestana.setForeground(activa ? Recursos.CREMA : Recursos.TEXTO);
    }

    public void refrescar() {
        contenedorTabla.removeAll();
        contenedorTabla.add(crearTabla(), BorderLayout.CENTER);
        lblCambio.setText(controller.getTextoTipoCambio());
        tarjeta.revalidate();
        tarjeta.repaint();
    }

    private JPanel crearTabla() {
        String[] encabezados = modoPatron ? ENCABEZADOS_PATRON : ENCABEZADOS;
        JPanel tabla = new JPanel(new GridBagLayout());
        tabla.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        for (int col = 0; col < encabezados.length; col++) {
            PanelRedondeado cab = new PanelRedondeado(new GridBagLayout(), Recursos.CAFE, Recursos.CAFE_OSCURO, 16);
            cab.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
            cab.add(Recursos.crearEtiqueta(encabezados[col], 22, true, Recursos.CREMA));
            gc.gridx = col;
            gc.gridy = 0;
            gc.weightx = PESOS_COLUMNAS[col];
            gc.weighty = 0;
            gc.insets = new Insets(0, 2, 6, 2);
            tabla.add(cab, gc);
        }
        List<Jugador> top = modoPatron ? controller.getTop5Patron() : controller.getTop5();
        for (int i = 0; i < 5; i++) {
            int posicion = i + 1;
            gc.gridy = posicion;
            gc.weighty = 1;
            gc.insets = new Insets(0, 2, 0, 2);

            if (i >= top.size()) {
                JComponent medalla = crearCeldaMedalla(posicion);
                medalla.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Recursos.CREMA_OSCURO));
                gc.gridx = 0;
                gc.gridwidth = 1;
                gc.weightx = PESOS_COLUMNAS[0];
                tabla.add(medalla, gc);

                JLabel libre = Recursos.crearEtiqueta(modoPatron ? TEXTO_LIBRE_PATRON : TEXTO_LIBRE,
                        22, false, Recursos.CAFE);
                libre.setHorizontalAlignment(JLabel.CENTER);
                libre.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Recursos.CREMA_OSCURO));
                gc.gridx = 1;
                gc.gridwidth = encabezados.length - 1;
                gc.weightx = 1 - PESOS_COLUMNAS[0];
                tabla.add(libre, gc);
                gc.gridwidth = 1;
                continue;
            }

            Jugador j = top.get(i);
            int puntaje;
            double premioDolares;
            if (modoPatron) {
                puntaje = (int) j.getPuntos();
                premioDolares = GestorPremios.calcularPremioPatronDolares(puntaje);
            } else {
                puntaje = j.getIntentos();
                premioDolares = 100.0 / j.getIntentos();
            }
            JComponent[] celdas = {
                    crearCeldaMedalla(posicion),
                    crearCeldaJugador(j.getNombre(), Recursos.DUENDES[i]),
                    crearCeldaTexto(String.valueOf(puntaje)),
                    crearCeldaTexto(String.format(Locale.US, "$ %,.2f", premioDolares)),
                    crearCeldaTexto(j.getPremio() <= 0 ? "-" : String.format(Locale.US, "₡ %,.2f", j.getPremio()))
            };
            for (int col = 0; col < celdas.length; col++) {
                celdas[col].setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Recursos.CREMA_OSCURO));
                gc.gridx = col;
                gc.weightx = PESOS_COLUMNAS[col];
                tabla.add(celdas[col], gc);
            }
        }
        return tabla;
    }

    private JComponent crearCeldaMedalla(int posicion) {
        JLabel medalla = new JLabel(Recursos.cargarImagen(Recursos.rutaMedalla(posicion), TAMANO_ICONO,
                TAMANO_ICONO, Recursos.CREMA_OSCURO), JLabel.CENTER);
        return medalla;
    }

    private JComponent crearCeldaJugador(String nombre, String rutaDuende) {
        JPanel celda = new JPanel();
        celda.setLayout(new BoxLayout(celda, BoxLayout.X_AXIS));
        celda.setOpaque(false);
        JLabel duende = new JLabel(Recursos.cargarImagen(rutaDuende, TAMANO_ICONO, TAMANO_ICONO,
                Recursos.CREMA_OSCURO));
        duende.setAlignmentY(0.5f);
        JLabel lblNombre = Recursos.crearEtiqueta(nombre, 24, false, Recursos.TEXTO);
        lblNombre.setAlignmentY(0.5f);
        celda.add(Box.createHorizontalStrut(10));
        celda.add(duende);
        celda.add(Box.createHorizontalStrut(14));
        celda.add(lblNombre);
        celda.add(Box.createHorizontalGlue());
        return celda;
    }

    private JComponent crearCeldaTexto(String texto) {
        JLabel etiqueta = Recursos.crearEtiqueta(texto, 26, false, Recursos.TEXTO);
        etiqueta.setHorizontalAlignment(JLabel.CENTER);
        return etiqueta;
    }
}