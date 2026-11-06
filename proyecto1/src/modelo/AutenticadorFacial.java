package modelo;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.BORDER_REPLICATE;
import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_core.mean;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.getRotationMatrix2D;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.warpAffine;

/**
 * Registro e inicio de sesión con el rostro (LBPH de OpenCV).
 * <p>
 * Registro: se toma UNA sola fotografía frontal, y solo cuando el rostro está bien colocado
 * (uno solo, de frente, centrado, cerca, con luz suficiente y quieto unos instantes).
 * Como una sola foto da poca información, al entrenar el modelo se generan en memoria
 * pequeñas variaciones de esa foto (espejo, giros y acercamientos leves, desenfoque) para
 * que el reconocimiento tolere mejor los cambios de postura y de cámara. En disco se
 * guarda únicamente la fotografía original.
 */
public class AutenticadorFacial {

    public static final int MUESTRAS_REQUERIDAS = 1;          // una única fotografía frontal
    private static final int LADO = 100;                      // los rostros se normalizan a 100x100
    private static final double UMBRAL = 80.0;                // menor distancia = más parecido; sube si rechaza de más
    private static final int VOTOS_NECESARIOS = 3;            // coincidencias para aceptar
    private static final int FOTOGRAMAS_ESTABLES = 4;         // fotogramas seguidos con el rostro bien colocado
    private static final int MINIMO_FOTOS_SIN_VARIACIONES = 5; // con menos fotos se generan variaciones
    private static final long ESPERA_CAMARA_MS = 8000;        // tiempo máximo para que la cámara mande imagen
    private static final long TIEMPO_CAPTURA_MS = 10000;
    private static final long TIEMPO_RECONOCER_MS = 7000;
    private static final String RUTA_CASCADA = "recursos/modelos/haarcascade_frontalface_default.xml";
    private static final File CARPETA_ROSTROS = new File("datos/rostros");

    public static final String MENSAJE_SIN_IMAGEN = "No se recibe imagen de la cámara.\n"
            + "Puede que otra aplicación (Zoom, Teams, Meet, OBS...) la esté usando: ciérrala o apaga "
            + "su video y vuelve a intentarlo. También revisa que la cámara esté conectada y con permiso.";

    private final ProveedorCamara camara;
    private final CascadeClassifier detector;
    private final Java2DFrameConverter aImagen = new Java2DFrameConverter();
    private final OpenCVFrameConverter.ToMat aMat = new OpenCVFrameConverter.ToMat();
    private final List<Mat> pendientes = new ArrayList<>();
    private final Map<Integer, String> etiquetaAUsuario = new HashMap<>();
    private LBPHFaceRecognizer reconocedor;
    private boolean modeloActualizado = false;
    private int pasos = 0;
    private volatile String motivo = "";

    public AutenticadorFacial(ProveedorCamara camara) {
        this.camara = camara;
        File xml = new File(RUTA_CASCADA);
        if (!xml.exists()) {
            throw new IllegalStateException("Falta el archivo " + RUTA_CASCADA
                    + " (haarcascade_frontalface_default.xml de OpenCV).");
        }
        // Primero con la ruta relativa: OpenCV no abre rutas absolutas con acentos o "ñ" en Windows
        // (por ejemplo C:\Users\José\...), y la ruta relativa del proyecto siempre es ASCII.
        CascadeClassifier cargado = cargarCascada(RUTA_CASCADA);
        if (cargado == null) {
            cargado = cargarCascada(xml.getAbsolutePath());
        }
        if (cargado == null) {
            throw new IllegalStateException("No se pudo cargar " + RUTA_CASCADA);
        }
        detector = cargado;
    }

    private static CascadeClassifier cargarCascada(String ruta) {
        try {
            CascadeClassifier c = new CascadeClassifier(ruta);
            return c.empty() ? null : c;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public String getEstadoCamara() {
        return camara.getEstado();
    }

    /** Explicación de por qué falló la última captura o el último reconocimiento. */
    public String getMotivoRechazo() {
        return motivo;
    }

    // ================================================================== registro

    /**
     * Toma la fotografía frontal del registro. Espera a que el rostro esté bien colocado y,
     * de los últimos fotogramas estables, se queda con el más nítido.
     * Devuelve el total de fotos listas (1), o -1 si no se logró (ver {@link #getMotivoRechazo()}).
     */
    public int capturarMuestra() {
        limpiarMuestras(); // una nueva captura reemplaza a la anterior
        motivo = "";
        esperarPrimeraImagen();

        long limite = System.currentTimeMillis() + TIEMPO_CAPTURA_MS;
        Rect anterior = null;
        List<Mat> estables = new ArrayList<>();
        List<Double> nitidez = new ArrayList<>();
        boolean hayImagen = false;
        while (System.currentTimeMillis() < limite) {
            BufferedImage img = camara.capturar();
            if (img == null) {
                pausa(100);
                continue;
            }
            hayImagen = true;
            Deteccion d = detectar(img);
            String problema = d.problemaParaRegistro();
            if (problema != null) {
                motivo = problema;
                anterior = null;
                estables.clear();
                nitidez.clear();
                pausa(120);
                continue;
            }
            if (anterior != null && !parecidos(anterior, d.rostro)) {
                estables.clear(); // se movió: vuelve a contar
                nitidez.clear();
            }
            anterior = d.rostro;
            Mat recorte = d.recortar();
            estables.add(recorte);
            nitidez.add(calcularNitidez(recorte));
            if (estables.size() >= FOTOGRAMAS_ESTABLES) {
                int mejor = 0;
                for (int i = 1; i < nitidez.size(); i++) {
                    if (nitidez.get(i) > nitidez.get(mejor)) {
                        mejor = i;
                    }
                }
                synchronized (this) {
                    pendientes.clear();
                    pendientes.add(estables.get(mejor));
                    pasos = 1;
                    motivo = "";
                    return pasos;
                }
            }
            pausa(150);
        }
        if (!hayImagen) {
            throw new IllegalStateException(MENSAJE_SIN_IMAGEN);
        }
        if (motivo.isEmpty()) {
            motivo = "Quédate quieto unos segundos mirando de frente a la cámara.";
        }
        return -1;
    }

    public synchronized int getTotalMuestras() {
        return pasos;
    }

    public synchronized void limpiarMuestras() {
        pendientes.clear();
        pasos = 0;
    }

    /** Guarda en disco la fotografía frontal del usuario recién registrado. */
    public synchronized void guardarRostros(String usuario) throws IOException {
        if (pendientes.isEmpty()) {
            throw new IOException("No hay ninguna fotografía del rostro capturada.");
        }
        File carpeta = new File(CARPETA_ROSTROS, usuario);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getPath());
        }
        // Si quedaron fotos viejas de ese mismo nombre de usuario se borran para no mezclar rostros
        File[] viejas = carpeta.listFiles((dir, nombre) -> nombre.toLowerCase().endsWith(".png"));
        if (viejas != null) {
            for (File vieja : viejas) {
                if (!vieja.delete()) {
                    System.err.println("No se pudo borrar " + vieja.getPath());
                }
            }
        }
        // Se guarda con ImageIO (Java): imwrite de OpenCV falla si la ruta tiene acentos en Windows
        File destino = new File(carpeta, "01.png");
        if (!ImageIO.write(matAImagen(pendientes.get(0)), "png", destino)) {
            throw new IOException("No se pudo guardar " + destino.getPath());
        }
        modeloActualizado = false;
        limpiarMuestras();
    }

    // ================================================================== inicio de sesión

    /** Devuelve el nombre de usuario reconocido, o null si no hubo coincidencia (ver {@link #getMotivoRechazo()}). */
    public String reconocerRostro() {
        motivo = "";
        prepararModelo();
        esperarPrimeraImagen();

        Map<String, Integer> votos = new HashMap<>();
        long limite = System.currentTimeMillis() + TIEMPO_RECONOCER_MS;
        boolean hayImagen = false;
        boolean vioRostro = false;
        double mejorDistancia = Double.MAX_VALUE;
        while (System.currentTimeMillis() < limite) {
            BufferedImage img = camara.capturar();
            if (img != null) {
                hayImagen = true;
                Deteccion d = detectar(img);
                if (d.rostro != null && !d.oscura) {
                    vioRostro = true;
                    Prediccion p = predecir(d.recortar());
                    mejorDistancia = Math.min(mejorDistancia, p.distancia);
                    if (p.usuario != null) {
                        int total = votos.merge(p.usuario, 1, Integer::sum);
                        if (total >= VOTOS_NECESARIOS) {
                            System.out.printf("[Rostro] Reconocido %s (mejor distancia %.1f, umbral %.1f)%n",
                                    p.usuario, mejorDistancia, UMBRAL);
                            return p.usuario;
                        }
                    }
                } else if (d.oscura) {
                    motivo = "La imagen de la cámara está muy oscura. Busca más luz.";
                }
            }
            pausa(150);
        }
        if (!hayImagen) {
            throw new IllegalStateException(MENSAJE_SIN_IMAGEN);
        }
        if (vioRostro) {
            // Se imprime para poder ajustar UMBRAL si el reconocimiento rechaza de más
            System.out.printf("[Rostro] Sin coincidencia (mejor distancia %.1f, umbral %.1f)%n",
                    mejorDistancia, UMBRAL);
            motivo = "No se reconoció tu rostro.";
        } else if (motivo.isEmpty()) {
            motivo = "No se detectó ningún rostro frente a la cámara.";
        }
        return null;
    }

    // ================================================================== detección

    /** Resultado de buscar rostros en un fotograma. */
    private final class Deteccion {
        final Mat gris;          // imagen ecualizada completa
        final Rect rostro;       // el rostro más grande, o null
        final int cantidad;      // cuántos rostros se encontraron
        final int ancho;
        final int alto;
        final boolean oscura;    // la cámara manda una imagen negra (tapada o bloqueada)
        final double brillo;     // brillo medio del rostro antes de ecualizar (0-255)

        Deteccion(Mat gris, Rect rostro, int cantidad, int ancho, int alto, boolean oscura, double brillo) {
            this.gris = gris;
            this.rostro = rostro;
            this.cantidad = cantidad;
            this.ancho = ancho;
            this.alto = alto;
            this.oscura = oscura;
            this.brillo = brillo;
        }

        Mat recortar() {
            synchronized (AutenticadorFacial.this) {
                Mat recorte = new Mat(gris, rostro);
                Mat salida = new Mat();
                resize(recorte, salida, new Size(LADO, LADO));
                return salida;
            }
        }

        /** null si el rostro está bien colocado para la foto del registro; si no, qué corregir. */
        String problemaParaRegistro() {
            if (oscura) {
                return "La imagen de la cámara está negra o muy oscura. Destápala o busca más luz.";
            }
            if (rostro == null) {
                return "No se detectó un rostro. Mira de frente a la cámara, sin nada tapándote la cara.";
            }
            if (cantidad > 1) {
                return "Hay más de un rostro frente a la cámara. Debe aparecer solo una persona.";
            }
            if (rostro.width() < ancho * 0.20) {
                return "Tu rostro se ve muy pequeño. Acércate un poco a la cámara.";
            }
            double cx = rostro.x() + rostro.width() / 2.0;
            double cy = rostro.y() + rostro.height() / 2.0;
            if (cx < ancho * 0.30 || cx > ancho * 0.70 || cy < alto * 0.22 || cy > alto * 0.78) {
                return "Centra tu rostro en el recuadro de la cámara.";
            }
            if (brillo < 45) {
                return "Hay muy poca luz en tu rostro. Busca un lugar más iluminado.";
            }
            if (brillo > 225) {
                return "Hay demasiada luz sobre tu rostro. Evita la luz directa.";
            }
            return null;
        }
    }

    private synchronized Deteccion detectar(BufferedImage img) {
        Mat color = aMat.convert(aImagen.convert(img));
        Mat gris = new Mat();
        if (color.channels() == 1) {
            color.copyTo(gris);
        } else {
            cvtColor(color, gris, COLOR_BGR2GRAY);
        }
        boolean oscura = mean(gris).get(0) < 18; // cámara tapada o bloqueada por otra aplicación
        Mat sinEcualizar = gris.clone();
        equalizeHist(gris, gris);

        RectVector rostros = new RectVector();
        detector.detectMultiScale(gris, rostros, 1.1, 5, 0, new Size(90, 90), new Size());
        if (rostros.size() == 0) {
            return new Deteccion(gris, null, 0, img.getWidth(), img.getHeight(), oscura, 0);
        }
        Rect mejor = rostros.get(0);
        for (long i = 1; i < rostros.size(); i++) {
            Rect r = rostros.get(i);
            if (r.width() * r.height() > mejor.width() * mejor.height()) {
                mejor = r;
            }
        }
        double brillo = mean(new Mat(sinEcualizar, mejor)).get(0);
        return new Deteccion(gris, mejor, (int) rostros.size(), img.getWidth(), img.getHeight(), oscura, brillo);
    }

    /** Dos detecciones seguidas casi en el mismo lugar y del mismo tamaño (la persona está quieta). */
    private static boolean parecidos(Rect a, Rect b) {
        double dx = (a.x() + a.width() / 2.0) - (b.x() + b.width() / 2.0);
        double dy = (a.y() + a.height() / 2.0) - (b.y() + b.height() / 2.0);
        double proporcion = (double) b.width() / a.width();
        return Math.hypot(dx, dy) < a.width() * 0.12 && proporcion > 0.88 && proporcion < 1.12;
    }

    /** Nitidez aproximada (variación del laplaciano): mayor = foto menos movida. */
    private static double calcularNitidez(Mat rostro) {
        byte[] p = bytesDe(rostro);
        double suma = 0;
        double sumaCuadrados = 0;
        int n = 0;
        for (int y = 1; y < LADO - 1; y++) {
            for (int x = 1; x < LADO - 1; x++) {
                int c = p[y * LADO + x] & 0xFF;
                int lap = (p[(y - 1) * LADO + x] & 0xFF) + (p[(y + 1) * LADO + x] & 0xFF)
                        + (p[y * LADO + x - 1] & 0xFF) + (p[y * LADO + x + 1] & 0xFF) - 4 * c;
                suma += lap;
                sumaCuadrados += (double) lap * lap;
                n++;
            }
        }
        double media = suma / n;
        return sumaCuadrados / n - media * media;
    }

    // ================================================================== modelo

    private static final class Prediccion {
        final String usuario;
        final double distancia;

        Prediccion(String usuario, double distancia) {
            this.usuario = usuario;
            this.distancia = distancia;
        }
    }

    private synchronized Prediccion predecir(Mat rostro) {
        IntPointer etiqueta = new IntPointer(1);
        DoublePointer distancia = new DoublePointer(1);
        reconocedor.predict(rostro, etiqueta, distancia);
        double d = distancia.get(0);
        String usuario = d <= UMBRAL ? etiquetaAUsuario.get(etiqueta.get(0)) : null;
        return new Prediccion(usuario, d);
    }

    private synchronized void prepararModelo() {
        if (reconocedor != null && modeloActualizado) {
            return;
        }
        List<Mat> imagenes = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        etiquetaAUsuario.clear();
        File[] carpetas = CARPETA_ROSTROS.listFiles(File::isDirectory);
        int id = 0;
        if (carpetas != null) {
            for (File carpeta : carpetas) {
                File[] pngs = carpeta.listFiles((d, nombre) -> nombre.toLowerCase().endsWith(".png"));
                if (pngs == null || pngs.length == 0) {
                    continue;
                }
                List<Mat> fotos = new ArrayList<>();
                for (File png : pngs) {
                    Mat m = leerRostro(png);
                    if (m != null) {
                        fotos.add(m);
                    }
                }
                if (fotos.isEmpty()) {
                    continue;
                }
                // Con una sola foto frontal se agregan variaciones para que el modelo sea más tolerante
                boolean variar = fotos.size() < MINIMO_FOTOS_SIN_VARIACIONES;
                etiquetaAUsuario.put(id, carpeta.getName());
                for (Mat foto : fotos) {
                    imagenes.add(foto);
                    ids.add(id);
                    if (variar) {
                        for (Mat v : variaciones(foto)) {
                            imagenes.add(v);
                            ids.add(id);
                        }
                    }
                }
                id++;
            }
        }
        if (imagenes.isEmpty()) {
            throw new IllegalStateException("Ninguna cuenta tiene el rostro registrado todavía.");
        }
        MatVector vector = new MatVector(imagenes.size());
        Mat etiquetas = new Mat(imagenes.size(), 1, CV_32SC1);
        IntBuffer buffer = etiquetas.createBuffer();
        for (int i = 0; i < imagenes.size(); i++) {
            vector.put(i, imagenes.get(i));
            buffer.put(i, ids.get(i));
        }
        reconocedor = LBPHFaceRecognizer.create();
        reconocedor.train(vector, etiquetas);
        modeloActualizado = true;
    }

    /** Variaciones leves de una foto: espejo, giros de ±6°, acercamiento/alejamiento, desplazamiento y desenfoque. */
    private static List<Mat> variaciones(Mat foto) {
        List<Mat> lista = new ArrayList<>();
        Mat espejo = new Mat();
        flip(foto, espejo, 1);
        lista.add(espejo);
        lista.add(transformar(foto, 6, 1.0, 0, 0));
        lista.add(transformar(foto, -6, 1.0, 0, 0));
        lista.add(transformar(foto, 0, 1.07, 0, 0));
        lista.add(transformar(foto, 0, 0.93, 0, 0));
        lista.add(transformar(foto, 0, 1.0, 3, 2));
        lista.add(transformar(foto, 0, 1.0, -3, -2));
        Mat borrosa = new Mat();
        GaussianBlur(foto, borrosa, new Size(3, 3), 0);
        lista.add(borrosa);
        return lista;
    }

    private static Mat transformar(Mat foto, double angulo, double escala, double dx, double dy) {
        Mat matriz = getRotationMatrix2D(new Point2f(LADO / 2f, LADO / 2f), angulo, escala);
        if (dx != 0 || dy != 0) {
            DoubleIndexer idx = matriz.createIndexer();
            idx.put(0, 2, idx.get(0, 2) + dx);
            idx.put(1, 2, idx.get(1, 2) + dy);
            idx.release();
        }
        Mat salida = new Mat();
        warpAffine(foto, salida, matriz, new Size(LADO, LADO), INTER_LINEAR, BORDER_REPLICATE, new Scalar());
        return salida;
    }

    // ================================================================== lectura / escritura de fotos

    /** Lee una foto guardada (con ImageIO para soportar rutas con acentos; imread como respaldo). */
    private static Mat leerRostro(File png) {
        try {
            BufferedImage img = ImageIO.read(png);
            if (img != null) {
                return imagenAMat(img);
            }
        } catch (IOException e) {
            // se intenta con OpenCV
        }
        Mat m = imread(png.getAbsolutePath(), IMREAD_GRAYSCALE);
        if (m == null || m.empty()) {
            System.err.println("No se pudo leer la foto del rostro " + png.getPath());
            return null;
        }
        if (m.rows() != LADO || m.cols() != LADO) {
            Mat r = new Mat();
            resize(m, r, new Size(LADO, LADO));
            return r;
        }
        return m;
    }

    private static byte[] bytesDe(Mat m) {
        Mat continuo = m.isContinuous() ? m : m.clone();
        byte[] datos = new byte[LADO * LADO];
        continuo.data().get(datos);
        return datos;
    }

    /** Mat gris de 100x100 → BufferedImage gris (para guardarla con ImageIO). */
    private static BufferedImage matAImagen(Mat m) {
        BufferedImage img = new BufferedImage(LADO, LADO, BufferedImage.TYPE_BYTE_GRAY);
        byte[] destino = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        System.arraycopy(bytesDe(m), 0, destino, 0, destino.length);
        return img;
    }

    /** BufferedImage → Mat gris de 100x100 (misma conversión a gris que OpenCV). */
    private static Mat imagenAMat(BufferedImage img) {
        byte[] datos = new byte[LADO * LADO];
        if (img.getType() == BufferedImage.TYPE_BYTE_GRAY && img.getWidth() == LADO && img.getHeight() == LADO) {
            img.getRaster().getDataElements(0, 0, LADO, LADO, datos);
        } else {
            BufferedImage rgb = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, LADO, LADO, null);
            g.dispose();
            for (int y = 0; y < LADO; y++) {
                for (int x = 0; x < LADO; x++) {
                    int p = rgb.getRGB(x, y);
                    int r = (p >> 16) & 0xFF;
                    int gr = (p >> 8) & 0xFF;
                    int b = p & 0xFF;
                    datos[y * LADO + x] = (byte) Math.round(0.299 * r + 0.587 * gr + 0.114 * b);
                }
            }
        }
        Mat m = new Mat(LADO, LADO, CV_8UC1);
        m.data().put(datos);
        return m;
    }

    // ================================================================== utilidades

    /** Espera a que la cámara mande su primera imagen (abrirla puede tardar unos segundos). */
    private void esperarPrimeraImagen() {
        long limite = System.currentTimeMillis() + ESPERA_CAMARA_MS;
        while (camara.capturar() == null) {
            if (System.currentTimeMillis() >= limite || Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(MENSAJE_SIN_IMAGEN);
            }
            pausa(100);
        }
    }

    private static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
