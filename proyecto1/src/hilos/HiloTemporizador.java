package hilos;
public class HiloTemporizador implements Runnable {

    private final int segundos;
    private final Runnable alTerminar;
    private volatile boolean enEjecucion = true;

    public HiloTemporizador(int segundos, Runnable alTerminar) {
        this.segundos = segundos;
        this.alTerminar = alTerminar;
    }

    public int getSegundos() {
        return segundos;
    }

    public void detener() {
        enEjecucion = false;
    }

    @Override
    public void run() {
    }
}