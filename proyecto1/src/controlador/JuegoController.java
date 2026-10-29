package controlador;

import apoyo.Recursos;
import hilos.HiloSonido;
import Modelo.AutenticadorFacial;
import Modelo.GestorPremios;
import Modelo.Jugador;
import Modelo.LibroHistoria;
import Modelo.ModoClasico;
import Modelo.ModoPatron;
import Modelo.TableroMatriz;
import vista.VentanaPrincipal;

import java.io.File;
import java.util.List;
public class JuegoController {

    private final ModoClasico modoClasico;
    private final ModoPatron modoPatron;
    private final LibroHistoria libro;
    private final GestorPremios gestorPremios;
    private final AutenticadorFacial autenticador;

    private Jugador jugadorActual;
    private VentanaPrincipal vista;
    private HiloSonido tareaSonido;
    private Thread hiloSonido;

    public JuegoController() {
        modoClasico = new ModoClasico(new Jugador("Jugador 1"), new Jugador("Jugador 2"));
        modoPatron = new ModoPatron();
        libro = new LibroHistoria();
        gestorPremios = new GestorPremios();
        autenticador = new AutenticadorFacial();
    }

    public void setVista(VentanaPrincipal vista) {
        this.vista = vista;
    }
    public void iniciar() {
        vista.mostrarPantalla(VentanaPrincipal.LOGIN);
        iniciarMusica();
        vista.actualizarBotonMusica(estaSonando());
    }
    public void irA(String pantalla) {
        vista.mostrarPantalla(pantalla);
    }
    public void volverAlMenu() {
        vista.mostrarPantalla(VentanaPrincipal.HISTORIA);
    }
    public void volverAlInicio() {
        libro.reiniciar();
        vista.reiniciarHistoria();
        vista.mostrarPantalla(VentanaPrincipal.LOGIN);
    }
    public String getEstadoCamara() {
        return autenticador.getEstadoCamara();
    }
    public void iniciarSesionDemo(String nombre) {
        String usuario = (nombre == null || nombre.trim().isEmpty()) ? "Invitado" : nombre.trim();
        jugadorActual = new Jugador(usuario);
        vista.mostrarPantalla(VentanaPrincipal.HISTORIA);
    }
    public void registrarRostroDemo(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            vista.mostrarMensaje("Escribe un nombre de usuario para registrar tu rostro.");
            return;
        }
        jugadorActual = new Jugador(nombre.trim());
        vista.mostrarPantalla(VentanaPrincipal.HISTORIA);
    }

    public String getNombreUsuarioActual() {
        return jugadorActual == null ? "" : jugadorActual.getNombre();
    }
    public int getTotalPaginas() {
        return libro.getTotalPaginas();
    }

    public int getPaginaActual() {
        return libro.getIndicePaginaActual();
    }

    public String getRutaImagenPagina(int indice) {
        return libro.getPagina(indice).getRutaImagen();
    }

    public String getTextoPaginaActual() {
        return libro.getPaginaActual().getTexto();
    }

    public boolean siguientePagina() {
        return libro.siguiente();
    }

    public void irAUltimaPagina() {
        libro.irAUltima();
    }

    public void reiniciarLibro() {
        libro.reiniciar();
    }
    public int getFilasTablero() {
        return TableroMatriz.filas;
    }

    public int getColumnasTablero() {
        return TableroMatriz.columnas;
    }

    public String getNombreJugador(int numero) {
        return modoClasico.getJugador(numero).getNombre();
    }

    public int getIntentosJugador(int numero) {
        return modoClasico.getJugador(numero).getIntentos();
    }
    public String getTextoTiempoInicial() {
        return String.format("%02d:%02d", 0, ModoClasico.TIEMPO_JUGADA_SEG);
    }

    public String getTextoTurno() {
        return modoClasico.getJugador(modoClasico.getTurnoActual()).getNombre();
    }

    public String[][] getImagenesTableroClasico(int numeroJugador) {
        return modoClasico.getTablero(numeroJugador).getRutasImagenes();
    }
    public void mostrarAnimacionVictoria() {
        vista.mostrarAnimacionVictoria();
    }
    public int getNivelPatron() {
        return modoPatron.getNivel();
    }

    public int getLongitudPatron() {
        return modoPatron.getLongitudPatron();
    }

    public int getTiempoTotalPatron() {
        return ModoPatron.tiempo;
    }

    public int getTiempoEntreCasillas() {
        return ModoPatron.tiempoEntreCasillas;
    }

    //PREMIOS
    public List<Jugador> getTop5() {
        return gestorPremios.obtenerTop5();
    }

    public String getTextoTipoCambio() {
        return gestorPremios.getTextoTipoCambio();
    }
    private boolean estaSonando() {
        return hiloSonido != null && hiloSonido.isAlive();
    }

    public void alternarMusica() {
        if (estaSonando()) {
            detenerMusica();
        } else {
            iniciarMusica();
        }
        vista.actualizarBotonMusica(estaSonando());
    }
    public void iniciarMusica() {
        if (estaSonando()) {
            return;
        }
        File archivo = new File(Recursos.ARCHIVO_MUSICA);
        if (!archivo.exists()) {
            System.err.println("Música no encontrada (opcional por ahora): " + Recursos.ARCHIVO_MUSICA);
            return;
        }
        try {
            tareaSonido = new HiloSonido(archivo);
            hiloSonido = new Thread(tareaSonido, "HiloSonido");
            hiloSonido.setDaemon(true);
            hiloSonido.start();
        } catch (LinkageError e) {
            System.err.println("Falta la librería JLayer (lib/jlayer-1.0.1.jar): " + e);
        }
    }
    public void detenerMusica() {
        if (tareaSonido != null) {
            tareaSonido.detener();
        }
        tareaSonido = null;
        hiloSonido = null;
    }
}
