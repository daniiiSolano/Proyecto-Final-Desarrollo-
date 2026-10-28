package Modelo;

public class Casillas {
//atributos
    private int fila;
    private int columna;
    private boolean visible;
    private boolean seleccionable;
    private boolean parejaLista;
    private String icon; //ubicacion

//metodos

    public Casillas(int fila, int columna, boolean visible, boolean seleccionable, boolean parejaLista) {
        this.fila = fila;
        this.columna = columna;
        this.visible = false; //al inicio del juego debe estar escondida
        this.seleccionable = true; // se puede seleccionar hasta q se empareja
        this.parejaLista = false; // falso hasta q se demuestre lo contrario
        this.icon = icon;
    }


//get n set
    public int getFila() {
        return fila;
    }

    public void setFila(int fila) {
        this.fila = fila;
    }

    public int getColumna() {
        return columna;
    }

    public void setColumna(int columna) {
        this.columna = columna;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setParejaLista(boolean parejaLista) {
        this.parejaLista = parejaLista;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

//logic
    public boolean isVisible() {
        return visible;
    }

    public boolean isParejaLista() {
        return parejaLista;
    }

    public boolean isSeleccionable() {
        return seleccionable;
    }

    public void revelar(){
    }

    public void ocultar(){
        if (!this.parejaLista){ // if pareja no esta lista
            this.visible = false; //se oculta cuando la pareja es incorrecta
        }
    }

    public void marcarEmparejada(){
        this.parejaLista = true;
        this.visible = true; //como la pareja ya esta si se puede ver
        this.seleccionable = false; // como ya hay pareja no se puede seleccionar
    }

    public void setSeleccionable(boolean seleccionable){
        this.seleccionable = seleccionable; // Se usa el seleccionable y no un true para poder decidir cuándo bloquear/desbloquear la casilla
    }

}
