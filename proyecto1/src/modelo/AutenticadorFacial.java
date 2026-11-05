package modelo;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class AutenticadorFacial {

    public static final int MUESTRAS_REQUERIDAS = 3;          // pasos del registro
    private static final int ROSTROS_POR_CAPTURA = 5;         // fotogramas guardados en cada paso
    private static final int MINIMO_POR_CAPTURA = 3;
    private static final int LADO = 100;                      // los rostros se normalizan a 100x100
    private static final double UMBRAL = 75.0;                // menor distancia = más parecido; sube si rechaza de más
    private static final int VOTOS_NECESARIOS = 3;            // coincidencias seguidas para aceptar
    private static final String RUTA_CASCADA = "recursos/modelos/haarcascade_frontalface_default.xml";
    private static final File CARPETA_ROSTROS = new File("datos/rostros");

    private final ProveedorCamara camara;
    private final CascadeClassifier detector;
    private final Java2DFrameConverter aImagen = new Java2DFrameConverter();
    private final OpenCVFrameConverter.ToMat aMat = new OpenCVFrameConverter.ToMat();
    private final List<Mat> pendientes = new ArrayList<>();
    private final Map<Integer, String> etiquetaAUsuario = new HashMap<>();
    private LBPHFaceRecognizer reconocedor;
    private boolean modeloActualizado = false;
    private int pasos = 0;

    public AutenticadorFacial(ProveedorCamara camara) {
        this.camara = camara;
        File xml = new File(RUTA_CASCADA);
        if (!xml.exists()) {
            throw new IllegalStateException("Falta el archivo " + RUTA_CASCADA
                    + " (haarcascade_frontalface_default.xml de OpenCV).");
        }
        detector = new CascadeClassifier(xml.getAbsolutePath());
        if (detector.empty()) {
            throw new IllegalStateException("No se pudo cargar " + RUTA_CASCADA);
        }
    }

    public String getEstadoCamara() {
        return camara.getEstado();
    }

    /** Un paso del registro: guarda varios fotogramas con rostro. Devuelve el total de pasos, o -1 si no vio un rostro. */
    public int capturarMuestra() {
        synchronized (this) {
            if (pasos >= MUESTRAS_REQUERIDAS) {
                limpiarMuestras();
            }
        }
        List<Mat> rafaga = new ArrayList<>();
        long limite = System.currentTimeMillis() + 5000;
        boolean hayImagen = false;
        while (rafaga.size() < ROSTROS_POR_CAPTURA && System.currentTimeMillis() < limite) {
            BufferedImage img = camara.capturar();
            if (img != null) {
                hayImagen = true;
                Mat rostro = extraerRostro(img);
                if (rostro != null) {
                    rafaga.add(rostro);
                }
            }
            pausa(250);
        }
        if (!hayImagen) {
            throw new IllegalStateException("No se recibe imagen de la cámara. Revisa que esté conectada "
                    + "y que otra aplicación no la esté usando.");
        }
        if (rafaga.size() < MINIMO_POR_CAPTURA) {
            return -1;
        }
        synchronized (this) {
            pendientes.addAll(rafaga);
            pasos++;
            return pasos;
        }
    }

    public synchronized int getTotalMuestras() {
        return pasos;
    }

    public synchronized void limpiarMuestras() {
        pendientes.clear();
        pasos = 0;
    }

    /** Guarda en disco los rostros capturados para el usuario recién registrado. */
    public synchronized void guardarRostros(String usuario) throws IOException {
        File carpeta = new File(CARPETA_ROSTROS, usuario);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getPath());
        }
        int n = 1;
        for (Mat rostro : pendientes) {
            File destino = new File(carpeta, String.format("%02d.png", n++));
            if (!imwrite(destino.getAbsolutePath(), rostro)) {
                throw new IOException("No se pudo guardar " + destino.getPath());
            }
        }
        modeloActualizado = false;
        limpiarMuestras();
    }

    /** Devuelve el nombre de usuario reconocido, o null si no hubo coincidencia. */
    public String reconocerRostro() {
        prepararModelo();
        Map<String, Integer> votos = new HashMap<>();
        long limite = System.currentTimeMillis() + 6000;
        boolean hayImagen = false;
        while (System.currentTimeMillis() < limite) {
            BufferedImage img = camara.capturar();
            if (img != null) {
                hayImagen = true;
                Mat rostro = extraerRostro(img);
                String usuario = rostro == null ? null : predecir(rostro);
                if (usuario != null) {
                    int total = votos.merge(usuario, 1, Integer::sum);
                    if (total >= VOTOS_NECESARIOS) {
                        return usuario;
                    }
                }
            }
            pausa(200);
        }
        if (!hayImagen) {
            throw new IllegalStateException("No se recibe imagen de la cámara. Revisa que esté conectada "
                    + "y que otra aplicación no la esté usando.");
        }
        return null;
    }

    private synchronized Mat extraerRostro(BufferedImage img) {
        Mat color = aMat.convert(aImagen.convert(img));
        Mat gris = new Mat();
        cvtColor(color, gris, COLOR_BGR2GRAY);
        equalizeHist(gris, gris);

        RectVector rostros = new RectVector();
        detector.detectMultiScale(gris, rostros, 1.1, 5, 0, new Size(90, 90), new Size());
        if (rostros.size() == 0) {
            return null;
        }
        Rect mejor = rostros.get(0);
        for (long i = 1; i < rostros.size(); i++) {
            Rect r = rostros.get(i);
            if (r.width() * r.height() > mejor.width() * mejor.height()) {
                mejor = r;
            }
        }
        Mat recorte = new Mat(gris, mejor);
        Mat salida = new Mat();
        resize(recorte, salida, new Size(LADO, LADO));
        return salida;
    }

    private synchronized String predecir(Mat rostro) {
        IntPointer etiqueta = new IntPointer(1);
        DoublePointer distancia = new DoublePointer(1);
        reconocedor.predict(rostro, etiqueta, distancia);
        return distancia.get(0) <= UMBRAL ? etiquetaAUsuario.get(etiqueta.get(0)) : null;
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
                etiquetaAUsuario.put(id, carpeta.getName());
                for (File png : pngs) {
                    Mat m = imread(png.getAbsolutePath(), IMREAD_GRAYSCALE);
                    if (!m.empty()) {
                        imagenes.add(m);
                        ids.add(id);
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

    private static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}