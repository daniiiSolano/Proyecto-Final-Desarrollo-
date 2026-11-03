package modelo;

import apoyo.Recursos;

public class Casillas {

// atributos

    public enum Estado { OCULTA, VISIBLE, EMPAREJADA }

    private final int idObjeto;
    private Estado estado = Estado.OCULTA;
    private String icon; //ubicacion


// metodos

    public Casillas(int idObjeto) {
        this.idObjeto = idObjeto;
        this.icon = Recursos.rutaObjeto(idObjeto);
    }

// get n set

    public int getIdObjeto() {
        return idObjeto;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

//

    // Comprueba si la casilla está visible
    public boolean isVisible() {
        return estado == Estado.VISIBLE;
    }

    // Comprueba si la casilla ya está emparejada
    public boolean isEmparejada() {
        return estado == Estado.EMPAREJADA;
    }

    public void revelar() {// Revela la casilla si todavía está oculta
        if (estado == Estado.OCULTA) {
            this.estado = Estado.VISIBLE;
        }
    }

    public void ocultar() { // Oculta la casilla cuando la pareja es incorrecta

        if (estado == Estado.VISIBLE) {
            this.estado = Estado.OCULTA;
        }
    }

    public void marcarEmparejada() { // Marca la casilla cuando se encuentra su pareja
        this.estado = Estado.EMPAREJADA;
    }

    public String getRutaImagen() { // Obtiene la ruta de la imagen correspondiente
        return Recursos.rutaObjeto(idObjeto);
    }
}
