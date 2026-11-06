package apoyo;

import modelo.ProveedorCamara;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


public class CamaraWebCam implements ProveedorCamara {

    private int usos = 0;
    private Thread hilo;
    private volatile BufferedImage ultimo;
    private volatile String estado = "Cámara apagada";

    private static final long REINTENTO_MS = 2500;        // espera antes de reintentar abrir la cámara
    private static final long SIN_IMAGEN_AVISO_MS = 3000;  // sin imagen: se avisa en el estado
    private static final long SIN_IMAGEN_MAX_MS = 8000;    // sin imagen: se cierra y se reintenta

    public synchronized void usar() {
        usos++;
        if (hilo == null) {
            iniciarHilo();
        }
    }

    public synchronized void soltar() {
        if (usos > 0) {
            usos--;
        }
    }

    @Override
    public String getEstado() {
        return estado;
    }

    @Override
    public BufferedImage capturar() {
        return ultimo;
    }

    public BufferedImage getFotograma() {
        return ultimo;
    }

    private synchronized boolean hayUsos() {
        return usos > 0;
    }

    private void iniciarHilo() {
        iniciarHilo(0);
    }

    /** Arranca el hilo de la cámara; si retrasoMs es mayor que 0 espera antes de abrirla (reintentos). */
    private void iniciarHilo(long retrasoMs) {
        hilo = new Thread(() -> {
            if (retrasoMs > 0) {
                try {
                    Thread.sleep(retrasoMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            bucle();
        }, "HiloCamara");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void bucle() {
        // Si mientras se esperaba el reintento ya nadie usa la cámara, no se abre
        synchronized (this) {
            if (usos <= 0) {
                hilo = null;
                estado = "Cámara apagada";
                return;
            }
        }
        boolean fallo = false;
        boolean sinLibreria = false;
        OpenCVFrameGrabber grabador = null;
        try {
            grabador = new OpenCVFrameGrabber(0);
            Java2DFrameConverter conversor = new Java2DFrameConverter();
            estado = "Abriendo cámara…";
            grabador.start();
            estado = "Cámara activa";
            long ultimoCuadro = System.currentTimeMillis();
            while (hayUsos()) {
                Frame cuadro = grabador.grab();
                if (cuadro == null || cuadro.image == null) {
                    long sinImagen = System.currentTimeMillis() - ultimoCuadro;
                    if (sinImagen > SIN_IMAGEN_MAX_MS) {
                        // Se abrió pero no manda nada (típico si otra app la tiene): se cierra y se reintenta
                        throw new IllegalStateException("La cámara no envía imágenes");
                    }
                    if (sinImagen > SIN_IMAGEN_AVISO_MS) {
                        estado = "La cámara no envía imagen";
                    }
                    Thread.sleep(10);
                    continue;
                }
                BufferedImage img = conversor.convert(cuadro);
                if (img != null) {
                    ultimo = copiar(img);
                    ultimoCuadro = System.currentTimeMillis();
                    if (!"Cámara activa".equals(estado)) {
                        estado = "Cámara activa";
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            fallo = true;
            sinLibreria = e instanceof LinkageError;
            estado = sinLibreria ? "Falta la librería de la cámara"
                    : "Cámara ocupada o no disponible";
            System.err.println("No se pudo usar la cámara (¿la está usando otra aplicación como Zoom?): " + e);
        } finally {
            // Siempre se libera la cámara para que otras aplicaciones (o el siguiente intento) puedan usarla
            try {
                if (grabador != null) {
                    grabador.stop();
                    grabador.release();
                }
            } catch (Exception | LinkageError e) {
                // se ignora: ya se estaba cerrando
            }
            ultimo = null;
            if (!fallo) {
                estado = "Cámara apagada";
            }
            synchronized (this) {
                hilo = null;
                if (usos > 0 && !sinLibreria) {
                    // Antes no se reintentaba tras un fallo: si Zoom tenía la cámara, quedaba sin imagen
                    // hasta cambiar de pantalla. Ahora se reintenta sola cada pocos segundos.
                    iniciarHilo(fallo ? REINTENTO_MS : 0);
                }
            }
        }
    }

    /** El conversor reutiliza su imagen interna, por eso se copia cada fotograma. */
    private static BufferedImage copiar(BufferedImage origen) {
        BufferedImage copia = new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = copia.createGraphics();
        g.drawImage(origen, 0, 0, null);
        g.dispose();
        return copia;
    }
}
