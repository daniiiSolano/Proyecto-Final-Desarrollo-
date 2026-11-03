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

import java.io.File;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;
import javax.swing.SwingWorker;

public class JuegoController {

    private final ModoClasico modoClasico;
    private final ModoPatron modoPatron;
    private final LibroHistoria libro;
    private final GestorPremios gestorPremios;
    private AutenticadorFacial autenticador;
    private CamaraWebCam camara;
    private volatile String errorFacial;
    private boolean ocupadoFacial = false;
    private final ExecutorService hiloCamara = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ControlCamara");
        t.setDaemon(true);
        return t;
    });
    private final GestorUsuarios gestorUsuarios;

    private Jugador jugadorActual;
    private Usuario usuarioActual;
    private VentanaPrincipal vista;
    private HiloSonido tareaSonido;
    private Thread hiloSonido;

    public JuegoController() {
        modoClasico = new ModoClasico(new Jugador("Jugador 1"), new Jugador("Jugador 2"));
        modoPatron = new ModoPatron();
        libro = new LibroHistoria();
        gestorPremios = new GestorPremios();
        gestorUsuarios = new GestorUsuarios("datos/usuarios.dat");
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
        boolean libre = VentanaPrincipal.LOGIN.equals(pantalla) || VentanaPrincipal.REGISTRO.equals(pantalla);
        if (!libre && !haySesion()) {
            vista.mostrarMensaje("Debes iniciar sesión o registrarte para poder jugar.");
            return;
        }
        vista.mostrarPantalla(pantalla);
    }

    public void volverAlMenu() {
        vista.mostrarPantalla(VentanaPrincipal.HISTORIA);
    }

    public void volverAlInicio() {
        cerrarSesion();
    }

    public String getEstadoCamara() {
        if (errorFacial != null) {
            return "Cámara no disponible";
        }
        CamaraWebCam c = camara;
        return c == null ? "Cámara apagada" : c.getEstado();
    }

    public BufferedImage getFotogramaCamara() {
        CamaraWebCam c = camara;
        return c == null ? null : c.getFotograma();
    }

    /** Las pantallas con vista de cámara piden/sueltan la webcam; se ordena en un solo hilo. */
    public void usarCamara() {
        hiloCamara.submit(() -> {
            if (facialDisponible()) {
                camara.usar();
            }
        });
    }

    public void soltarCamara() {
        hiloCamara.submit(() -> {
            CamaraWebCam c = camara;
            if (c != null) {
                c.soltar();
            }
        });
    }

    private synchronized boolean facialDisponible() {
        if (autenticador != null) {
            return true;
        }
        if (errorFacial != null) {
            return false;
        }
        try {
            camara = new CamaraWebCam();
            autenticador = new AutenticadorFacial(camara);
            return true;
        } catch (LinkageError e) {
            errorFacial = "Falta la librería JavaCV (o sus archivos nativos) en lib/: " + e.getMessage();
        } catch (RuntimeException e) {
            errorFacial = e.getMessage();
        }
        camara = null;
        autenticador = null;
        return false;
    }

    private static String mensajeDe(Exception e) {
        Throwable causa = e.getCause() != null ? e.getCause() : e;
        return causa.getMessage() != null ? causa.getMessage() : "Ocurrió un error con la cámara.";
    }

    public boolean haySesion() {
        return usuarioActual != null;
    }

    public String getNombreUsuarioActual() {
        return usuarioActual == null ? "" : usuarioActual.getUsuario();
    }

    public void iniciarSesionConCredenciales(String identificador, char[] clave) {
        if (identificador == null || identificador.trim().isEmpty() || clave.length == 0) {
            vista.mostrarMensaje("Escribe tu correo o usuario y tu contraseña.");
            Arrays.fill(clave, '\0');
            return;
        }
        Usuario u = gestorUsuarios.autenticar(identificador, clave);
        if (u == null) {
            vista.mostrarMensaje("Correo/usuario o contraseña incorrectos.");
            return;
        }
        abrirSesion(u);
    }

    public void iniciarSesionConRostro() {
        if (gestorUsuarios.estaVacio()) {
            vista.mostrarMensaje("Aún no hay cuentas registradas. Crea una cuenta primero.");
            return;
        }
        if (ocupadoFacial) {
            return;
        }
        ocupadoFacial = true;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                if (!facialDisponible()) {
                    throw new IllegalStateException(errorFacial);
                }
                return autenticador.reconocerRostro();
            }

            @Override
            protected void done() {
                ocupadoFacial = false;
                try {
                    String usuario = get();
                    Usuario u = usuario == null ? null : gestorUsuarios.buscarPorUsuario(usuario);
                    if (u == null) {
                        vista.mostrarMensaje("No se reconoció tu rostro. Colócate de frente y con buena luz, "
                                + "o entra con tu correo y contraseña.");
                    } else {
                        abrirSesion(u);
                    }
                } catch (Exception e) {
                    vista.mostrarMensaje(mensajeDe(e));
                }
            }
        }.execute();
    }

    public boolean registrarUsuario(String nombre, String apellido, String correo, String usuario,
                                    char[] clave, char[] confirmar) {
        int muestras = autenticador == null ? 0 : autenticador.getTotalMuestras();
        if (muestras > 0 && muestras < AutenticadorFacial.MUESTRAS_REQUERIDAS) {
            Arrays.fill(clave, '\0');
            Arrays.fill(confirmar, '\0');
            vista.mostrarMensaje("Faltan " + (AutenticadorFacial.MUESTRAS_REQUERIDAS - muestras)
                    + " captura(s) del rostro. Complétalas o descártalas.");
            return false;
        }
        String error = gestorUsuarios.registrar(nombre, apellido, correo, usuario, clave, confirmar);
        if (error != null) {
            vista.mostrarMensaje(error);
            return false;
        }
        if (muestras > 0) {
            try {
                autenticador.guardarRostros(usuario.trim());
            } catch (IOException e) {
                vista.mostrarMensaje("Tu cuenta se creó, pero no se pudo guardar el rostro: " + e.getMessage());
            }
        }
        vista.limpiarFormularios();
        vista.mostrarMensaje("¡Cuenta creada! Ahora inicia sesión para jugar.");
        vista.mostrarPantalla(VentanaPrincipal.LOGIN);
        return true;
    }

    /** Un paso del registro facial. Al terminar avisa (en el hilo de Swing) cuántos pasos lleva. */
    public void capturarMuestraRostro(IntConsumer alTerminar) {
        if (ocupadoFacial) {
            alTerminar.accept(autenticador == null ? 0 : autenticador.getTotalMuestras());
            return;
        }
        ocupadoFacial = true;
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                if (!facialDisponible()) {
                    throw new IllegalStateException(errorFacial);
                }
                return autenticador.capturarMuestra();
            }

            @Override
            protected void done() {
                ocupadoFacial = false;
                try {
                    int n = get();
                    if (n < 0) {
                        vista.mostrarMensaje("No se detectó tu rostro. Colócate de frente, con buena luz, "
                                + "y vuelve a intentarlo.");
                    }
                } catch (Exception e) {
                    vista.mostrarMensaje(mensajeDe(e));
                }
                alTerminar.accept(autenticador == null ? 0 : autenticador.getTotalMuestras());
            }
        }.execute();
    }

    public void descartarMuestrasRostro() {
        if (autenticador != null) {
            autenticador.limpiarMuestras();
        }
    }

    public void recuperarClave() {
        vista.mostrarMensaje("La recuperación de contraseña por correo aún no está disponible.");
    }

    public void cerrarSesion() {
        usuarioActual = null;
        jugadorActual = null;
        libro.reiniciar();
        vista.reiniciarHistoria();
        vista.limpiarFormularios();
        vista.actualizarSesion(false, "");
        vista.mostrarPantalla(VentanaPrincipal.LOGIN);
    }

    private void abrirSesion(Usuario u) {
        usuarioActual = u;
        jugadorActual = new Jugador(u.getUsuario());
        vista.limpiarFormularios();
        vista.actualizarSesion(true, u.getUsuario());
        vista.mostrarPantalla(VentanaPrincipal.HISTORIA);
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