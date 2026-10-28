package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;

import javax.swing.*;
import java.awt.*;

public class PanelIniciarSesion extends PanelConFondo {

    private final JuegoController controller;
    private JTextField txtNombre;

    public PanelIniciarSesion(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 60, 28, 60));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        norte.setOpaque(false);
        norte.add(Recursos.crearBanner("Los Duendes Coleccionistas", 44));
        add(norte, BorderLayout.NORTH);

        PanelRedondeado tarjeta = new PanelRedondeado(new GridBagLayout(), Recursos.CREMA, Recursos.VERDE_OSCURO, 44);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(18, 40, 18, 40));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 14, 6, 14);

        JLabel lblTitulo = Recursos.crearEtiqueta("Iniciar sesión", 34, true, Recursos.CAFE_OSCURO);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        tarjeta.add(lblTitulo, gc);

        JPanel columnas = new JPanel(new GridLayout(1, 2, 30, 0));
        columnas.setOpaque(false);
        columnas.add(crearZonaCamara());
        columnas.add(crearZonaControles());
        gc.gridwidth = 2;
        gc.gridy = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        tarjeta.add(columnas, gc);

        JLabel lblNota = Recursos.crearEtiqueta("El registro se realiza una sola vez.", 20, false, Recursos.TEXTO);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.NONE;
        tarjeta.add(lblNota, gc);

        add(tarjeta, BorderLayout.CENTER);
    }
    private JPanel crearZonaCamara() {
        JPanel zona = new JPanel(new BorderLayout(0, 10));
        zona.setOpaque(false);
        PanelRedondeado camara = new PanelRedondeado(new BorderLayout(), new Color(38, 38, 40), Recursos.CAFE, 50) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight()) / 3;
                g2.setColor(new Color(120, 120, 125));
                g2.fillOval(getWidth() / 2 - d / 2, getHeight() / 2 - d, d, d);
                g2.fillArc(getWidth() / 2 - d, getHeight() / 2 + d / 10, 2 * d, 2 * d, 0, 180);
                g2.dispose();
            }
        };
        zona.add(camara, BorderLayout.CENTER);

        PanelRedondeado estado = new PanelRedondeado(new GridBagLayout(), Recursos.VERDE, Recursos.VERDE_OSCURO, 26);
        estado.setBorder(BorderFactory.createEmptyBorder(4, 20, 6, 20));
        estado.add(Recursos.crearEtiqueta(controller.getEstadoCamara(), 22, true, Recursos.CREMA));
        JPanel envoltorio = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        envoltorio.setOpaque(false);
        envoltorio.add(estado);
        zona.add(envoltorio, BorderLayout.SOUTH);
        return zona;
    }
    private JPanel crearZonaControles() {
        JPanel zona = new JPanel(new GridBagLayout());
        zona.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 0, 6, 0);

        JTextArea instrucciones = new JTextArea(
                "Coloca tu rostro frente a la cámara para iniciar sesión.\n\n"
                        + "Si es tu primera vez, registra tu rostro una sola vez.", 5, 20);
        instrucciones.setEditable(false);
        instrucciones.setFocusable(false);
        instrucciones.setOpaque(false);
        instrucciones.setLineWrap(true);
        instrucciones.setWrapStyleWord(true);
        instrucciones.setFont(Recursos.fuente(Font.PLAIN, 22));
        instrucciones.setForeground(Recursos.TEXTO);
        gc.gridy = 0;
        gc.weighty = 1;
        gc.anchor = GridBagConstraints.CENTER;
        zona.add(instrucciones, gc);

        gc.weighty = 0;
        Recursos.BotonBosque btnIniciar = Recursos.crearBoton("Iniciar sesión con mi rostro");
        btnIniciar.setFont(Recursos.fuente(Font.BOLD, 26));
        btnIniciar.addActionListener(evt -> controller.iniciarSesionDemo(txtNombre.getText()));
        gc.gridy = 1;
        gc.ipady = 14;
        zona.add(btnIniciar, gc);

        Recursos.BotonBosque btnRegistrar = Recursos.crearBoton("Registrar mi rostro", Recursos.ROJO);
        btnRegistrar.setFont(Recursos.fuente(Font.BOLD, 22));
        btnRegistrar.addActionListener(evt -> controller.registrarRostroDemo(txtNombre.getText()));
        gc.gridy = 2;
        gc.ipady = 8;
        zona.add(btnRegistrar, gc);

        JLabel lblNombre = Recursos.crearEtiqueta("Nombre de usuario", 24, true, Recursos.CAFE_OSCURO);
        lblNombre.setHorizontalAlignment(JLabel.CENTER);
        gc.gridy = 3;
        gc.ipady = 0;
        gc.insets = new Insets(14, 0, 4, 0);
        zona.add(lblNombre, gc);

        txtNombre = new JTextField();
        txtNombre.setFont(Recursos.fuente(Font.PLAIN, 24));
        txtNombre.setBackground(new Color(245, 228, 195));
        txtNombre.setForeground(Recursos.TEXTO);
        txtNombre.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Recursos.CAFE, 2),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        gc.gridy = 4;
        gc.insets = new Insets(0, 0, 6, 0);
        zona.add(txtNombre, gc);
        return zona;
    }
}
