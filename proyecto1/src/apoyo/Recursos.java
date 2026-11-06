package apoyo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Recursos {

    public static final String CARPETA_IMAGENES = "recursos/imagenes/";
    public static final String CARPETA_SONIDOS = "recursos/sonidos/";
    public static final String FONDO = CARPETA_IMAGENES + "fondo.png";
    public static final String ARCHIVO_MUSICA = CARPETA_SONIDOS + "musica_fondo.mpeg";

    public static final String CARPETA_HISTORIA = "historia/";
    public static final String CARPETA_ANIMACIONES = "animaciones/";


    public static final String PREFIJO_LIBRO = "libro";
    public static final String PREFIJO_VICTORIA = "victoria";
    public static final int PAGINAS_LIBRO_MINIMAS = 3;
    public static final int FOTOGRAMAS_VICTORIA_MINIMOS = 3;

    public static final boolean TEXTO_EN_IMAGEN_HISTORIA = true;

    public static final int CANTIDAD_OBJETOS = 18;
    public static final String CARTA_REVERSO = "objetos/carta_reverso.png";
    public static final String CASILLA_PATRON = "patrones/casilla_patron.png";
    public static final String CASILLA_PATRON_ACTIVA = "patrones/casilla_patron_activa.png";
    public static final String[] DUENDES = {
            "premios/duende_verde.png", "premios/duende_rojo.png", "premios/duende_amarillo.png",
            "premios/duende_morado.png", "premios/duende_azul.png"
    };


    public static final Color CAFE = new Color(110, 70, 35);
    public static final Color CAFE_OSCURO = new Color(70, 42, 20);
    public static final Color VERDE = new Color(76, 110, 50);
    public static final Color VERDE_OSCURO = new Color(48, 78, 38);
    public static final Color CREMA = new Color(250, 236, 205);
    public static final Color CREMA_OSCURO = new Color(232, 208, 160);
    public static final Color TEXTO = new Color(50, 35, 20);
    public static final Color ROJO = new Color(165, 50, 45);

    // ---------- Ventanillas de aviso ----------

    public static final String AVISO_FONDO = "avisos/fondo_aviso.png";
    private static final int AVISO_ANCHO = 700;
    // Medidas sobre la imagen original (959 x 679): eje central de la zona crema y
    // línea donde termina el botón OK (el marco verde inferior empieza en y=650)
    private static final int AVISO_CENTRO_X = 425;
    private static final int AVISO_BASE_BOTON_Y = 626;

    /**
     * Muestra un aviso con la temática del juego en lugar del JOptionPane estándar.
     * Todo el aspecto viene de la imagen de fondo y de los colores del juego; encima solo se
     * colocan el título, el mensaje y el botón. Si falta la imagen, se usa el JOptionPane normal.
     */
    public static void mostrarAviso(Component padre, String titulo, String mensaje) {
        // Swing solo debe usarse desde el hilo de eventos; si se llama desde otro hilo se reprograma
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mostrarAviso(padre, titulo, mensaje));
            return;
        }
        BufferedImage fondo = Imagenes.cargar(AVISO_FONDO);
        if (fondo == null) {
            JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Las zonas están medidas sobre la imagen original (959 x 679 px) y se escalan al ancho final
        double e = (double) AVISO_ANCHO / fondo.getWidth();
        Dimension tamano = new Dimension(AVISO_ANCHO, (int) Math.round(fondo.getHeight() * e));

        Window dueno = padre == null ? null
                : (padre instanceof Window ? (Window) padre : SwingUtilities.getWindowAncestor(padre));
        JDialog dialogo = new JDialog(dueno, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setUndecorated(true);
        dialogo.setBackground(new Color(0, 0, 0, 0)); // transparente: solo se ve la imagen

        PanelImagen panel = new PanelImagen(AVISO_FONDO, null, tamano);
        panel.setPreferredSize(tamano);

        // Título sobre el banner verde (se reduce si es muy largo)
        JLabel lblTitulo = crearEtiqueta(titulo, 34, true, CREMA);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setBounds((int) (160 * e), (int) (120 * e), (int) (515 * e), (int) (80 * e));
        for (int tam = 34; tam > 18; tam--) {
            lblTitulo.setFont(fuente(Font.BOLD, tam));
            if (lblTitulo.getFontMetrics(lblTitulo.getFont()).stringWidth(titulo) <= lblTitulo.getWidth() - 20) {
                break;
            }
        }

        // Mensaje sobre la zona crema: varias líneas, centrado; la letra se reduce si no cabe
        // La zona crema de la imagen va de x=21 a x=830 (centro en x=425); el mensaje y el botón
        // se centran sobre ese eje para que queden alineados con el marco.
        Rectangle zona = new Rectangle((int) Math.round((AVISO_CENTRO_X - 370) * e), (int) (222 * e),
                (int) Math.round(740 * e), (int) (318 * e));
        String texto = mensaje == null ? "" : mensaje.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        boolean unaLinea = !texto.contains("\n");
        String[] partes = texto.split("\n", 2);
        String cuerpo = unaLinea ? texto
                : "<b style='font-size:1.15em'>" + partes[0] + "</b><br>" + partes[1].replace("\n", "<br>");
        JLabel lblMensaje = new JLabel("<html><div style='text-align:center; width:" + (zona.width - 30) + "'>"
                + cuerpo + "</div></html>", SwingConstants.CENTER);
        lblMensaje.setForeground(TEXTO);
        lblMensaje.setBounds(zona);
        for (int tam = unaLinea ? 23 : 19; tam >= 12; tam--) {
            lblMensaje.setFont(fuente(unaLinea ? Font.BOLD : Font.PLAIN, tam));
            if (lblMensaje.getPreferredSize().height <= zona.height) {
                break;
            }
        }

        // Botón OK con la imagen verde del juego
        BotonImagen btnOk = new BotonImagen("OK", RutasLogin.BOTON_VERDE, RutasLogin.BOTON_VERDE_PRES);
        btnOk.setFont(fuente(Font.BOLD, 24));
        // Centrado horizontal exacto sobre la zona crema y separado del borde inferior del marco
        int anchoOk = (int) Math.round(300 * e);
        int altoOk = (int) Math.round(64 * e);
        int xOk = (int) Math.round(AVISO_CENTRO_X * e) - anchoOk / 2;
        int yOk = (int) Math.round(AVISO_BASE_BOTON_Y * e) - altoOk;
        btnOk.setBounds(xOk, yOk, anchoOk, altoOk);
        btnOk.addActionListener(ev -> dialogo.dispose());

        panel.add(lblTitulo);
        panel.add(lblMensaje);
        panel.add(btnOk);

        // Como no tiene barra de título, se cierra con OK, Enter o Esc. No se puede arrastrar.
        dialogo.setContentPane(panel);
        dialogo.setResizable(false);
        dialogo.getRootPane().setDefaultButton(btnOk);
        dialogo.getRootPane().registerKeyboardAction(ev -> dialogo.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialogo.pack();
        // Siempre centrada sobre la ventana principal del juego (aunque el aviso salga de otro diálogo)
        dialogo.setLocationRelativeTo(ventanaPrincipal(dueno));
        final Point posicionFija = dialogo.getLocation();
        // Si el sistema intenta moverla (p. ej. con atajos de teclado), vuelve a su lugar
        dialogo.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent ev) {
                if (!dialogo.getLocation().equals(posicionFija)) {
                    dialogo.setLocation(posicionFija);
                }
            }
        });
        btnOk.requestFocusInWindow();
        dialogo.setVisible(true);
    }

    /** Busca la ventana principal (el JFrame) subiendo por los dueños de la ventana dada. */
    private static Window ventanaPrincipal(Window ventana) {
        Window actual = ventana;
        while (actual != null && !(actual instanceof Frame) && actual.getOwner() != null) {
            actual = actual.getOwner();
        }
        return actual;
    }


    public static String rutaObjeto(int numero) {
        return String.format("objetos/objeto_%02d.png", numero);
    }

    public static String rutaMedalla(int posicion) {
        return "premios/medalla_" + posicion + ".png";
    }


    public static String rutaSecuencia(String carpeta, String prefijo, int numero) {
        return String.format("%s%s_%02d.png", carpeta, prefijo, numero);
    }

    public static int contarSecuencia(String carpeta, String prefijo, int minimo) {
        int n = 0;
        while (new File(CARPETA_IMAGENES + rutaSecuencia(carpeta, prefijo, n + 1)).exists()) {
            n++;
        }
        return Math.max(n, minimo);
    }

    public static Font fuente(int estilo, int tamano) {
        return new Font("Serif", estilo, tamano);
    }

    public static ImageIcon cargarImagen(String ruta, int maxAncho, int maxAlto, Color colorMarcador) {
        maxAncho = Math.max(maxAncho, 8);
        maxAlto = Math.max(maxAlto, 8);
        ImageIcon original = new ImageIcon(CARPETA_IMAGENES + ruta);
        if (original.getIconWidth() <= 0) {
            String nombre = ruta.substring(ruta.lastIndexOf('/') + 1);
            return crearMarcador(maxAncho, maxAlto, nombre, colorMarcador);
        }
        double factor = Math.min((double) maxAncho / original.getIconWidth(),
                (double) maxAlto / original.getIconHeight());
        int ancho = Math.max(1, (int) (original.getIconWidth() * factor));
        int alto = Math.max(1, (int) (original.getIconHeight() * factor));
        return new ImageIcon(original.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
    }


    public static ImageIcon crearMarcador(int ancho, int alto, String texto, Color fondo) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int arco = Math.max(6, Math.min(ancho, alto) / 6);
        g.setColor(fondo);
        g.fillRoundRect(2, 2, ancho - 4, alto - 4, arco, arco);
        g.setColor(CAFE_OSCURO);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(2, 2, ancho - 5, alto - 5, arco, arco);
        boolean oscuro = (fondo.getRed() + fondo.getGreen() + fondo.getBlue()) / 3 < 128;
        g.setColor(oscuro ? CREMA : TEXTO);

        String[] lineas = {texto};
        int tam = ajustarFuente(g, lineas, ancho, alto);
        if (tam < 14) {
            lineas = texto.replaceAll("\\.[^.]*$", "").split("_");
            tam = ajustarFuente(g, lineas, ancho, alto);
        }
        Font f = fuente(Font.BOLD, tam);
        FontMetrics fm = g.getFontMetrics(f);
        g.setFont(f);
        int altoLinea = fm.getHeight();
        int y = (alto - altoLinea * lineas.length) / 2 + fm.getAscent();
        for (String linea : lineas) {
            g.drawString(linea, (ancho - fm.stringWidth(linea)) / 2, y);
            y += altoLinea;
        }
        g.dispose();
        return new ImageIcon(img);
    }

    private static int ajustarFuente(Graphics2D g, String[] lineas, int ancho, int alto) {
        for (int tam = 28; tam > 8; tam--) {
            FontMetrics fm = g.getFontMetrics(fuente(Font.BOLD, tam));
            int anchoMax = 0;
            for (String linea : lineas) {
                anchoMax = Math.max(anchoMax, fm.stringWidth(linea));
            }
            if (anchoMax <= ancho - 12 && fm.getHeight() * lineas.length <= alto - 8) {
                return tam;
            }
        }
        return 8;
    }

    public static JLabel crearEtiqueta(String texto, int tamano, boolean negrita, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(negrita ? Font.BOLD : Font.PLAIN, tamano));
        etiqueta.setForeground(color);
        return etiqueta;
    }

    public static PanelRedondeado crearBanner(String texto, int tamano) {
        PanelRedondeado p = new PanelRedondeado(new GridBagLayout(), CAFE, CAFE_OSCURO, 36);
        p.setBorder(BorderFactory.createEmptyBorder(6, 44, 10, 44));
        p.add(crearEtiqueta(texto, tamano, true, CREMA));
        return p;
    }

    public static PanelRedondeado crearSubtitulo(String texto) {
        PanelRedondeado p = new PanelRedondeado(new GridBagLayout(), CREMA, CAFE, 30);
        p.setBorder(BorderFactory.createEmptyBorder(4, 34, 6, 34));
        p.add(crearEtiqueta(texto, 26, false, TEXTO));
        return p;
    }

    public static BotonBosque crearBoton(String texto) {
        return new BotonBosque(texto, VERDE);
    }

    public static BotonBosque crearBoton(String texto, Color color) {
        return new BotonBosque(texto, color);
    }

    public static class PanelRedondeado extends JPanel {
        private Color relleno;
        private final Color borde;
        private final int arco;

        public PanelRedondeado(LayoutManager layout, Color relleno, Color borde, int arco) {
            super(layout);
            this.relleno = relleno;
            this.borde = borde;
            this.arco = arco;
            setOpaque(false);
        }

        public void setRelleno(Color relleno) {
            this.relleno = relleno;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(relleno);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
            if (borde != null) {
                g2.setColor(borde);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arco, arco);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class BotonBosque extends JButton {
        private Color colorFondo;

        public BotonBosque(String texto, Color colorFondo) {
            super(texto);
            this.colorFondo = colorFondo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(CREMA);
            setFont(fuente(Font.BOLD, 22));
            setMargin(new Insets(8, 26, 8, 26));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void setColorFondo(Color colorFondo) {
            this.colorFondo = colorFondo;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isPressed() ? colorFondo.darker() : colorFondo;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g2.setColor(CAFE_OSCURO);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
