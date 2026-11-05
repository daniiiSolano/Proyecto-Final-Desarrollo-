package apoyo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CampoTexto extends JTextField {

    private final BufferedImage fondo;

    public CampoTexto(String rutaFondo) {
        fondo = Imagenes.cargar(rutaFondo);
        setFont(Recursos.fuente(Font.PLAIN, 24));
        setForeground(Recursos.TEXTO);
        setCaretColor(Recursos.TEXTO);
        if (fondo != null) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        } else {
            setBackground(new Color(245, 228, 195));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Recursos.CAFE, 2),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (fondo != null) {
            Imagenes.pintar(g, fondo, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (fondo != null) {
            d.height = Math.max(d.height, fondo.getHeight());
        }
        return d;
    }
}