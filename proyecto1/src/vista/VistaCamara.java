package vista;

import apoyo.Imagenes;
import apoyo.RutasLogin;
import controlador.JuegoController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;

// Muestra la imagen de la webcam (espejada). Enciende la cámara al verse y la suelta al ocultarse. //

class VistaCamara extends JComponent {

    private final JuegoController controller;
    private final JLabel lblEstado;
    private final Timer temporizador;
    private BufferedImage fotograma;
    private boolean usando = false;

    VistaCamara(JuegoController controller, JLabel lblEstado) {
        this.controller = controller;
        this.lblEstado = lblEstado;
        setPreferredSize(new Dimension(320, 240));
        temporizador = new Timer(40, e -> {
            fotograma = controller.getFotogramaCamara();
            actualizarEstado();
            repaint();
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing() && !usando) {
                usando = true;
                controller.usarCamara();
                temporizador.start();
            } else if (!isShowing() && usando) {
                usando = false;
                temporizador.stop();
                controller.soltarCamara();
                fotograma = null;
                repaint();
            }
        });
    }

    private void actualizarEstado() {
        if (lblEstado != null) {
            String texto = controller.getEstadoCamara();
            if (!texto.equals(lblEstado.getText())) {
                lblEstado.setText(texto);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        BufferedImage img = fotograma;
        if (img != null) {
            double factor = Math.min((double) getWidth() / img.getWidth(), (double) getHeight() / img.getHeight());
            int ancho = (int) (img.getWidth() * factor);
            int alto = (int) (img.getHeight() * factor);
            int x = (getWidth() - ancho) / 2;
            int y = (getHeight() - alto) / 2;
            g2.drawImage(img, x + ancho, y, -ancho, alto, null);   // espejo horizontal
        } else {
            BufferedImage silueta = Imagenes.cargar(RutasLogin.SILUETA);
            if (silueta != null) {
                g2.drawImage(silueta, (getWidth() - silueta.getWidth()) / 2,
                        (getHeight() - silueta.getHeight()) / 2, null);
            }
        }
        g2.dispose();
    }
}