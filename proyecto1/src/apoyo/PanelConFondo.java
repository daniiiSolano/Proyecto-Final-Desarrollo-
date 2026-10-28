package apoyo;

import javax.swing.*;
import java.awt.*;

public class PanelConFondo extends JPanel {

    private final Image fondo;

    public PanelConFondo(String rutaImagen) {
        ImageIcon icono = new ImageIcon(rutaImagen);
        fondo = icono.getIconWidth() > 0 ? icono.getImage() : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        } else {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(new GradientPaint(0, 0, Recursos.VERDE_OSCURO, 0, getHeight(), Recursos.CAFE_OSCURO));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}