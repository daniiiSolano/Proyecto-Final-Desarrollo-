package modelo;

import java.awt.image.BufferedImage;

public interface ProveedorCamara {

    String getEstado();

    /** Último fotograma de la cámara (BGR de 3 canales), o null si la cámara no está entregando imagen. */
    BufferedImage capturar();
}