package vista;

import apoyo.BotonImagen;
import apoyo.CampoClave;
import apoyo.CampoTexto;
import apoyo.Imagenes;
import apoyo.PanelConFondo;
import apoyo.PanelImagen;
import apoyo.Recursos;
import apoyo.RutasLogin;
import controlador.JuegoController;
import modelo.AutenticadorFacial;

import javax.swing.*;
import java.awt.*;

public class PanelRegistro extends PanelConFondo {

    private static final int PASOS = AutenticadorFacial.MUESTRAS_REQUERIDAS;
    // El registro usa una sola fotografía de frente (no se toman fotos desde otros ángulos)
    private static final String[] PISTAS = {
            "mira al frente"
    };

    private final JuegoController controller;
    private final JLabel[] indicadores = new JLabel[PASOS];

    private CampoTexto txtNombre;
    private CampoTexto txtApellido;
    private CampoTexto txtCorreo;
    private CampoTexto txtUsuario;
    private CampoClave txtClave;
    private CampoClave txtConfirmar;
    private JLabel lblPaso;

    public PanelRegistro(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 60, 10, 60));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        norte.setOpaque(false);
        norte.add(ComponentesLogin.banner("Crear cuenta"));
        add(norte, BorderLayout.NORTH);

        PanelImagen tarjeta = new PanelImagen(
                RutasLogin.TARJETA,
                new BorderLayout(0, 12),
                new Dimension(900, 560)
        ).respaldo(Recursos.CREMA);

        tarjeta.setBorder(
                BorderFactory.createEmptyBorder(20, 44, 16, 44)
        );

        JPanel columnas = new JPanel(new GridLayout(1, 2, 40, 0));
        columnas.setOpaque(false);
        columnas.add(crearFormulario());
        columnas.add(crearZonaRostro());

        tarjeta.add(columnas, BorderLayout.CENTER);
        tarjeta.add(crearPie(), BorderLayout.SOUTH);

        add(tarjeta, BorderLayout.CENTER);
    }

    private JPanel crearFormulario() {
        txtNombre = new CampoTexto(RutasLogin.CAMPO);
        txtApellido = new CampoTexto(RutasLogin.CAMPO);
        txtCorreo = new CampoTexto(RutasLogin.CAMPO);
        txtUsuario = new CampoTexto(RutasLogin.CAMPO);
        txtClave = new CampoClave(RutasLogin.CAMPO);
        txtConfirmar = new CampoClave(RutasLogin.CAMPO);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
        form.setOpaque(false);

        form.add(fila(
                ComponentesLogin.columna("Nombre", txtNombre),
                ComponentesLogin.columna("Apellido", txtApellido)
        ));

        form.add(ComponentesLogin.columna("Correo electrónico", txtCorreo));
        form.add(ComponentesLogin.columna("Nombre de usuario", txtUsuario));

        form.add(fila(
                ComponentesLogin.columna("Contraseña", txtClave),
                ComponentesLogin.columna("Confirmar contraseña", txtConfirmar)
        ));

        form.add(Recursos.crearEtiqueta(
                "Mínimo 8 caracteres, con letras y números.",
                18, false, Recursos.TEXTO
        ));

        return form;
    }

    private JPanel fila(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        return p;
    }

    private JPanel crearZonaRostro() {
        JPanel zona = new JPanel(new BorderLayout(0, 10));
        zona.setOpaque(false);

        JLabel titulo = Recursos.crearEtiqueta(
                "Registro facial (Face ID)",
                22, true, Recursos.CAFE_OSCURO
        );

        titulo.setHorizontalAlignment(JLabel.CENTER);
        zona.add(titulo, BorderLayout.NORTH);

        zona.add(
                ComponentesLogin.marcoCamara(controller, null),
                BorderLayout.CENTER
        );

        JPanel sur = new JPanel();
        sur.setOpaque(false);
        sur.setLayout(new BoxLayout(sur, BoxLayout.Y_AXIS));

        lblPaso = Recursos.crearEtiqueta(
                "", 18, true, Recursos.CREMA
        );

        // El indicador de pasos y los botones van en filas compartidas para dejarle altura a la cámara
        JPanel pasos = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 0)
        );
        pasos.setOpaque(false);
        pasos.add(ComponentesLogin.pill(lblPaso));

        for (int i = 0; i < PASOS; i++) {
            indicadores[i] = new JLabel("", JLabel.CENTER);
            indicadores[i].setFont(
                    Recursos.fuente(Font.BOLD, 20)
            );
            indicadores[i].setForeground(Recursos.CAFE_OSCURO);
            pasos.add(indicadores[i]);
        }

        sur.add(pasos);
        sur.add(Box.createVerticalStrut(8));

        BotonImagen btnCapturar = ComponentesLogin.botonCafe(
                "Capturar rostro", 22
        );
        btnCapturar.setPreferredSize(new Dimension(280, 54));

        btnCapturar.addActionListener(e -> {
            btnCapturar.setEnabled(false);

            controller.capturarMuestraRostro(n -> {
                SwingUtilities.invokeLater(() -> {
                    btnCapturar.setEnabled(true);
                    actualizarPasos(n);
                });
            });
        });

        JPanel acciones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 0)
        );
        acciones.setOpaque(false);
        acciones.add(btnCapturar);
        acciones.add(
                ComponentesLogin.enlace("Descartar foto", () -> {
                    controller.descartarMuestrasRostro();
                    actualizarPasos(0);
                })
        );
        sur.add(acciones);
        sur.add(Box.createVerticalStrut(4));

        sur.add(ComponentesLogin.texto(
                "Se toma una sola foto de frente, cuando tu rostro esté centrado y quieto. "
                        + "Es opcional, pero sin rostro solo podrás entrar con contraseña.",
                16
        ));

        zona.add(sur, BorderLayout.SOUTH);

        actualizarPasos(0);

        return zona;
    }

    private JPanel crearPie() {
        JPanel pie = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 30, 4)
        );
        pie.setOpaque(false);

        BotonImagen btnCrear = ComponentesLogin.botonVerde(
                "Crear cuenta", 28
        );

        btnCrear.addActionListener(e -> controller.registrarUsuario(
                txtNombre.getText(),
                txtApellido.getText(),
                txtCorreo.getText(),
                txtUsuario.getText(),
                txtClave.getPassword(),
                txtConfirmar.getPassword()
        ));

        pie.add(btnCrear);

        pie.add(Recursos.crearEtiqueta(
                "¿Ya tienes cuenta?", 20, false, Recursos.TEXTO
        ));

        pie.add(ComponentesLogin.enlace(
                "Iniciar sesión",
                () -> controller.irA(VentanaPrincipal.LOGIN)
        ));

        return pie;
    }

    private void actualizarPasos(int n) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> actualizarPasos(n));
            return;
        }

        lblPaso.setText(
                n >= PASOS
                        ? "Foto frontal lista"
                        : "Foto frontal · " + PISTAS[Math.min(n, PISTAS.length - 1)]
        );

        for (int i = 0; i < PASOS; i++) {
            boolean hecho = i < n;

            ImageIcon icono = Imagenes.icono(hecho ? RutasLogin.CHECK_ON : RutasLogin.CHECK_OFF);

            indicadores[i].setIcon(icono);
            indicadores[i].setText(icono == null ? (hecho ? "●" : "○") : "");
        }
    }

    public void limpiar() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::limpiar);
            return;
        }

        txtNombre.setText("");
        txtApellido.setText("");
        txtCorreo.setText("");
        txtUsuario.setText("");
        txtClave.setText("");
        txtConfirmar.setText("");

        controller.descartarMuestrasRostro();
        actualizarPasos(0);
    }
}
