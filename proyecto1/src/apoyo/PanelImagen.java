package apoyo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PanelImagen extends JPanel {

    private final BufferedImage imagen;
    private final Dimension tamanoRespaldo;

    public PanelImagen(String ruta, LayoutManager layout, Dimension tamanoRespaldo) {
        super(layout);
        this.imagen = Imagenes.cargar(ruta);
        this.tamanoRespaldo = tamanoRespaldo;
        setOpaque(false);
    }

    public PanelImagen respaldo(Color color) {
        if (imagen == null) {
            setOpaque(true);
            setBackground(color);
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (imagen != null) {
            Imagenes.pintar(g, imagen, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return imagen != null ? new Dimension(imagen.getWidth(), imagen.getHeight()) : tamanoRespaldo;
    }
}