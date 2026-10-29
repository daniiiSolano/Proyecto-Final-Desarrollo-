package Modelo;

import java.util.ArrayList;
import java.util.List;
public class GestorPremios {

    private final List<Jugador> jugadores = new ArrayList<>();

    public GestorPremios() {
        jugadores.add(new Jugador("DuendeVerde", 12, 24.83));
        jugadores.add(new Jugador("BosqueMágico", 15, 19.86));
        jugadores.add(new Jugador("Coleccionista", 18, 16.55));
        jugadores.add(new Jugador("HongoFeliz", 22, 13.56));
        jugadores.add(new Jugador("DuendeAzul", 27, 11.04));
    }

    public List<Jugador> obtenerTop5() {
        return new ArrayList<>(jugadores.subList(0, Math.min(5, jugadores.size())));
    }
    public String getTextoTipoCambio() {
        return "Tipo de cambio BCR: pendiente de consultar";
    }
}
