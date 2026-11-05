package modelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Guarda a los ganadores del modo clásico y calcula el premio de los 5 mejores.
// Solo cuenta el modo clásico porque el puntaje es "la cantidad de intentos necesarios para ganar el juego".
// Cada jugador aparece una sola vez, con su mejor marca (la menor cantidad de intentos).
// Los datos se guardan en un archivo de texto (una línea por jugador: nombre;intentos).
public class GestorPremios {

    private static final String SEPARADOR = ";";

    private final Path archivo;
    private final List<Jugador> jugadores = new ArrayList<>();
    private volatile double tipoCambio = 0;
    private volatile String textoTipoCambio = "Tipo de cambio BCCR: consultando...";

    public GestorPremios(String ruta) {
        this.archivo = Paths.get(ruta);
        cargar();
    }

    // Fórmula del enunciado: premio = 1 / puntaje * 100 * CambioDolar (el resultado queda en colones)
    public static double calcularPremio(int puntaje, double cambioDolar) {
        if (puntaje <= 0 || cambioDolar <= 0) {
            return 0;
        }
        return 1.0 / puntaje * 100 * cambioDolar;
    }

    // Consulta el tipo de cambio actual en el API del Banco Central de Costa Rica
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

    // Anota al ganador de una partida del modo clásico.
    // Si el jugador ya estaba, solo se cambia su marca cuando la nueva es mejor (menos intentos)
    public synchronized void registrarGanador(String nombre, int intentos) {
        if (nombre == null || nombre.trim().isEmpty() || intentos <= 0) {
            return;
        }
        nombre = nombre.trim().replace(SEPARADOR, ","); // el ";" se usa para separar en el archivo
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

    // Ordena de menos a más intentos y calcula el premio de los 5 primeros con el tipo de cambio actual
    public synchronized List<Jugador> obtenerTop5() {
        List<Jugador> ordenados = new ArrayList<>(jugadores);
        ordenados.sort(Comparator.comparingInt(Jugador::getIntentos));
        List<Jugador> top = new ArrayList<>(ordenados.subList(0, Math.min(5, ordenados.size())));
        for (Jugador j : top) {
            j.setPremio(calcularPremio(j.getIntentos(), tipoCambio));
        }
        return top;
    }

    public String getTextoTipoCambio() {
        return textoTipoCambio;
    }

    // Lee el archivo de ganadores (si todavía no existe, la lista queda vacía)
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
                        // línea dañada: se ignora
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
        }
    }

    // Escribe todos los ganadores en el archivo
    private void guardar() {
        List<String> lineas = new ArrayList<>();
        for (Jugador j : jugadores) {
            lineas.add(j.getNombre() + SEPARADOR + j.getIntentos());
        }
        try {
            if (archivo.getParent() != null) {
                Files.createDirectories(archivo.getParent());
            }
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + archivo + ": " + e.getMessage());
        }
    }
}

