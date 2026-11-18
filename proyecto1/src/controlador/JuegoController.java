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
    private String nombreJugadorPatron = "";
    private VentanaPrincipal vista;
    private HiloSonido tareaSonido;
    private Thread hiloSonido;
    private final AtomicBoolean consultandoCambio = new AtomicBoolean(false);

    public JuegoController() {
        modoClasico = new ModoClasico(new Jugador("Jugador 1"), new Jugador("Jugador 2"));
        modoPatron = new ModoPatron();
        libro = new LibroHistoria();
        gestorPremios = new GestorPremios("datos/premios.txt", "datos/premios_patron.txt");
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
        modoPatron.cancelarPartida();
        modoClasico.cancelarPartida();
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
        String estado = camara.getEstado();
        if (ocupadoRostro.get() && "Cámara activa".equals(estado)) {
            return "Analizando tu rostro…";
        }
        return estado;
    }

    private synchronized AutenticadorFacial obtenerAutenticador() {
        if (autenticador == null) {
            try {
                autenticador = new AutenticadorFacial(camara);
                errorAutenticador = null;
            } catch (RuntimeException | LinkageError e) {
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
        modoClasico.getJugador(1).setNombre(nombreUsuario);
        modoClasico.getJugador(2).setNombre("Jugador 2");
        nombreJugadorPatron = "";
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
                    mensaje(a.getMotivoRechazo() + "\nMira de frente a la cámara con buena luz, "
                            + "o entra con tu correo y contraseña.");
                } else {
                    entrar(nombre);
                }
            } catch (IllegalStateException e) {
                mensaje(e.getMessage());
            } catch (RuntimeException e) {
                System.err.println("Error en el reconocimiento facial: " + e);
                mensaje("Ocurrió un error al reconocer el rostro. Inténtalo de nuevo "
                        + "o entra con tu correo y contraseña.");
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
                    mensaje("No se pudo tomar la foto\n" + a.getMotivoRechazo());
                }
            } catch (IllegalStateException e) {
                mensaje(e.getMessage());
            } catch (RuntimeException e) {
                System.err.println("Error al capturar el rostro: " + e);
                mensaje("Ocurrió un error al tomar la foto del rostro. Inténtalo de nuevo.");
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

    private void terminarPartidaClasico() {
        int ganador = modoClasico.getGanador();
        if (ganador == 1 || ganador == 2) {
            gestorPremios.registrarGanador(getNombreJugador(ganador), getIntentosJugador(ganador));
            vista.actualizarPremios();
        }
        mostrarAnimacionVictoria();
    }

    public void mostrarAnimacionVictoria() {
        int ganador = modoClasico.getGanador();
        String mensaje;
        if (ganador == 0) {
            mensaje = "¡Empate con " + getIntentosJugador(1) + " intentos!";
        } else {
            mensaje = "¡Ganó " + getNombreJugador(ganador) + " con " + getIntentosJugador(ganador) + " intentos!";
        }
        vista.mostrarAnimacionVictoria(mensaje);
    }

    public void setNombresJugadoresClasico(String nombre1, String nombre2) {
        modoClasico.getJugador(1).setNombre(limpiarNombre(nombre1, 1));
        modoClasico.getJugador(2).setNombre(limpiarNombre(nombre2, 2));
    }

    private String limpiarNombre(String nombre, int numero) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "Jugador " + numero;
        }
        return nombre.trim();
    }

    public void iniciarPartidaClasico() {
        modoClasico.iniciarPartida();
    }

    public void cancelarPartidaClasico() {
        modoClasico.cancelarPartida();
    }

    public boolean isPartidaClasicoEnCurso() {
        return modoClasico.isEnCurso();
    }

    public ModoClasico.ResultadoJugada seleccionarCasillaClasico(int numeroJugador, int fila, int columna) {
        ModoClasico.ResultadoJugada resultado = modoClasico.seleccionarCasilla(numeroJugador, fila, columna);
        if (resultado == ModoClasico.ResultadoJugada.FIN_PARTIDA) {
            terminarPartidaClasico();
        }
        return resultado;
    }

    public boolean resolverFalloClasico() {
        boolean termino = modoClasico.resolverFallo();
        if (termino && modoClasico.isJuegoFinalizado()) {
            terminarPartidaClasico();
        }
        return termino;
    }

    public boolean actualizarTiempoClasico(long milisegundos) {
        boolean seAcabo = modoClasico.avanzarTiempo(milisegundos);
        if (seAcabo && modoClasico.isJuegoFinalizado()) {
            terminarPartidaClasico();
        }
        return seAcabo;
    }

    public int getTerminoPrimeroClasico() {
        return modoClasico.getTerminoPrimero();
    }

    public int getIntentosPrimeroClasico() {
        return modoClasico.getIntentosPrimero();
    }

    public boolean[][] getCasillasDescubiertasClasico(int numeroJugador) {
        return modoClasico.getTablero(numeroJugador).getCasillasDescubiertas();
    }

    public int getTurnoClasico() {
        return modoClasico.getTurnoActual();
    }

    public String getTextoTiempoClasico() {
        long ms = modoClasico.getTiempoRestanteMs();
        int segundos = (int) Math.ceil(ms / 1000.0);
        return String.format("%02d:%02d", segundos / 60, segundos % 60);
    }

    public int getParejasJugador(int numero) {
        return modoClasico.contarParejas(numero);
    }

    public boolean jugadorTerminoTablero(int numero) {
        return modoClasico.todasLasParejasEncontradas(numero);
    }

    public boolean isPartidaClasicoFinalizada() {
        return modoClasico.isJuegoFinalizado();
    }

    public int getGanadorClasico() {
        return modoClasico.getGanador();
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

    public void setNombreJugadorPatron(String nombre) {
        nombreJugadorPatron = nombre == null ? "" : nombre.trim();
    }

    public String getNombreJugadorPatron() {
        if (nombreJugadorPatron.isEmpty()) {
            return getNombreUsuarioActual();
        }
        return nombreJugadorPatron;
    }

    public void iniciarPartidaPatron() {
        modoPatron.iniciarPartida();
    }

    public List<int[]> getSecuenciaPatron() {
        return modoPatron.getSecuenciaCorrecta();
    }

    public void comenzarTurnoPatron() {
        modoPatron.comenzarTurnoJugador();
    }

    public ModoPatron.ResultadoPaso seleccionarCasillaPatron(int fila, int columna) {
        ModoPatron.ResultadoPaso resultado = modoPatron.verificarPaso(fila, columna);
        if (resultado == ModoPatron.ResultadoPaso.INCORRECTO) {
            terminarPartidaPatron();
        }
        return resultado;
    }

    public boolean actualizarTiempoPatron(long milisegundos) {
        boolean termino = modoPatron.avanzarTiempo(milisegundos);
        if (termino) {
            terminarPartidaPatron();
        }
        return termino;
    }

    private void terminarPartidaPatron() {
        int superados = modoPatron.getNivelesSuperados();
        String nombre = getNombreJugadorPatron();
        if (superados > 0 && !nombre.isEmpty()) {
            gestorPremios.registrarGanadorPatron(nombre, superados);
            vista.actualizarPremios();
        }
    }

    public long getTiempoRestantePatronMs() {
        return modoPatron.getTiempoRestanteMs();
    }

    public long getTiempoEntreCasillasRestanteMs() {
        return modoPatron.getTiempoEntreCasillasRestanteMs();
    }

    public int getPasoActualPatron() {
        return modoPatron.getPasoActual();
    }

    public int[] getPasoEsperadoPatron() {
        return modoPatron.getPasoEsperado();
    }

    public boolean isPartidaPatronEnCurso() {
        ModoPatron.Fase fase = modoPatron.getFase();
        return fase == ModoPatron.Fase.MOSTRANDO_PATRON || fase == ModoPatron.Fase.TURNO_JUGADOR;
    }

    public String getMotivoFinPatron() {
        return modoPatron.getMotivoFin();
    }

    public int getNivelesSuperadosPatron() {
        return modoPatron.getNivelesSuperados();
    }

    public int getMejorNivelPatron() {
        return modoPatron.getMejorNivel();
    }

    public void cancelarPartidaPatron() {
        modoPatron.cancelarPartida();
    }

    public List<Jugador> getTop5() {
        return gestorPremios.obtenerTop5();
    }

    public List<Jugador> getTop5Patron() {
        return gestorPremios.obtenerTop5Patron();
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