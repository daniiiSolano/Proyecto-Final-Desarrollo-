package Modelo;

import apoyo.Recursos;

import java.util.ArrayList;
import java.util.List;

public class LibroHistoria {

    public static class Pagina {
        private final String rutaImagen;
        private final String texto;

        public Pagina(String rutaImagen, String texto) {
            this.rutaImagen = rutaImagen;
            this.texto = texto;
        }

        public String getRutaImagen() {
            return rutaImagen;
        }

        public String getTexto() {
            return texto;
        }
    }

    private static final String TEXTO_PRIMERA_PAGINA =
            "¡Bienvenido al almacén secreto! Los duendes no roban: coleccionan. En un árbol hueco "
                    + "funciona un almacén secreto donde cada duende registra el objeto más extraño que encontró "
                    + "en el mundo de los humanos. Pero el duende jefe mezcló todos los registros y ahora cada "
                    + "objeto tiene un gemelo equivocado. ¡Ayúdalos a reunir cada par y ordenar el inventario "
                    + "antes de que comience la inspección!";

    private final List<Pagina> paginas = new ArrayList<>();
    private int paginaActual = 0;

    public LibroHistoria() {
        int total = Recursos.contarSecuencia(Recursos.CARPETA_HISTORIA, Recursos.PREFIJO_LIBRO,
                Recursos.PAGINAS_LIBRO_MINIMAS);
        for (int i = 1; i <= total; i++) {
            String ruta = Recursos.rutaSecuencia(Recursos.CARPETA_HISTORIA, Recursos.PREFIJO_LIBRO, i);
            String texto = (i == 1) ? TEXTO_PRIMERA_PAGINA : "Página " + i + " de la historia (texto provisional).";
            paginas.add(new Pagina(ruta, texto));
        }
    }

    public int getTotalPaginas() {
        return paginas.size();
    }

    public int getIndicePaginaActual() {
        return paginaActual;
    }

    public Pagina getPagina(int indice) {
        return paginas.get(indice);
    }

    public Pagina getPaginaActual() {
        return paginas.get(paginaActual);
    }

    public boolean siguiente() {
        if (paginaActual < paginas.size() - 1) {
            paginaActual++;
            return true;
        }
        return false;
    }

    public void irAUltima() {
        paginaActual = paginas.size() - 1;
    }

    public void reiniciar() {
        paginaActual = 0;
    }
}
