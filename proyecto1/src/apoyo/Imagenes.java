package apoyo;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Imagenes {

    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private Imagenes() {
    }

    public static BufferedImage cargar(String ruta) {
        if (ruta == null) {
            return null;
        }
        BufferedImage imagen = CACHE.get(ruta);
        if (imagen != null) {
            return imagen;
        }
        File archivo = new File(Recursos.CARPETA_IMAGENES + ruta);
        if (!archivo.exists()) {
            return null;
        }
        try {
            imagen = ImageIO.read(archivo);
        } catch (IOException e) {
            return null;
        }
        if (imagen != null) {
            CACHE.put(ruta, imagen);
        }
        return imagen;
    }

    public static ImageIcon icono(String ruta) {
        BufferedImage imagen = cargar(ruta);
        return imagen == null ? null : new ImageIcon(imagen);
    }

    public static void pintar(Graphics g, BufferedImage imagen, int ancho, int alto) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(imagen, 0, 0, ancho, alto, null);
        g2.dispose();
    }
}
