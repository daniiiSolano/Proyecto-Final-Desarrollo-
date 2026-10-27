import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Collections;

public class VentanaLoguin extends JFrame {

    public static final String PESTANA_ROSTRO = "ROSTRO";
    public static final String PESTANA_USUARIO = "USUARIO";
    public static final String PESTANA_REGISTRO = "REGISTRO";

    private static final Color MADERA        = new Color(0x6F4527);
    private static final Color PAPEL         = new Color(0xFBF1DB);
    private static final Color PAPEL_OSC     = new Color(0xEBD7B0);
    private static final Color MARCO         = new Color(0xB97F4E);
    private static final Color TERRACOTA     = new Color(0xC96B4E);
    private static final Color TERRACOTA_OSC = new Color(0x9C4A33);
    private static final Color VERDE         = new Color(0x4E9A6A);
    private static final Color CREMA         = new Color(0xFFFBEF);
    private static final Color TINTA         = new Color(0x6B4E33);
    private static final Color TEXTO_CLARO   = new Color(0xFFF4E0);
    private static final Color PUNTADA       = new Color(0xC9A06E);
    private static final Color ROJO_PORTADA  = new Color(0x8E2F2A);
    private static final Color ROJO_BORDE    = new Color(0x5E1B19);
    private static final Color DORADO        = new Color(0xC9993E);

    private static final String FUENTE = "Comic Sans MS";

    private PanelConFondo pnlFondo;
    private PanelLibro pnlLibro;
    private JLabel lblLogo;
    private JLabel lblTitulo;

    private Pildora pldRostro;
    private Pildora pldUsuario;
    private Pildora pldRegistro;
    private JPanel pnlTarjetas;

    private PanelCamara pnlCamara;
    private JTextField txtNombreRostro;
    private JButton btnLoginRostro;
    private JButton btnRegistrarRostro;
    private JLabel lblEstado;

    private JTextField txtUsuario;
    private JPasswordField txtClave;
    private JButton btnEntrar;
    private JButton btnOlvido;
    private JButton btnIrRegistro;

    private JTextField txtNombre;
    private JPasswordField txtClaveNueva;
    private JPasswordField txtClaveConfirmar;
    private JButton btnCrearCuenta;
    private JButton btnVolver;

    public VentanaLoguin() {
        initComponents();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaLoguin().setVisible(true));
    }

    private void initComponents() {
        setTitle("Los duendes coleccionistas - Iniciar sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pnlFondo = new PanelConFondo("recursos/fondo.jpg");
        pnlFondo.setLayout(new GridBagLayout());

        pnlLibro = new PanelLibro();
        pnlLibro.setLayout(new BoxLayout(pnlLibro, BoxLayout.Y_AXIS));
        pnlLibro.setBorder(new EmptyBorder(30, 44, 48, 30));

        lblLogo = new JLabel(cargarLogo("recursos/logo.png", 64));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        PanelCinta pnlCinta = new PanelCinta();
        pnlCinta.setLayout(new BorderLayout());
        pnlCinta.setBorder(new EmptyBorder(8, 16, 12, 16));
        pnlCinta.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCinta.setPreferredSize(new Dimension(340, 50));
        pnlCinta.setMaximumSize(new Dimension(340, 50));
        lblTitulo = crearEtiqueta("Los duendes coleccionistas", Font.BOLD, 18, TEXTO_CLARO);
        pnlCinta.add(lblTitulo, BorderLayout.CENTER);

        PanelPildoras pnlPildoras = new PanelPildoras();
        pnlPildoras.setLayout(new GridLayout(1, 3));
        pnlPildoras.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlPildoras.setPreferredSize(new Dimension(340, 34));
        pnlPildoras.setMaximumSize(new Dimension(340, 34));
        pldRostro = new Pildora("Rostro");
        pldUsuario = new Pildora("Usuario");
        pldRegistro = new Pildora("Registro");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(pldRostro);
        grupo.add(pldUsuario);
        grupo.add(pldRegistro);
        pnlPildoras.add(pldRostro);
        pnlPildoras.add(pldUsuario);
        pnlPildoras.add(pldRegistro);
        pldRostro.setSelected(true);

        pnlTarjetas = new JPanel(new CardLayout());
        pnlTarjetas.setOpaque(false);
        pnlTarjetas.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlTarjetas.add(crearTarjetaRostro(), PESTANA_ROSTRO);
        pnlTarjetas.add(crearTarjetaUsuario(), PESTANA_USUARIO);
        pnlTarjetas.add(crearTarjetaRegistro(), PESTANA_REGISTRO);

        pnlLibro.add(lblLogo);
        pnlLibro.add(Box.createVerticalStrut(6));
        pnlLibro.add(pnlCinta);
        pnlLibro.add(Box.createVerticalStrut(12));
        pnlLibro.add(pnlPildoras);
        pnlLibro.add(Box.createVerticalStrut(14));
        pnlLibro.add(pnlTarjetas);

        pnlFondo.add(pnlLibro);
        add(pnlFondo);

        pldRostro.addActionListener(evt -> pldPestanaActionPerformed(PESTANA_ROSTRO));
        pldUsuario.addActionListener(evt -> pldPestanaActionPerformed(PESTANA_USUARIO));
        pldRegistro.addActionListener(evt -> pldPestanaActionPerformed(PESTANA_REGISTRO));

        btnIrRegistro.addActionListener(evt -> pldPestanaActionPerformed(PESTANA_REGISTRO));
        btnVolver.addActionListener(evt -> pldPestanaActionPerformed(PESTANA_USUARIO));

        setSize(520, 830);
        setMinimumSize(new Dimension(500, 800));
        setLocationRelativeTo(null);
    }

    private JPanel crearTarjetaRostro() {
        JPanel tarjeta = nuevaTarjeta();

        pnlCamara = new PanelCamara();
        pnlCamara.setPreferredSize(new Dimension(150, 150));
        pnlCamara.setMaximumSize(new Dimension(150, 150));
        pnlCamara.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        Duende duendeRojo = new Duende(new Color(0xE8695F), 58, 68);
        Duende duendeVerde = new Duende(new Color(0x3F9A63), 58, 68);

        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setAlignmentX(Component.CENTER_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Short.MAX_VALUE, 150));
        fila.add(Box.createHorizontalGlue());
        fila.add(duendeRojo);
        fila.add(Box.createHorizontalStrut(4));
        fila.add(pnlCamara);
        fila.add(Box.createHorizontalStrut(4));
        fila.add(duendeVerde);
        fila.add(Box.createHorizontalGlue());

        txtNombreRostro = new JTextField();
        btnLoginRostro = new BotonCuento("Entrar con mi rostro", VERDE, new Color(0xCDE8C4));
        btnRegistrarRostro = new BotonCuento("Registrar mi rostro", CREMA, new Color(0xD9A98C));
        btnRegistrarRostro.setForeground(new Color(0xA0502F));
        lblEstado = crearEtiqueta("Mira a la cámara con una sonrisa", Font.PLAIN, 12, TINTA);

        tarjeta.add(fila);
        tarjeta.add(Box.createVerticalStrut(12));
        tarjeta.add(btnLoginRostro);
        tarjeta.add(Box.createVerticalStrut(12));
        tarjeta.add(crearCampo("Tu nombre (para registrarte)", txtNombreRostro));
        tarjeta.add(Box.createVerticalStrut(6));
        tarjeta.add(btnRegistrarRostro);
        tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(lblEstado);
        return tarjeta;
    }

    private JPanel crearTarjetaUsuario() {
        JPanel tarjeta = nuevaTarjeta();

        Duende duende = new Duende(new Color(0x9B84E0), 70, 82);
        duende.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUsuario = new JTextField();
        txtClave = new JPasswordField();
        btnEntrar = new BotonCuento("Entrar", VERDE, new Color(0xCDE8C4));
        btnOlvido = new BotonEnlace("¿Olvidó su contraseña?");
        btnIrRegistro = new BotonEnlace("¿Primera vez? Crear cuenta");

        tarjeta.add(duende);
        tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(crearCampo("Usuario", txtUsuario));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(crearCampo("Contraseña", txtClave));
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(btnEntrar);
        tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(btnOlvido);
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(btnIrRegistro);
        return tarjeta;
    }

    private JPanel crearTarjetaRegistro() {
        JPanel tarjeta = nuevaTarjeta();

        Duende duendeA = new Duende(new Color(0xE8695F), 58, 68);
        Duende duendeB = new Duende(new Color(0x3F9A63), 58, 68);
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setAlignmentX(Component.CENTER_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Short.MAX_VALUE, 68));
        fila.add(Box.createHorizontalGlue());
        fila.add(duendeA);
        fila.add(Box.createHorizontalStrut(10));
        JLabel lblBienvenida = crearEtiqueta("¡Únete a la colección!", Font.BOLD, 14, TERRACOTA_OSC);
        lblBienvenida.setMaximumSize(lblBienvenida.getPreferredSize());
        fila.add(lblBienvenida);
        fila.add(Box.createHorizontalStrut(10));
        fila.add(duendeB);
        fila.add(Box.createHorizontalGlue());

        txtNombre = new JTextField();
        txtClaveNueva = new JPasswordField();
        txtClaveConfirmar = new JPasswordField();
        btnCrearCuenta = new BotonCuento("Crear cuenta", TERRACOTA, new Color(0xF6D2C6));
        btnVolver = new BotonEnlace("Volver a entrar");

        tarjeta.add(fila);
        tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(crearCampo("Nombre", txtNombre));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(crearCampo("Contraseña", txtClaveNueva));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(crearCampo("Confirmar contraseña", txtClaveConfirmar));
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(btnCrearCuenta);
        tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(btnVolver);
        return tarjeta;
    }

    private void pldPestanaActionPerformed(String pestana) {
        mostrarPestana(pestana);
    }

    public void mostrarPestana(String pestana) {
        ((CardLayout) pnlTarjetas.getLayout()).show(pnlTarjetas, pestana);
        pldRostro.setSelected(PESTANA_ROSTRO.equals(pestana));
        pldUsuario.setSelected(PESTANA_USUARIO.equals(pestana));
        pldRegistro.setSelected(PESTANA_REGISTRO.equals(pestana));
    }

    public void mostrarFrame(BufferedImage frame) {
        pnlCamara.setFrame(frame);
    }

    public void mostrarEstado(String mensaje) {
        lblEstado.setText(mensaje);
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error de autenticación",
                JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarInfo(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Los duendes coleccionistas",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void setOcupado(boolean ocupado) {
        btnLoginRostro.setEnabled(!ocupado);
        btnRegistrarRostro.setEnabled(!ocupado);
        btnEntrar.setEnabled(!ocupado);
        btnCrearCuenta.setEnabled(!ocupado);
    }

    public void habilitarRegistroRostro(boolean habilitado) {
        btnRegistrarRostro.setEnabled(habilitado);
        txtNombreRostro.setEnabled(habilitado);
    }

    public void limpiarCampos() {
        txtNombreRostro.setText("");
        txtUsuario.setText("");
        txtClave.setText("");
        txtNombre.setText("");
        txtClaveNueva.setText("");
        txtClaveConfirmar.setText("");
    }

    private static JPanel nuevaTarjeta() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JLabel crearEtiqueta(String texto, int estilo, int tamano, Color color) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setFont(new Font(FUENTE, estilo, tamano));
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static JPanel crearCampo(String etiqueta, JTextField campo) {
        JLabel l = new JLabel(etiqueta);
        l.setFont(new Font(FUENTE, Font.PLAIN, 11));
        l.setForeground(TINTA);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 4, 3, 0));

        campo.setFont(new Font(FUENTE, Font.PLAIN, 13));
        campo.setBackground(CREMA);
        campo.setForeground(TINTA);
        campo.setCaretColor(TINTA);
        campo.setBorder(new BordePuntadas(PUNTADA));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setPreferredSize(new Dimension(240, 32));
        campo.setMaximumSize(new Dimension(240, 32));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setPreferredSize(new Dimension(240, 54));
        p.setMaximumSize(new Dimension(240, 54));
        p.add(l);
        p.add(campo);
        return p;
    }

    private static ImageIcon cargarLogo(String ruta, int alto) {
        ImageIcon original = new ImageIcon(ruta);
        if (original.getIconWidth() <= 0) {
            return original;
        }
        int ancho = original.getIconWidth() * alto / original.getIconHeight();
        return new ImageIcon(original.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
    }

    private static void suavizar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    static class PanelConFondo extends JPanel {
        private final Image fondo;

        public PanelConFondo(String rutaImagen) {
            ImageIcon icono = new ImageIcon(rutaImagen);
            fondo = icono.getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        }
    }

    static class PanelLibro extends JPanel {
        PanelLibro() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fill(new RoundRectangle2D.Float(8, 10, w - 10, h - 10, 28, 28));
            g2.setColor(PAPEL_OSC);
            g2.fill(new RoundRectangle2D.Float(6, 8, w - 12, h - 10, 26, 26));
            g2.setPaint(new GradientPaint(0, 0, ROJO_PORTADA, w, h, ROJO_BORDE));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 12, h - 12, 26, 26));
            g2.setColor(ROJO_BORDE);
            g2.fillRoundRect(0, 0, 26, h - 12, 26, 26);

            g2.setColor(MARCO);
            g2.fill(new RoundRectangle2D.Float(34, 14, w - 60, h - 40, 22, 22));
            g2.setColor(PAPEL);
            g2.fill(new RoundRectangle2D.Float(37, 17, w - 66, h - 46, 19, 19));
            g2.setColor(DORADO);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{2f, 6f}, 0f));
            g2.draw(new RoundRectangle2D.Float(43, 23, w - 78, h - 58, 14, 14));
            g2.dispose();
        }
    }

    static class PanelCinta extends JPanel {
        PanelCinta() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(TERRACOTA_OSC);
            g2.fill(new RoundRectangle2D.Float(0, 4, w, h - 4, 10, 10));
            g2.setColor(TERRACOTA);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h - 6, 10, 10));
            g2.setColor(new Color(0xF6D2C6));
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 4f}, 0f));
            g2.draw(new RoundRectangle2D.Float(5, 4, w - 10, h - 15, 6, 6));
            g2.dispose();
        }
    }

    static class PanelPildoras extends JPanel {
        PanelPildoras() {
            setOpaque(false);
            setBorder(new EmptyBorder(3, 3, 3, 3));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setColor(PAPEL_OSC);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
    }

    static class Pildora extends JToggleButton {
        Pildora(String texto) {
            super(texto);
            setFont(new Font(FUENTE, Font.BOLD, 11));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            if (isSelected()) {
                g2.setColor(VERDE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            }
            setForeground(isSelected() ? TEXTO_CLARO : TINTA);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class BordePuntadas implements Border {
        private final Color color;

        BordePuntadas(Color color) {
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 3f}, 0f));
            g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 12, 12);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(5, 12, 5, 12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    static class BotonCuento extends JButton {
        private final Color base;
        private final Color puntada;

        BotonCuento(String texto, Color base, Color puntada) {
            super(texto);
            this.base = base;
            this.puntada = puntada;
            setFont(new Font(FUENTE, Font.BOLD, 13));
            setForeground(TEXTO_CLARO);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(260, 36));
            setMaximumSize(new Dimension(260, 36));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            Color c = !isEnabled() ? new Color(0xB9B2A3)
                    : getModel().isPressed() ? base.darker()
                      : getModel().isRollover() ? base.brighter() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setColor(puntada);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 3f}, 0f));
            g2.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class BotonEnlace extends JButton {
        BotonEnlace(String texto) {
            super(texto);
            Font base = new Font(FUENTE, Font.PLAIN, 12);
            setFont(base.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE,
                    TextAttribute.UNDERLINE_ON)));
            setForeground(new Color(0xA0502F));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }
    }

    static class Duende extends JComponent {
        private final Color gorro;

        Duende(Color gorro, int ancho, int alto) {
            this.gorro = gorro;
            setOpaque(false);
            Dimension d = new Dimension(ancho, alto);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setAlignmentY(Component.BOTTOM_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.scale(getWidth() / 60.0, getHeight() / 70.0);

            g2.setColor(new Color(0xF8D9BF));
            g2.fill(new Ellipse2D.Float(17, 31, 26, 25));

            Path2D.Float sombrero = new Path2D.Float();
            sombrero.moveTo(12, 38);
            sombrero.quadTo(18, 14, 44, 6);
            sombrero.quadTo(40, 20, 48, 38);
            sombrero.closePath();
            g2.setColor(gorro);
            g2.fill(sombrero);
            g2.setColor(gorro.darker());
            g2.fill(new RoundRectangle2D.Float(10, 33, 40, 9, 9, 9));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(40, 2, 8, 8));

            Path2D.Float barba = new Path2D.Float();
            barba.moveTo(17, 47);
            barba.quadTo(30, 54, 43, 47);
            barba.quadTo(44, 68, 30, 68);
            barba.quadTo(16, 68, 17, 47);
            g2.setColor(Color.WHITE);
            g2.fill(barba);
            g2.setColor(new Color(0xE6D9C2));
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(barba);

            g2.setColor(new Color(0xF4A6A0));
            g2.fill(new Ellipse2D.Float(18.5f, 45, 6, 4));
            g2.fill(new Ellipse2D.Float(35.5f, 45, 6, 4));
            g2.setColor(new Color(0x4A3426));
            g2.fill(new Ellipse2D.Float(23.5f, 41.5f, 4.4f, 5f));
            g2.fill(new Ellipse2D.Float(32.1f, 41.5f, 4.4f, 5f));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(25f, 42f, 1.7f, 1.7f));
            g2.fill(new Ellipse2D.Float(33.6f, 42f, 1.7f, 1.7f));
            g2.setColor(new Color(0xF2A58E));
            g2.fill(new Ellipse2D.Float(27.5f, 45.5f, 5f, 4.5f));
            g2.setColor(new Color(0x8E5B47));
            g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Float(26.5f, 49, 7, 4.5f, 200, 140, Arc2D.OPEN));
            g2.dispose();
        }
    }

    static class PanelCamara extends JPanel {
        private BufferedImage frame;

        PanelCamara() {
            setOpaque(false);
        }

        void setFrame(BufferedImage frame) {
            this.frame = frame;
            repaint();
        }

        private static Area arco(float x, float y, float w, float h) {
            Area a = new Area(new Ellipse2D.Float(x, y, w, w));
            a.add(new Area(new RoundRectangle2D.Float(x, y + w / 2, w, h - w / 2, 10, 10)));
            return a;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(MADERA);
            g2.fill(arco(0, 0, w, h));

            Area interior = arco(8, 8, w - 16, h - 16);
            g2.setClip(interior);
            if (frame != null) {
                g2.drawImage(frame, 8, 8, w - 16, h - 16, null);
            } else {
                g2.setColor(new Color(0x2F2018));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(0xF2D9A8));
                g2.setFont(new Font(FUENTE, Font.PLAIN, 12));
                String s = "Cámara apagada";
                g2.drawString(s, (w - g2.getFontMetrics().stringWidth(s)) / 2, h - 22);
            }
            g2.setClip(null);

            g2.setColor(new Color(255, 255, 255, 170));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{7f, 6f}, 0f));
            g2.drawOval(w / 2 - 34, h / 2 - 46, 68, 88);
            g2.dispose();
        }
    }
}