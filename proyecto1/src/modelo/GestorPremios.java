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

public class GestorPremios {

    private static final String SEPARADOR = ";";
    public static final double PREMIO_POR_NIVEL_USD = 10;

    private final Path archivo;
<<<<<<< Updated upstream
    private final Path archivoPatron;                       // ganadores del modo patrones
    private final List<Jugador> jugadoresPatron = new ArrayList<>(); // intentos = niveles superados
=======
    private final Path archivoPatron;
>>>>>>> Stashed changes
    private final List<Jugador> jugadores = new ArrayList<>();
    private final List<Jugador> jugadoresPatron = new ArrayList<>();
    private volatile double tipoCambio = 0;
    private volatile String textoTipoCambio = "Tipo de cambio BCCR: consultando...";

    public GestorPremios(String ruta) {
        this(ruta, null);
    }

    public GestorPremios(String ruta, String rutaPatron) {
        this.archivo = Paths.get(ruta);
<<<<<<< Updated upstream
        // el archivo del modo patrones queda en la misma carpeta que el del modo clásico
        Path carpeta = this.archivo.getParent();
        this.archivoPatron = (carpeta != null) ? carpeta.resolve("ganadores_patron.txt")
                : Paths.get("ganadores_patron.txt");
=======
        this.archivoPatron = rutaPatron == null ? null : Paths.get(rutaPatron);
>>>>>>> Stashed changes
        cargar();
        cargarPatron();
    }

    public static double calcularPremio(int puntaje, double cambioDolar) {
        if (puntaje <= 0 || cambioDolar <= 0) {
            return 0;
        }
        return 1.0 / puntaje * 100 * cambioDolar;
    }

<<<<<<< Updated upstream
    // Modo patrones: premio en dólares por cada nivel superado (más niveles = más premio)
    public static final double USD_POR_NIVEL = 1.0;

    public static double calcularPremioDolaresPatron(int niveles) {
        return niveles <= 0 ? 0 : niveles * USD_POR_NIVEL;
    }

    // Modo patrones: premio en colones = niveles * USD_POR_NIVEL * CambioDolar
=======
    public static double calcularPremioPatronDolares(int niveles) {
        if (niveles <= 0) {
            return 0;
        }
        return niveles * PREMIO_POR_NIVEL_USD;
    }

>>>>>>> Stashed changes
    public static double calcularPremioPatron(int niveles, double cambioDolar) {
        if (niveles <= 0 || cambioDolar <= 0) {
            return 0;
        }
<<<<<<< Updated upstream
        return calcularPremioDolaresPatron(niveles) * cambioDolar;
    }

    // Consulta el tipo de cambio actual en el API del Banco Central de Costa Rica
=======
        return calcularPremioPatronDolares(niveles) * cambioDolar;
    }

>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    // Anota el resultado de un jugador del modo clásico (se llama una vez por cada jugador al terminar la partida).
    // Solo se guarda si entra al top 5 (puestos 1 a 5; los empates comparten puesto).
    // Si el jugador ya estaba, solo se cambia su marca cuando la nueva es mejor (menos intentos)
=======
>>>>>>> Stashed changes
    public synchronized void registrarGanador(String nombre, int intentos) {
        if (registrarSiEntraAlTop(jugadores, nombre, intentos, true)) {
            guardar();
        }
<<<<<<< Updated upstream
=======
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
>>>>>>> Stashed changes
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
        ordenados.sort(Comparator.comparingInt(Jugador::getIntentos));
        List<Jugador> top = recortarTop(ordenados);
        for (Jugador j : top) {
            j.setPremio(calcularPremio(j.getIntentos(), tipoCambio));
        }
        return top;
    }

<<<<<<< Updated upstream
    // Anota el resultado de una partida del modo patrones.
    // El puntaje es la cantidad de niveles superados (se guarda en el campo "intentos" del Jugador).
    // Solo se guarda si entra al top 5 (los empates comparten puesto). Quien no superó ningún nivel no entra.
    // Si el jugador ya estaba, solo se cambia su marca cuando la nueva es mejor (más niveles).
    public synchronized void registrarGanadorPatron(String nombre, int niveles) {
        if (registrarSiEntraAlTop(jugadoresPatron, nombre, niveles, false)) {
            guardarPatron();
        }
    }

    // Ordena de más a menos niveles y calcula el premio de los 5 primeros (más niveles = más premio)
    public synchronized List<Jugador> obtenerTop5Patron() {
        List<Jugador> ordenados = new ArrayList<>(jugadoresPatron);
        ordenados.sort(Comparator.comparingInt(Jugador::getIntentos).reversed());
        List<Jugador> top = recortarTop(ordenados);
        for (Jugador j : top) {
            j.setPremio(calcularPremioPatron(j.getIntentos(), tipoCambio));
=======
    public synchronized List<Jugador> obtenerTop5Patron() {
        List<Jugador> ordenados = new ArrayList<>(jugadoresPatron);
        ordenados.sort(Comparator.comparingDouble(Jugador::getPuntos).reversed());
        List<Jugador> top = new ArrayList<>(ordenados.subList(0, Math.min(5, ordenados.size())));
        for (Jugador j : top) {
            j.setPremio(calcularPremioPatron((int) j.getPuntos(), tipoCambio));
>>>>>>> Stashed changes
        }
        return top;
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

<<<<<<< Updated upstream
    // ---------------------------------------------------------------- reglas del top 5

    private static final int PUESTOS = 5;

    private static boolean esMejor(int nuevo, int actual, boolean menorEsMejor) {
        return menorEsMejor ? nuevo < actual : nuevo > actual;
    }

    // Puesto que tiene un puntaje en la lista: 1 + cantidad de marcas estrictamente mejores.
    // Los empates comparten puesto (por ejemplo 1, 1, 3). "excluir" es el jugador que se está actualizando
    private static int puesto(List<Jugador> lista, int puntaje, boolean menorEsMejor, Jugador excluir) {
        int mejores = 0;
        for (Jugador j : lista) {
            if (j != excluir && esMejor(j.getIntentos(), puntaje, menorEsMejor)) {
                mejores++;
            }
        }
        return mejores + 1;
    }

    // Guarda el resultado solo si queda dentro de los puestos 1 a 5 (o empata con el 5.º).
    // Después quita a quienes quedaron fuera del top 5. Devuelve true si la lista cambió
    private static boolean registrarSiEntraAlTop(List<Jugador> lista, String nombre, int puntaje,
                                                 boolean menorEsMejor) {
        if (nombre == null || nombre.trim().isEmpty() || puntaje <= 0) {
            return false;
        }
        nombre = nombre.trim().replace(SEPARADOR, ","); // el ";" se usa para separar en el archivo
        Jugador existente = null;
        for (Jugador j : lista) {
            if (j.getNombre().equalsIgnoreCase(nombre)) {
                existente = j;
                break;
            }
        }
        if (existente != null && !esMejor(puntaje, existente.getIntentos(), menorEsMejor)) {
            return false; // no mejora su propia marca
        }
        if (puesto(lista, puntaje, menorEsMejor, existente) > PUESTOS) {
            return false; // no alcanza el top 5
        }
        if (existente != null) {
            existente.setIntentos(puntaje);
        } else {
            lista.add(new Jugador(nombre, puntaje, 0));
        }
        List<Jugador> fuera = new ArrayList<>();
        for (Jugador j : lista) {
            if (puesto(lista, j.getIntentos(), menorEsMejor, null) > PUESTOS) {
                fuera.add(j);
            }
        }
        lista.removeAll(fuera);
        return true;
    }

    // Recibe la lista ya ordenada del mejor al peor y deja los puestos 1 a 5 (con todos los empates).
    // Puede devolver más de 5 jugadores si hay empates en el último puesto
    private static List<Jugador> recortarTop(List<Jugador> ordenados) {
        List<Jugador> top = new ArrayList<>();
        int puesto = 0;
        for (int i = 0; i < ordenados.size(); i++) {
            Jugador j = ordenados.get(i);
            if (i == 0 || j.getIntentos() != ordenados.get(i - 1).getIntentos()) {
                puesto = i + 1;
            }
            if (puesto > PUESTOS) {
                break;
            }
            top.add(j);
        }
        return top;
    }

    // Lee el archivo de ganadores del modo patrones
    private void cargarPatron() {
        if (!Files.exists(archivoPatron)) {
=======
    private void cargarPatron() {
        if (archivoPatron == null || !Files.exists(archivoPatron)) {
>>>>>>> Stashed changes
            return;
        }
        try {
            for (String linea : Files.readAllLines(archivoPatron, StandardCharsets.UTF_8)) {
                String[] partes = linea.split(SEPARADOR);
                if (partes.length == 2) {
                    try {
<<<<<<< Updated upstream
                        jugadoresPatron.add(new Jugador(partes[0], Integer.parseInt(partes[1].trim()), 0));
                    } catch (NumberFormatException e) {
                        // línea dañada: se ignora
=======
                        int niveles = Integer.parseInt(partes[1].trim());
                        jugadoresPatron.add(new Jugador(partes[0], 0, niveles, 0, 0, 0));
                    } catch (NumberFormatException e) {
>>>>>>> Stashed changes
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivoPatron + ": " + e.getMessage());
        }
    }

<<<<<<< Updated upstream
    // Escribe los ganadores del modo patrones en su archivo
    private void guardarPatron() {
        List<String> lineas = new ArrayList<>();
        for (Jugador j : jugadoresPatron) {
            lineas.add(j.getNombre() + SEPARADOR + j.getIntentos());
        }
        try {
            if (archivoPatron.getParent() != null) {
                Files.createDirectories(archivoPatron.getParent());
            }
            Files.write(archivoPatron, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + archivoPatron + ": " + e.getMessage());
        }
    }

    // Escribe todos los ganadores en el archivo
=======
>>>>>>> Stashed changes
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