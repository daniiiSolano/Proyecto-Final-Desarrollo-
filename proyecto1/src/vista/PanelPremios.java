package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;
import modelo.Jugador;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

public class PanelPremios extends PanelConFondo {

    // Premio (USD) = 100 / intentos.  Premio (₡) = fórmula del enunciado: 1/puntaje * 100 * CambioDolar
    private static final String[] ENCABEZADOS = {"Posición", "Jugador", "Intentos", "Premio (USD)", "Premio (₡)"};
    private static final double[] PESOS_COLUMNAS = {0.13, 0.31, 0.14, 0.19, 0.23};
    private static final String TEXTO_LIBRE = "¡Gana una partida para aparecer aquí!";
    private static final int TAMANO_ICONO = 56;

    private final JuegoController controller;
    private PanelRedondeado tarjeta;
    private JLabel lblCambio;

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
        norte.add(Recursos.crearSubtitulo("Los 5 mejores jugadores del modo clásico"), gc);
        add(norte, BorderLayout.NORTH);

        tarjeta = new PanelRedondeado(new BorderLayout(0, 6), Recursos.CREMA, Recursos.VERDE_OSCURO, 36);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        tarjeta.add(crearTabla(), BorderLayout.CENTER);
        lblCambio = Recursos.crearEtiqueta(controller.getTextoTipoCambio(), 18, false, Recursos.TEXTO);
        lblCambio.setHorizontalAlignment(JLabel.CENTER);
        tarjeta.add(lblCambio, BorderLayout.SOUTH);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        envoltorio.add(tarjeta, BorderLayout.CENTER);
        add(envoltorio, BorderLayout.CENTER);

        Recursos.BotonBosque btnVolver = Recursos.crearBoton("Volver al menú");
        btnVolver.setFont(Recursos.fuente(Font.BOLD, 24));
        btnVolver.addActionListener(evt -> controller.volverAlMenu());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        botones.setOpaque(false);
        botones.add(btnVolver);
        add(botones, BorderLayout.SOUTH);
    }

    public void refrescar() {
        tarjeta.removeAll();
        tarjeta.add(crearTabla(), BorderLayout.CENTER);
        lblCambio.setText(controller.getTextoTipoCambio());
        tarjeta.add(lblCambio, BorderLayout.SOUTH);
        tarjeta.revalidate();
        tarjeta.repaint();
    }

    private JPanel crearTabla() {
        JPanel tabla = new JPanel(new GridBagLayout());
        tabla.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        for (int col = 0; col < ENCABEZADOS.length; col++) {
            PanelRedondeado cab = new PanelRedondeado(new GridBagLayout(), Recursos.CAFE, Recursos.CAFE_OSCURO, 16);
            cab.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
            cab.add(Recursos.crearEtiqueta(ENCABEZADOS[col], 22, true, Recursos.CREMA));
            gc.gridx = col;
            gc.gridy = 0;
            gc.weightx = PESOS_COLUMNAS[col];
            gc.weighty = 0;
            gc.insets = new Insets(0, 2, 6, 2);
            tabla.add(cab, gc);
        }
        List<Jugador> top = controller.getTop5();
        for (int i = 0; i < 5; i++) {
            int posicion = i + 1;
            gc.gridy = posicion;
            gc.weighty = 1;
            gc.insets = new Insets(0, 2, 0, 2);

            if (i >= top.size()) {
                // Puesto libre: la medalla y un mensaje que ocupa el resto de la fila
                JComponent medalla = crearCeldaMedalla(posicion);
                medalla.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Recursos.CREMA_OSCURO));
                gc.gridx = 0;
                gc.gridwidth = 1;
                gc.weightx = PESOS_COLUMNAS[0];
                tabla.add(medalla, gc);

                JLabel libre = Recursos.crearEtiqueta(TEXTO_LIBRE, 22, false, Recursos.CAFE);
                libre.setHorizontalAlignment(JLabel.CENTER);
                libre.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Recursos.CREMA_OSCURO));
                gc.gridx = 1;
                gc.gridwidth = ENCABEZADOS.length - 1;
                gc.weightx = 1 - PESOS_COLUMNAS[0];
                tabla.add(libre, gc);
                gc.gridwidth = 1;
                continue;
            }

            Jugador j = top.get(i);
            double premioDolares = 100.0 / j.getIntentos(); // 1/puntaje * 100
            JComponent[] celdas = {
                    crearCeldaMedalla(posicion),
                    crearCeldaJugador(j.getNombre(), Recursos.DUENDES[i]),
                    crearCeldaTexto(String.valueOf(j.getIntentos())),
                    crearCeldaTexto(String.format(Locale.US, "$ %,.2f", premioDolares)),
                    // Si el BCCR no respondió todavía, el premio en colones queda en "-"
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


