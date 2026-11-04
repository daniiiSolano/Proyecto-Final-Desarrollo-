package modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GestorPremios {

    private final List<Jugador> jugadores = new ArrayList<>();
    private volatile double tipoCambio = 0;
    private volatile String textoTipoCambio = "Tipo de cambio BCCR: consultando...";

    public GestorPremios() {
        jugadores.add(new Jugador("DuendeVerde", 12, 0));
        jugadores.add(new Jugador("BosqueMágico", 15, 0));
        jugadores.add(new Jugador("Coleccionista", 18, 0));
        jugadores.add(new Jugador("HongoFeliz", 22, 0));
        jugadores.add(new Jugador("DuendeAzul", 27, 0));
    }

    public static double calcularPremio(int puntaje, double cambioDolar) {
        if (puntaje <= 0 || cambioDolar <= 0) {
            return 0;
        }
        return 1.0 / puntaje * 100 * cambioDolar;
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

    public List<Jugador> obtenerTop5() {
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
}
