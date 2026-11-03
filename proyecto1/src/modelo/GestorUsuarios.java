package modelo;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/** Registro y autenticación de usuarios. Las contraseñas se guardan con PBKDF2 y sal, nunca en claro. */
public class GestorUsuarios {

    private static final int ITERACIONES = 120_000;
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    private final Path archivo;
    private final List<Usuario> usuarios = new ArrayList<>();

    public GestorUsuarios(String ruta) {
        this.archivo = Paths.get(ruta);
        cargar();
    }

    public synchronized boolean estaVacio() {
        return usuarios.isEmpty();
    }

    public synchronized List<Usuario> getUsuarios() {
        return new ArrayList<>(usuarios);
    }

    public synchronized Usuario buscarPorUsuario(String nombreUsuario) {
        for (Usuario u : usuarios) {
            if (u.getUsuario().equalsIgnoreCase(nombreUsuario)) {
                return u;
            }
        }
        return null;
    }

    /** Devuelve null si el registro fue exitoso; si no, el mensaje de error para el usuario. */
    public synchronized String registrar(String nombre, String apellido, String correo, String usuario,
                                         char[] clave, char[] confirmar) {
        try {
            nombre = nombre == null ? "" : nombre.trim();
            apellido = apellido == null ? "" : apellido.trim();
            correo = correo == null ? "" : correo.trim();
            usuario = usuario == null ? "" : usuario.trim();

            if (nombre.isEmpty() || apellido.isEmpty()) {
                return "Escribe tu nombre y tu apellido.";
            }
            if (!CORREO.matcher(correo).matches()) {
                return "El correo electrónico no es válido.";
            }
            if (!USUARIO.matcher(usuario).matches()) {
                return "El nombre de usuario debe tener de 3 a 20 caracteres: letras, números o guion bajo.";
            }
            if (!claveValida(clave)) {
                return "La contraseña debe tener al menos 8 caracteres, con letras y números.";
            }
            if (!Arrays.equals(clave, confirmar)) {
                return "Las contraseñas no coinciden.";
            }
            for (Usuario u : usuarios) {
                if (u.getCorreo().equalsIgnoreCase(correo)) {
                    return "Ya existe una cuenta con ese correo.";
                }
                if (u.getUsuario().equalsIgnoreCase(usuario)) {
                    return "Ese nombre de usuario ya está en uso.";
                }
            }

            byte[] sal = new byte[16];
            new SecureRandom().nextBytes(sal);
            Usuario nuevo = new Usuario(nombre, apellido, correo, usuario,
                    Base64.getEncoder().encodeToString(sal),
                    Base64.getEncoder().encodeToString(derivar(clave, sal)));
            usuarios.add(nuevo);
            try {
                guardar();
            } catch (IOException e) {
                usuarios.remove(nuevo);
                return "No se pudo guardar la cuenta: " + e.getMessage();
            }
            return null;
        } catch (GeneralSecurityException e) {
            return "No se pudo proteger la contraseña: " + e.getMessage();
        } finally {
            Arrays.fill(clave, '\0');
            Arrays.fill(confirmar, '\0');
        }
    }

    /** Acepta correo o nombre de usuario. Devuelve el usuario o null si las credenciales no coinciden. */
    public synchronized Usuario autenticar(String identificador, char[] clave) {
        try {
            String id = identificador == null ? "" : identificador.trim();
            for (Usuario u : usuarios) {
                if (u.getCorreo().equalsIgnoreCase(id) || u.getUsuario().equalsIgnoreCase(id)) {
                    byte[] calculado = derivar(clave, Base64.getDecoder().decode(u.getSal()));
                    byte[] guardado = Base64.getDecoder().decode(u.getHash());
                    return MessageDigest.isEqual(calculado, guardado) ? u : null;
                }
            }
            return null;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    private static boolean claveValida(char[] clave) {
        if (clave.length < 8) {
            return false;
        }
        boolean letra = false;
        boolean numero = false;
        for (char c : clave) {
            letra |= Character.isLetter(c);
            numero |= Character.isDigit(c);
        }
        return letra && numero;
    }

    private static byte[] derivar(char[] clave, byte[] sal) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String b64(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String d64(String texto) {
        return new String(Base64.getDecoder().decode(texto), StandardCharsets.UTF_8);
    }

    private void guardar() throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Usuario u : usuarios) {
            lineas.add(String.join("\t", b64(u.getNombre()), b64(u.getApellido()), b64(u.getCorreo()),
                    b64(u.getUsuario()), u.getSal(), u.getHash()));
        }
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Files.write(archivo, lineas, StandardCharsets.UTF_8);
    }

    private void cargar() {
        if (!Files.exists(archivo)) {
            return;
        }
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] p = linea.split("\t", -1);
                if (p.length != 6) {
                    continue;
                }
                try {
                    usuarios.add(new Usuario(d64(p[0]), d64(p[1]), d64(p[2]), d64(p[3]), p[4], p[5]));
                } catch (IllegalArgumentException e) {
                    // línea dañada: se ignora
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
        }
    }
}