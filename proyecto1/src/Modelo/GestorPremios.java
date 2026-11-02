package Modelo;

import java.util.ArrayList;
import java.util.List;
public class GestorPremios {

    private final List<Jugador> jugadores = new ArrayList<>();

    public GestorPremios() {
       }

    public List<Jugador> obtenerTop5() {
        return new ArrayList<>(jugadores.subList(0, Math.min(5, jugadores.size())));
    }
    public String getTextoTipoCambio() {
        return "Tipo de cambio BCCR: pendiente de consultar";
    }
}
