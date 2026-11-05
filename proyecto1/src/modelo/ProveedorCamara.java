package modelo;

import java.awt.image.BufferedImage;

public interface ProveedorCamara {

    String getEstado();

    BufferedImage capturar();
}