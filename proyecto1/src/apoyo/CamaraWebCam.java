package apoyo;

import modelo.ProveedorCamara;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Webcam real (JavaCV/OpenCV). Se enciende cuando alguna pantalla la necesita (usar) y se apaga
 * cuando nadie la usa (soltar), así la luz de la cámara no queda encendida sin motivo.
 */
public class CamaraWebCam implements ProveedorCamara {

    private int usos = 0;
    private Thread hilo;
    private volatile BufferedImage ultimo;
    private volatile String estado = "Cámara apagada";

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
        hilo = new Thread(this::bucle, "HiloCamara");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void bucle() {
        boolean fallo = false;
        OpenCVFrameGrabber grabador = new OpenCVFrameGrabber(0);
        Java2DFrameConverter conversor = new Java2DFrameConverter();
        try {
            estado = "Abriendo cámara…";
            grabador.start();
            estado = "Cámara activa";
            while (hayUsos()) {
                Frame cuadro = grabador.grab();
                if (cuadro == null || cuadro.image == null) {
                    Thread.sleep(10);
                    continue;
                }
                BufferedImage img = conversor.convert(cuadro);
                if (img != null) {
                    ultimo = copiar(img);
                }
            }
        } catch (Exception | LinkageError e) {
            fallo = true;
            estado = "No se pudo abrir la cámara";
        } finally {
            try {
                grabador.stop();
                grabador.release();
            } catch (Exception | LinkageError e) {
                // se ignora: ya se estaba cerrando
            }
            ultimo = null;
            if (!fallo) {
                estado = "Cámara apagada";
            }
            synchronized (this) {
                hilo = null;
                if (!fallo && usos > 0) {
                    iniciarHilo();
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