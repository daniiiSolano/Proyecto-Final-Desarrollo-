package modelo;

public class Usuario {

    private final String nombre;
    private final String apellido;
    private final String correo;
    private final String usuario;
    private final String sal;
    private final String hash;

    public Usuario(String nombre, String apellido, String correo, String usuario, String sal, String hash) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.usuario = usuario;
        this.sal = sal;
        this.hash = hash;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getSal() {
        return sal;
    }

    public String getHash() {
        return hash;
    }
}