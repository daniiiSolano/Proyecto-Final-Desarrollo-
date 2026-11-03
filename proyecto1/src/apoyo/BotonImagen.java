package apoyo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Botón cuyo aspecto es una imagen (normal y presionado); el texto se pinta encima. */
public class BotonImagen extends JButton {

    private BufferedImage normal;
    private BufferedImage presionado;

    public BotonImagen(String texto, String rutaNormal, String rutaPresionado) {
        super(texto);
        setFocusPainted(false);
        setMargin(new Insets(8, 26, 8, 26));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Recursos.fuente(Font.BOLD, 22));
        setImagenes(rutaNormal, rutaPresionado);
    }

    public final void setImagenes(String rutaNormal, String rutaPresionado) {
        normal = Imagenes.cargar(rutaNormal);
        presionado = Imagenes.cargar(rutaPresionado);
        boolean hay = normal != null;
        setContentAreaFilled(!hay);
        setBorderPainted(!hay);
        setOpaque(false);
        setForeground(hay ? Recursos.CREMA : Color.BLACK);
        repaint();
    }

    /** Cambia el color del texto solo cuando hay imagen (sin imagen se conserva el aspecto estándar). */
    public void setColorTextoSobreImagen(Color color) {
        if (normal != null) {
            setForeground(color);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (normal != null) {
            BufferedImage img = (getModel().isPressed() && presionado != null) ? presionado : normal;
            Imagenes.pintar(g, img, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        if (normal != null && !isPreferredSizeSet()) {
            return new Dimension(normal.getWidth(), normal.getHeight());
        }
        return super.getPreferredSize();
    }
}