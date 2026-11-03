package controlador;

import apoyo.CamaraWebCam;
import apoyo.Recursos;
import hilos.HiloSonido;
import modelo.AutenticadorFacial;
import modelo.GestorPremios;
import modelo.GestorUsuarios;
import modelo.Jugador;
import modelo.LibroHistoria;
import modelo.ModoClasico;
import modelo.ModoPatron;
import modelo.TableroMatriz;
import modelo.Usuario;
import vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class JuegoController {

    private final ModoClasico modoClasico;
    private final ModoPatron modoPatron;
    private final LibroHistoria libro;
    private final GestorPremios gestorPremios;
    private final GestorUsuarios gestorUsuarios;
    private final CamaraWebCam camara = new CamaraWebCam();
    private volatile AutenticadorFacial autenticador;
    private String errorAutenticador;
    private final AtomicBoolean ocupadoRostro = new AtomicBoolean(false);

    private Jugador jugadorActual;
    private VentanaPrincipal vista;
    private HiloSonido tareaSonido;
    private Thread hiloSonido;
    private final AtomicBoolean consultandoCambio = new AtomicBoolean(false);

    public JuegoController() {
        modoClasico = new ModoClasico(new Jugador("Jugador 1"), new Jugador("Jugador 2"));
        modoPatron = new ModoPatron();
        libro = new LibroHistoria();
        gestorPremios = new GestorPremios();
        gestorUsuarios = new GestorUsuarios("datos/usuarios.txt");
    }

    public void setVista(VentanaPrincipal vista) {
        this.vista = vista;
    }

    public void iniciar() {
        vista.mostrarPantalla(VentanaPrincipal.LOGIN);
        iniciarMusica();
        vista.actualizarBotonMusica(estaSonando());
        consultarTipoCambio();
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

    public boolean haySesion() {
        return jugadorActual != null;
    }

    public void usarCamara() {
        camara.usar();
    }

    public void soltarCamara() {
        camara.soltar();
    }

    public BufferedImage getFotogramaCamara() {
        return camara.getFotograma();
    }

    public String getEstadoCamara() {
        return camara.getEstado();
    }

    private synchronized AutenticadorFacial obtenerAutenticador() {
        if (autenticador == null) {
            try {
                autenticador = new AutenticadorFacial(camara);
                errorAutenticador = null;
            } catch (IllegalStateException | LinkageError e) {
                errorAutenticador = e instanceof LinkageError
                        ? "Falta la librería JavaCV (carpeta javacv-libs): " + e
                        : e.getMessage();
            }
        }
        return autenticador;
    }

    private void mensaje(String texto) {
        SwingUtilities.invokeLater(() -> vista.mostrarMensaje(texto));
    }

    private void entrar(String nombreUsuario) {
        jugadorActual = new Jugador(nombreUsuario);
        SwingUtilities.invokeLater(() -> {
            vista.limpiarFormularios();
            vista.mostrarPantalla(VentanaPrincipal.HISTORIA);
        });
    }

    public void iniciarSesionConCredenciales(String identificador, char[] clave) {
        Usuario usuario = gestorUsuarios.autenticar(identificador, clave);
        if (usuario == null) {
            vista.mostrarMensaje("Usuario o contraseña incorrectos.");
            return;
        }
        entrar(usuario.getUsuario());
    }

    public void iniciarSesionConRostro() {
        if (!ocupadoRostro.compareAndSet(false, true)) {
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                AutenticadorFacial a = obtenerAutenticador();
                if (a == null) {
                    mensaje(errorAutenticador);
                    return;
                }
                String nombre = a.reconocerRostro();
                if (nombre == null) {
                    mensaje("No se reconoció tu rostro. Mira de frente a la cámara con buena luz, "
                            + "o entra con tu correo y contraseña.");
                } else {
                    entrar(nombre);
                }
            } catch (IllegalStateException e) {
                mensaje(e.getMessage());
            } finally {
                ocupadoRostro.set(false);
            }
        }, "HiloReconocimiento");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void capturarMuestraRostro(Consumer<Integer> alTerminar) {
        if (!ocupadoRostro.compareAndSet(false, true)) {
            alTerminar.accept(totalMuestrasRostro());
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                AutenticadorFacial a = obtenerAutenticador();
                if (a == null) {
                    mensaje(errorAutenticador);
                } else if (a.capturarMuestra() < 0) {
                    mensaje("No se detectó un rostro. Mira de frente a la cámara, con buena luz y sin nada tapándote.");
                }
            } catch (IllegalStateException e) {
                mensaje(e.getMessage());
            } finally {
                ocupadoRostro.set(false);
                alTerminar.accept(totalMuestrasRostro());
            }
        }, "HiloCapturaRostro");
        hilo.setDaemon(true);
        hilo.start();
    }

    private int totalMuestrasRostro() {
        return autenticador == null ? 0 : autenticador.getTotalMuestras();
    }

    public void descartarMuestrasRostro() {
        if (autenticador != null) {
            autenticador.limpiarMuestras();
        }
    }

    public void registrarUsuario(String nombre, String apellido, String correo, String usuario,
                                 char[] clave, char[] confirmar) {
        String error = gestorUsuarios.registrar(nombre, apellido, correo, usuario, clave, confirmar);
        if (error != null) {
            vista.mostrarMensaje(error);
            return;
        }
        String aviso = "Cuenta creada. ¡Bienvenido!";
        if (autenticador != null && autenticador.getTotalMuestras() >= AutenticadorFacial.MUESTRAS_REQUERIDAS) {
            try {
                autenticador.guardarRostros(usuario.trim());
            } catch (java.io.IOException e) {
                aviso = "Cuenta creada, pero no se pudo guardar tu rostro: " + e.getMessage();
            }
        } else {
            aviso = "Cuenta creada sin rostro. Solo podrás entrar con tu contraseña.";
        }
        vista.mostrarMensaje(aviso);
        entrar(usuario.trim());
    }

    public void recuperarClave() {
        vista.mostrarMensaje("La recuperación de contraseña todavía no está disponible. "
                + "Si la olvidaste, entra con tu rostro o crea una cuenta nueva.");
    }

    public void detenerCamara() {
        descartarMuestrasRostro();
    }

    public void iniciarSesionDemo(String nombre) {
        String usuario = (nombre == null || nombre.trim().isEmpty()) ? "Invitado" : nombre.trim();
        entrar(usuario);
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

    public List<String> getRutasTransicion(int indicePagina) {
        return libro.getRutasTransicion(indicePagina);
    }
    public List<String> getRutasCierre() {
        return libro.getRutasCierre();
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
        return TableroMatriz.FILAS;
    }

    public int getColumnasTablero() {
        return TableroMatriz.COLUMNAS;
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
        return ModoPatron.TIEMPO_TOTAL_SEG;
    }

    public int getTiempoEntreCasillas() {
        return ModoPatron.TIEMPO_ENTRE_CASILLAS_SEG;
    }

    public List<Jugador> getTop5() {
        return gestorPremios.obtenerTop5();
    }

    public String getTextoTipoCambio() {
        return gestorPremios.getTextoTipoCambio();
    }

    public boolean hayTipoCambio() {
        return gestorPremios.hayTipoCambio();
    }

    public void consultarTipoCambio() {
        if (!consultandoCambio.compareAndSet(false, true)) {
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                gestorPremios.actualizarTipoCambio();
            } finally {
                consultandoCambio.set(false);
            }
            SwingUtilities.invokeLater(() -> vista.actualizarPremios());
        }, "HiloBCCR");
        hilo.setDaemon(true);
        hilo.start();
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