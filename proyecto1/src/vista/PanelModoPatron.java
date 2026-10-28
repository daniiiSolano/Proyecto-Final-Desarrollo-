package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;

import javax.swing.*;
import java.awt.*;
public class PanelModoPatron extends PanelConFondo {

    private static final String[] INSTRUCCIONES = {
            "Observa el patrón que se mostrará al inicio.",
            "Recuerda el orden de las casillas.",
            "Presiona las mismas casillas en el mismo orden.",
            "Si lo logras, pasarás al siguiente nivel con un patrón más largo.",
            "Si fallas, el juego termina."
    };

    private final JuegoController controller;

    public PanelModoPatron(JuegoController controller) {
        super(Recursos.FONDO);
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 30, 12, 30));

        JPanel norte = new JPanel(new GridBagLayout());
        norte.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        norte.add(Recursos.crearBanner("Modo patrones", 42), gc);
        gc.gridy = 1;
        gc.insets = new Insets(6, 0, 0, 0);
        norte.add(Recursos.crearSubtitulo("Memoriza el patrón de casillas y repítelo en el mismo orden."), gc);
        add(norte, BorderLayout.NORTH);


        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.insets = new Insets(10, 8, 6, 8);

        c.gridx = 0;
        c.weightx = 0.25;
        centro.add(crearPanelIzquierdo(), c);

        PanelTablero tablero = new PanelTablero(controller.getFilasTablero(), controller.getColumnasTablero(),
                Recursos.VERDE);
        tablero.setImagenBase(Recursos.CASILLA_PATRON);
        tablero.setImagenAlterna(Recursos.CASILLA_PATRON_ACTIVA);

        tablero.setOyenteCasillas((f, col) -> tablero.marcarCasilla(f, col, !tablero.estaMarcada(f, col)));
        c.gridx = 1;
        c.weightx = 0.5;
        centro.add(tablero, c);

        c.gridx = 2;
        c.weightx = 0.25;
        centro.add(crearPanelInstrucciones(), c);
        add(centro, BorderLayout.CENTER);

        Recursos.BotonBosque btnVolver = Recursos.crearBoton("Volver al menú");
        btnVolver.setFont(Recursos.fuente(Font.BOLD, 24));
        btnVolver.addActionListener(evt -> controller.volverAlMenu());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        botones.setOpaque(false);
        botones.add(btnVolver);
        add(botones, BorderLayout.SOUTH);
    }

    /** Nivel actual, longitud del patrón y los dos indicadores de tiempo. */
    private JPanel crearPanelIzquierdo() {
        PanelRedondeado panel = new PanelRedondeado(new GridBagLayout(), Recursos.CREMA, Recursos.CAFE, 30);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setPreferredSize(new Dimension(320, 100));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);
        int fila = 0;

        gc.gridy = fila++;
        panel.add(centrada("Nivel actual", 26, true), gc);
        PanelRedondeado circulo = new PanelRedondeado(new GridBagLayout(), Recursos.CAFE, Recursos.CAFE_OSCURO, 90);
        circulo.setPreferredSize(new Dimension(68, 68));
        circulo.add(Recursos.crearEtiqueta(String.valueOf(controller.getNivelPatron()), 36, true, Recursos.CREMA));
        JPanel envoltorio = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        envoltorio.setOpaque(false);
        envoltorio.add(circulo);
        gc.gridy = fila++;
        panel.add(envoltorio, gc);


        gc.gridy = fila++;
        gc.insets = new Insets(20, 0, 2, 0);
        panel.add(centrada("Longitud del patrón", 24, true), gc);
        gc.gridy = fila++;
        gc.insets = new Insets(0, 0, 4, 0);
        panel.add(centrada(controller.getLongitudPatron() + " casillas", 30, false), gc);

        gc.gridy = fila++;
        gc.insets = new Insets(20, 0, 4, 0);
        panel.add(centrada("Tiempo restante", 24, true), gc);
        gc.gridy = fila++;
        gc.insets = new Insets(0, 0, 2, 0);
        panel.add(crearBarra(controller.getTiempoTotalPatron()), gc);
        gc.gridy = fila++;
        panel.add(centrada(controller.getTiempoTotalPatron() + " segundos", 22, false), gc);

        gc.gridy = fila++;
        gc.insets = new Insets(20, 0, 4, 0);
        panel.add(centrada("Tiempo entre casillas", 24, true), gc);
        gc.gridy = fila++;
        gc.insets = new Insets(0, 0, 2, 0);
        panel.add(crearBarra(controller.getTiempoEntreCasillas()), gc);
        gc.gridy = fila++;
        panel.add(centrada(controller.getTiempoEntreCasillas() + " segundos", 22, false), gc);
        gc.gridy = fila;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        panel.add(relleno, gc);
        return panel;
    }
    private JPanel crearPanelInstrucciones() {
        PanelRedondeado panel = new PanelRedondeado(new BorderLayout(0, 10), Recursos.CREMA, Recursos.CAFE, 30);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        panel.setPreferredSize(new Dimension(360, 100));
        panel.add(centrada("Instrucciones", 30, true), BorderLayout.NORTH);

        JPanel lista = new JPanel(new GridLayout(INSTRUCCIONES.length, 1, 0, 6));
        lista.setOpaque(false);
        for (int i = 0; i < INSTRUCCIONES.length; i++) {
            JPanel fila = new JPanel(new BorderLayout(10, 0));
            fila.setOpaque(false);

            PanelRedondeado numero = new PanelRedondeado(new GridBagLayout(), Recursos.VERDE,
                    Recursos.VERDE_OSCURO, 44);
            numero.setPreferredSize(new Dimension(44, 44));
            numero.add(Recursos.crearEtiqueta(String.valueOf(i + 1), 22, true, Recursos.CREMA));
            JPanel marco = new JPanel(new GridBagLayout());
            marco.setOpaque(false);
            marco.add(numero);
            fila.add(marco, BorderLayout.WEST);

            JTextArea texto = new JTextArea(INSTRUCCIONES[i]);
            texto.setEditable(false);
            texto.setFocusable(false);
            texto.setOpaque(false);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            texto.setFont(Recursos.fuente(Font.PLAIN, 20));
            texto.setForeground(Recursos.TEXTO);
            // Se centra verticalmente respecto al círculo numerado
            JPanel marcoTexto = new JPanel(new GridBagLayout());
            marcoTexto.setOpaque(false);
            GridBagConstraints gt = new GridBagConstraints();
            gt.weightx = 1;
            gt.fill = GridBagConstraints.HORIZONTAL;
            marcoTexto.add(texto, gt);
            fila.add(marcoTexto, BorderLayout.CENTER);
            lista.add(fila);
        }
        panel.add(lista, BorderLayout.CENTER);
        return panel;
    }

    private JLabel centrada(String texto, int tamano, boolean negrita) {
        JLabel etiqueta = Recursos.crearEtiqueta(texto, tamano, negrita, Recursos.TEXTO);
        etiqueta.setHorizontalAlignment(JLabel.CENTER);
        return etiqueta;
    }
    private JProgressBar crearBarra(int maximo) {
        JProgressBar barra = new JProgressBar(0, maximo);
        barra.setValue(maximo);
        barra.setForeground(Recursos.VERDE);
        barra.setBackground(Recursos.CREMA_OSCURO);
        barra.setBorder(BorderFactory.createLineBorder(Recursos.CAFE, 2));
        barra.setPreferredSize(new Dimension(100, 22));
        return barra;
    }
}
