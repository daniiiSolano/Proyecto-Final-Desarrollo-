package Modelo;

import apoyo.Recursos;
public class Casillas {

    public enum Estado { oculta, visible, emparejada }

    private final int idObjeto; // 1..18, corresponde a objeto_01.png ... objeto_18.png
    private Estado estado = Estado.oculta;

    public Casillas(int idObjeto) {
        this.idObjeto = idObjeto;
    }

    public int getIdObjeto() {
        return idObjeto;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
    public String getRutaImagen() {
        return Recursos.rutaObjeto(idObjeto);
    }
}
