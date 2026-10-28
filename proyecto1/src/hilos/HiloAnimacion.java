package hilos;

import javax.swing.*;

public class HiloAnimacion implements Runnable {

    private final JLabel etiquetaImagen;
    private final ImageIcon[] fotogramas;
    private final int[] retrasosMs;
    private final boolean repetir;
    private final Runnable alTerminar;
    private volatile boolean enEjecucion = true;

    public HiloAnimacion(JLabel etiquetaImagen, ImageIcon[] fotogramas, int[] retrasosMs) {
        this(etiquetaImagen, fotogramas, retrasosMs, true, null);
    }

    public HiloAnimacion(JLabel etiquetaImagen, ImageIcon[] fotogramas, int[] retrasosMs,
                         boolean repetir, Runnable alTerminar) {
        this.etiquetaImagen = etiquetaImagen;
        this.fotogramas = fotogramas;
        this.retrasosMs = retrasosMs;
        this.repetir = repetir;
        this.alTerminar = alTerminar;
    }
    public void detener() {
        enEjecucion = false;
    }

    @Override
    public void run() {
        int indice = 0;
        while (enEjecucion && fotogramas.length > 0) {
            final ImageIcon fotogramaActual = fotogramas[indice];
            SwingUtilities.invokeLater(() -> etiquetaImagen.setIcon(fotogramaActual));

            try {
                Thread.sleep(retrasosMs[indice]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                enEjecucion = false;
                break;
            }

            indice++;
            if (indice >= fotogramas.length) {
                if (repetir) {
                    indice = 0;
                } else {
                    break;
                }
            }
        }
        boolean terminoSola = enEjecucion;
        enEjecucion = false;
        if (terminoSola && alTerminar != null) {
            SwingUtilities.invokeLater(alTerminar);
        }
    }
}
