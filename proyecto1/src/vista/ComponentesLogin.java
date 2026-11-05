package vista;

import apoyo.BotonImagen;
import apoyo.Imagenes;
import apoyo.PanelImagen;
import apoyo.Recursos;
import apoyo.RutasLogin;
import controlador.JuegoController;

import javax.swing.*;
import java.awt.*;

final class ComponentesLogin {

    private ComponentesLogin() {
    }

    static PanelImagen banner(String texto) {
        PanelImagen b = new PanelImagen(RutasLogin.BANNER, new GridBagLayout(), new Dimension(720, 80))
                .respaldo(Recursos.CAFE);
        b.setBorder(BorderFactory.createEmptyBorder(6, 44, 10, 44));
        b.add(Recursos.crearEtiqueta(texto, 40, true, Recursos.CREMA));
        return b;
    }

    static BotonImagen botonVerde(String texto, int tamano) {
        return boton(texto, tamano, RutasLogin.BOTON_VERDE, RutasLogin.BOTON_VERDE_PRES);
    }

    static BotonImagen botonCafe(String texto, int tamano) {
        return boton(texto, tamano, RutasLogin.BOTON_CAFE, RutasLogin.BOTON_CAFE_PRES);
    }

    private static BotonImagen boton(String texto, int tamano, String normal, String presionado) {
        BotonImagen b = new BotonImagen(texto, normal, presionado);
        b.setFont(Recursos.fuente(Font.BOLD, tamano));
        return b;
    }

    static JButton enlace(String texto, Runnable accion) {
        JButton b = new JButton("<html><u>" + texto + "</u></html>");
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(Recursos.CAFE_OSCURO);
        b.setFont(Recursos.fuente(Font.PLAIN, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> accion.run());
        return b;
    }

    static PanelImagen marcoCamara(JuegoController controller, JLabel estado) {
        PanelImagen marco = new PanelImagen(RutasLogin.MARCO_CAMARA, new BorderLayout(), new Dimension(480, 360))
                .respaldo(new Color(38, 38, 40));
        marco.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        marco.add(new VistaCamara(controller, estado), BorderLayout.CENTER);
        return marco;
    }

    static PanelImagen pill(JLabel etiqueta) {
        PanelImagen p = new PanelImagen(RutasLogin.PILL, new GridBagLayout(), new Dimension(360, 44))
                .respaldo(Recursos.VERDE);
        p.add(etiqueta);
        return p;
    }

    static JCheckBox casilla(String texto) {
        JCheckBox c = new JCheckBox(texto);
        c.setOpaque(false);
        c.setFocusPainted(false);
        c.setFont(Recursos.fuente(Font.PLAIN, 20));
        c.setForeground(Recursos.TEXTO);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ImageIcon off = Imagenes.icono(RutasLogin.CHECK_OFF);
        ImageIcon on = Imagenes.icono(RutasLogin.CHECK_ON);
        if (off != null && on != null) {
            c.setIcon(off);
            c.setSelectedIcon(on);
        }
        return c;
    }

    static JTextArea texto(String contenido, int tamano) {
        JTextArea t = new JTextArea(contenido);
        t.setEditable(false);
        t.setFocusable(false);
        t.setOpaque(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setFont(Recursos.fuente(Font.PLAIN, tamano));
        t.setForeground(Recursos.TEXTO);
        return t;
    }

    static JPanel centrado(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    static JPanel columna(String etiqueta, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(Recursos.crearEtiqueta(etiqueta, 20, true, Recursos.CAFE_OSCURO), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }
}