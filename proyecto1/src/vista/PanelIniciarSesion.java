package vista;

import apoyo.BotonImagen;
import apoyo.CampoClave;
import apoyo.CampoTexto;
import apoyo.PanelConFondo;
import apoyo.PanelImagen;
import apoyo.Recursos;
import apoyo.RutasLogin;
import controlador.JuegoController;

import javax.swing.*;
import java.awt.*;

public class PanelIniciarSesion extends PanelConFondo {

    private static final String VISTA_ROSTRO = "rostro";
    private static final String VISTA_CREDENCIALES = "credenciales";

    private final JuegoController controller;
    private final CardLayout cartas = new CardLayout();
    private JPanel zonaVistas;
    private BotonImagen pestanaRostro;
    private BotonImagen pestanaCredenciales;
    private CampoTexto txtIdentificador;
    private CampoClave txtClave;
    private JCheckBox chkMostrar;

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
        norte.add(ComponentesLogin.banner("Los Duendes Coleccionistas"));
        add(norte, BorderLayout.NORTH);

        PanelImagen tarjeta = new PanelImagen(RutasLogin.TARJETA, new BorderLayout(0, 14), new Dimension(900, 500))
                .respaldo(Recursos.CREMA);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(22, 44, 18, 44));
        tarjeta.add(crearPestanas(), BorderLayout.NORTH);

        zonaVistas = new JPanel(cartas);
        zonaVistas.setOpaque(false);
        zonaVistas.add(crearVistaRostro(), VISTA_ROSTRO);
        zonaVistas.add(crearVistaCredenciales(), VISTA_CREDENCIALES);
        tarjeta.add(zonaVistas, BorderLayout.CENTER);

        JLabel nota = Recursos.crearEtiqueta("Debes iniciar sesión para poder jugar.", 18, false, Recursos.TEXTO);
        nota.setHorizontalAlignment(JLabel.CENTER);
        tarjeta.add(nota, BorderLayout.SOUTH);
        add(tarjeta, BorderLayout.CENTER);

        mostrarVista(VISTA_ROSTRO);
    }

    private JPanel crearPestanas() {
        pestanaRostro = crearPestana("Rostro (Face ID)", VISTA_ROSTRO);
        pestanaCredenciales = crearPestana("Correo y contraseña", VISTA_CREDENCIALES);
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        fila.setOpaque(false);
        fila.add(pestanaRostro);
        fila.add(pestanaCredenciales);
        return fila;
    }

    private BotonImagen crearPestana(String texto, String vista) {
        BotonImagen b = new BotonImagen(texto, RutasLogin.PESTANA_INACTIVA, null);
        b.setFont(Recursos.fuente(Font.BOLD, 22));
        b.addActionListener(e -> mostrarVista(vista));
        return b;
    }

    private void mostrarVista(String vista) {
        cartas.show(zonaVistas, vista);
        boolean rostro = VISTA_ROSTRO.equals(vista);
        pestanaRostro.setImagenes(rostro ? RutasLogin.PESTANA_ACTIVA : RutasLogin.PESTANA_INACTIVA, null);
        pestanaCredenciales.setImagenes(rostro ? RutasLogin.PESTANA_INACTIVA : RutasLogin.PESTANA_ACTIVA, null);
        pestanaRostro.setColorTextoSobreImagen(rostro ? Recursos.CREMA : Recursos.TEXTO);
        pestanaCredenciales.setColorTextoSobreImagen(rostro ? Recursos.TEXTO : Recursos.CREMA);
        pestanaRostro.setFont(Recursos.fuente(rostro ? Font.BOLD : Font.PLAIN, 22));
        pestanaCredenciales.setFont(Recursos.fuente(rostro ? Font.PLAIN : Font.BOLD, 22));
    }

    private JPanel crearVistaRostro() {
        JPanel vista = new JPanel(new GridLayout(1, 2, 40, 0));
        vista.setOpaque(false);

        JPanel izquierda = new JPanel(new BorderLayout(0, 12));
        izquierda.setOpaque(false);
        JLabel estado = Recursos.crearEtiqueta(controller.getEstadoCamara(), 20, true, Recursos.CREMA);
        izquierda.add(ComponentesLogin.marcoCamara(controller, estado), BorderLayout.CENTER);
        izquierda.add(ComponentesLogin.centrado(ComponentesLogin.pill(estado)), BorderLayout.SOUTH);
        vista.add(izquierda);

        JPanel derecha = new JPanel(new GridBagLayout());
        derecha.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 0, 8, 0);

        gc.gridy = 0;
        derecha.add(ComponentesLogin.texto(
                "Mira de frente a la cámara. Reconoceremos tu rostro y entrarás directo a la historia.", 24), gc);

        BotonImagen btnRostro = ComponentesLogin.botonVerde("Iniciar sesión con mi rostro", 26);
        btnRostro.addActionListener(e -> controller.iniciarSesionConRostro());
        gc.gridy = 1;
        derecha.add(btnRostro, gc);

        JLabel o = Recursos.crearEtiqueta("— o —", 20, false, Recursos.TEXTO);
        o.setHorizontalAlignment(JLabel.CENTER);
        gc.gridy = 2;
        derecha.add(o, gc);

        BotonImagen btnCred = ComponentesLogin.botonCafe("Usar correo y contraseña", 22);
        btnCred.addActionListener(e -> mostrarVista(VISTA_CREDENCIALES));
        gc.gridy = 3;
        derecha.add(btnCred, gc);

        gc.gridy = 4;
        derecha.add(ComponentesLogin.centrado(ComponentesLogin.enlace("¿Primera vez? Crear cuenta",
                () -> controller.irA(VentanaPrincipal.REGISTRO))), gc);
        vista.add(derecha);
        return vista;
    }

    private JPanel crearVistaCredenciales() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 0, 6, 0);

        txtIdentificador = new CampoTexto(RutasLogin.CAMPO);
        txtClave = new CampoClave(RutasLogin.CAMPO);
        gc.gridy = 0;
        form.add(ComponentesLogin.columna("Correo o nombre de usuario", txtIdentificador), gc);
        gc.gridy = 1;
        form.add(ComponentesLogin.columna("Contraseña", txtClave), gc);

        chkMostrar = ComponentesLogin.casilla("Mostrar contraseña");
        chkMostrar.addActionListener(e -> txtClave.setEchoChar(chkMostrar.isSelected() ? (char) 0 : '\u2022'));
        JPanel opciones = new JPanel(new BorderLayout());
        opciones.setOpaque(false);
        opciones.add(chkMostrar, BorderLayout.WEST);
        opciones.add(ComponentesLogin.enlace("¿Olvidaste tu contraseña?", controller::recuperarClave),
                BorderLayout.EAST);
        gc.gridy = 2;
        form.add(opciones, gc);

        BotonImagen btnEntrar = ComponentesLogin.botonVerde("Entrar", 28);
        btnEntrar.addActionListener(e -> controller.iniciarSesionConCredenciales(
                txtIdentificador.getText(), txtClave.getPassword()));
        txtClave.addActionListener(e -> btnEntrar.doClick());
        txtIdentificador.addActionListener(e -> txtClave.requestFocusInWindow());
        gc.gridy = 3;
        form.add(btnEntrar, gc);

        JPanel enlaces = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        enlaces.setOpaque(false);
        enlaces.add(ComponentesLogin.enlace("Crear cuenta", () -> controller.irA(VentanaPrincipal.REGISTRO)));
        enlaces.add(ComponentesLogin.enlace("Entrar con mi rostro", () -> mostrarVista(VISTA_ROSTRO)));
        gc.gridy = 4;
        form.add(enlaces, gc);
        form.setPreferredSize(new Dimension(560, form.getPreferredSize().height));

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(form);
        return centro;
    }

    /** Vacía los campos y vuelve a la pestaña de rostro. */
    public void limpiar() {
        txtIdentificador.setText("");
        txtClave.setText("");
        chkMostrar.setSelected(false);
        txtClave.setEchoChar('\u2022');
        mostrarVista(VISTA_ROSTRO);
    }
}