import controlador.JuegoController;
import vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {

            }
            JuegoController controller = new JuegoController();
            VentanaPrincipal vista = new VentanaPrincipal(controller);
            controller.setVista(vista);
            vista.setVisible(true);
            controller.iniciar();
        });
    }
}
