package hilos;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class HiloSonido implements Runnable {

    private final File archivoSonido;
    private volatile boolean enEjecucion = true;
    private volatile Player reproductorActual;

    public HiloSonido(File archivoSonido) {
        this.archivoSonido = archivoSonido;
    }

    public void detener() {
        enEjecucion = false;
        if (reproductorActual != null) {
            reproductorActual.close();
        }
    }

    @Override
    public void run() {
        if (!archivoSonido.exists()) {
            System.err.println("No se encontró el archivo de sonido: " + archivoSonido.getPath());
            return;
        }
        while (enEjecucion) {
            try (FileInputStream flujoArchivo = new FileInputStream(archivoSonido);
                 BufferedInputStream flujoConBuffer = new BufferedInputStream(flujoArchivo)) {

                reproductorActual = new Player(flujoConBuffer);
                reproductorActual.play();

            } catch (Throwable e) {
                System.err.println("No se pudo reproducir el sonido: " + e);
                break;
            }
        }
    }
}
