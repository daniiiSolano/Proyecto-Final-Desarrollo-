package modelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestorPremios {

    private static final String SEPARADOR = ";";
    public static final double PREMIO_POR_NIVEL_USD = 10;

    private final Path archivo;
    private final Path archivoPatron;
    private final List<Jugador> jugadores = new ArrayList<>();
    private final List<Jugador> jugadoresPatron = new ArrayList<>();
    private volatile double tipoCambio = 0;
    private volatile String textoTipoCambio = "Tipo de cambio BCCR: consultando...";

    public GestorPremios(String ruta) {
        this(ruta, null);
    }

    public GestorPremios(String ruta, String rutaPatron) {
        this.archivo = Paths.get(ruta);
        this.archivoPatron = rutaPatron == null ? null : Paths.get(rutaPatron);
        cargar();
        cargarPatron();
    }

    public static double calcularPremio(int puntaje, double cambioDolar) {
        if (puntaje <= 0 || cambioDolar <= 0) {
            return 0;
        }
        return 1.0 / puntaje * 100 * cambioDolar;
    }

    public static double calcularPremioPatronDolares(int niveles) {
        if (niveles <= 0) {
            return 0;
        }
        return niveles * PREMIO_POR_NIVEL_USD;
    }

    public static double calcularPremioPatron(int niveles, double cambioDolar) {
        if (niveles <= 0 || cambioDolar <= 0) {
            return 0;
        }
        return calcularPremioPatronDolares(niveles) * cambioDolar;
    }

    public void actualizarTipoCambio() {
        try {
            tipoCambio = ServicioBCCR.obtenerTipoCambio(ServicioBCCR.leerToken());
            textoTipoCambio = String.format(Locale.US, "Tipo de cambio BCCR: %.2f colones por dólar", tipoCambio);
        } catch (Exception e) {
            tipoCambio = 0;
            textoTipoCambio = "Tipo de cambio BCCR: no disponible (" + e.getMessage() + ")";
            System.err.println("BCCR: " + e.getMessage());
        }
    }

    public boolean hayTipoCambio() {
        return tipoCambio > 0;
    }

    public synchronized void registrarGanador(String nombre, int intentos) {
        if (nombre == null || nombre.trim().isEmpty() || intentos <= 0) {
            return;
        }
        nombre = nombre.trim().replace(SEPARADOR, ",");
        for (Jugador j : jugadores) {
            if (j.getNombre().equalsIgnoreCase(nombre)) {
                if (intentos < j.getIntentos()) {
                    j.setIntentos(intentos);
                    guardar();
                }
                return;
            }
        }
        jugadores.add(new Jugador(nombre, intentos, 0));
        guardar();
    }

    public synchronized void registrarGanadorPatron(String nombre, int niveles) {
        if (archivoPatron == null || nombre == null || nombre.trim().isEmpty() || niveles <= 0) {
            return;
        }
        nombre = nombre.trim().replace(SEPARADOR, ",");
        for (Jugador j : jugadoresPatron) {
            if (j.getNombre().equalsIgnoreCase(nombre)) {
                if (niveles > (int) j.getPuntos()) {
                    j.setPuntos(niveles);
                    guardarPatron();
                }
                return;
            }
        }
        jugadoresPatron.add(new Jugador(nombre, 0, niveles, 0, 0, 0));
        guardarPatron();
    }

    public synchronized List<Jugador> obtenerTop5() {
        List<Jugador> ordenados = new ArrayList<>(jugadores);
        mergeSort(ordenados, 0, ordenados.size() - 1, false);
        List<Jugador> top = new ArrayList<>(ordenados.subList(0, Math.min(5, ordenados.size())));
        for (Jugador j : top) {
            j.setPremio(calcularPremio(j.getIntentos(), tipoCambio));
        }
        return top;
    }

    public synchronized List<Jugador> obtenerTop5Patron() {
        List<Jugador> ordenados = new ArrayList<>(jugadoresPatron);
        mergeSort(ordenados, 0, ordenados.size() - 1, true);
        List<Jugador> top = new ArrayList<>(ordenados.subList(0, Math.min(5, ordenados.size())));
        for (Jugador j : top) {
            j.setPremio(calcularPremioPatron((int) j.getPuntos(), tipoCambio));
        }
        return top;
    }

    private static void mergeSort(List<Jugador> lista, int izq, int der, boolean modoPatron) {
        if (izq >= der) {
            return;
        }
        int medio = (izq + der) / 2;
        mergeSort(lista, izq, medio, modoPatron);
        mergeSort(lista, medio + 1, der, modoPatron);
        merge(lista, izq, medio, der, modoPatron);
    }

    private static void merge(List<Jugador> lista, int izq, int medio, int der, boolean modoPatron) {
        List<Jugador> izquierda = new ArrayList<>(lista.subList(izq, medio + 1));
        List<Jugador> derecha = new ArrayList<>(lista.subList(medio + 1, der + 1));

        int i = 0, j = 0, k = izq;
        while (i < izquierda.size() && j < derecha.size()) {
            if (vaPrimero(izquierda.get(i), derecha.get(j), modoPatron)) {
                lista.set(k++, izquierda.get(i++));
            } else {
                lista.set(k++, derecha.get(j++));
            }
        }
        while (i < izquierda.size()) {
            lista.set(k++, izquierda.get(i++));
        }
        while (j < derecha.size()) {
            lista.set(k++, derecha.get(j++));
        }
    }

    private static boolean vaPrimero(Jugador a, Jugador b, boolean modoPatron) {
        if (modoPatron) {
            return a.getPuntos() >= b.getPuntos();
        }
        return a.getIntentos() <= b.getIntentos();
    }

    public String getTextoTipoCambio() {
        return textoTipoCambio;
    }

    private void cargar() {
        if (!Files.exists(archivo)) {
            return;
        }
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] partes = linea.split(SEPARADOR);
                if (partes.length == 2) {
                    try {
                        jugadores.add(new Jugador(partes[0], Integer.parseInt(partes[1].trim()), 0));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
        }
    }

    private void cargarPatron() {
        if (archivoPatron == null || !Files.exists(archivoPatron)) {
            return;
        }
        try {
            for (String linea : Files.readAllLines(archivoPatron, StandardCharsets.UTF_8)) {
                String[] partes = linea.split(SEPARADOR);
                if (partes.length == 2) {
                    try {
                        int niveles = Integer.parseInt(partes[1].trim());
                        jugadoresPatron.add(new Jugador(partes[0], 0, niveles, 0, 0, 0));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivoPatron + ": " + e.getMessage());
        }
    }

    private void guardar() {
        List<String> lineas = new ArrayList<>();
        for (Jugador j : jugadores) {
            lineas.add(j.getNombre() + SEPARADOR + j.getIntentos());
        }
        escribirArchivo(archivo, lineas);
    }

    private void guardarPatron() {
        List<String> lineas = new ArrayList<>();
        for (Jugador j : jugadoresPatron) {
            lineas.add(j.getNombre() + SEPARADOR + (int) j.getPuntos());
        }
        escribirArchivo(archivoPatron, lineas);
    }

    private void escribirArchivo(Path ruta, List<String> lineas) {
        try {
            if (ruta.getParent() != null) {
                Files.createDirectories(ruta.getParent());
            }
            Files.write(ruta, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + ruta + ": " + e.getMessage());
        }
    }
}