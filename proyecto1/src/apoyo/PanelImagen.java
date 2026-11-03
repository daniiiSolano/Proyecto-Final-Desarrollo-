package apoyo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Panel cuyo fondo es una imagen PNG estirada al tamaño del panel. */
public class PanelImagen extends JPanel {

    private final BufferedImage imagen;
    private final Dimension tamanoRespaldo;

    public PanelImagen(String ruta, LayoutManager layout, Dimension tamanoRespaldo) {
        super(layout);
        this.imagen = Imagenes.cargar(ruta);
        this.tamanoRespaldo = tamanoRespaldo;
        setOpaque(false);
    }

    /** Si la imagen no existe, usa un color liso de Swing para que la pantalla siga siendo usable. */
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