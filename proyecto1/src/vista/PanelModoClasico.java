package vista;

import apoyo.PanelConFondo;
import apoyo.Recursos;
import apoyo.Recursos.PanelRedondeado;
import controlador.JuegoController;

import javax.swing.*;
import java.awt.*;

    public class PanelModoClasico extends PanelConFondo {

        private final JuegoController controller;
        private final PanelTablero[] tableros = new PanelTablero[2];
        private boolean objetosVisibles = false;

        public PanelModoClasico(JuegoController controller) {
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
            norte.add(Recursos.crearBanner("Modo clásico", 42), gc);
            gc.gridy = 1;
            gc.insets = new Insets(6, 0, 0, 0);
            norte.add(Recursos.crearSubtitulo("¡Encuentra todas las parejas!"), gc);
            add(norte, BorderLayout.NORTH);

            JPanel centro = new JPanel(new GridBagLayout());
            centro.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridy = 0;
            c.insets = new Insets(8, 0, 4, 0);

            c.gridx = 0;
            c.weightx = 1;
            centro.add(crearColumnaJugador(1), c);
            c.gridx = 1;
            c.weightx = 0;
            c.insets = new Insets(8, 14, 4, 14);
            centro.add(crearColumnaCentral(), c);
            c.gridx = 2;
            c.weightx = 1;
            c.insets = new Insets(8, 0, 4, 0);
            centro.add(crearColumnaJugador(2), c);
            add(centro, BorderLayout.CENTER);
            Recursos.BotonBosque btnVolver = Recursos.crearBoton("Volver al menú");
            btnVolver.setFont(Recursos.fuente(Font.BOLD, 24));
            btnVolver.addActionListener(evt -> controller.volverAlMenu());

            Recursos.BotonBosque btnVerObjetos = Recursos.crearBoton("Ver objetos (demo)", Recursos.CAFE);
            btnVerObjetos.setFont(Recursos.fuente(Font.BOLD, 18));
            btnVerObjetos.addActionListener(evt -> {
                objetosVisibles = !objetosVisibles;
                for (PanelTablero t : tableros) {
                    t.mostrarFrente(objetosVisibles);
                }
            });

            Recursos.BotonBosque btnVictoria = Recursos.crearBoton("Animación de victoria (demo)", Recursos.CAFE);
            btnVictoria.setFont(Recursos.fuente(Font.BOLD, 18));
            btnVictoria.addActionListener(evt -> controller.mostrarAnimacionVictoria());

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
            botones.setOpaque(false);
            botones.add(btnVerObjetos);
            botones.add(btnVolver);
            botones.add(btnVictoria);
            add(botones, BorderLayout.SOUTH);
        }
        private JPanel crearColumnaJugador(int numero) {
            JPanel columna = new JPanel(new BorderLayout(0, 8));
            columna.setOpaque(false);
            columna.add(crearPanelJugador(numero), BorderLayout.NORTH);

            final PanelTablero tablero = new PanelTablero(controller.getFilasTablero(),
                    controller.getColumnasTablero(), Recursos.VERDE);
            tablero.setImagenBase(Recursos.CARTA_REVERSO);
            tablero.setImagenesFrente(controller.getImagenesTableroClasico(numero));
            tablero.setOyenteCasillas((f, col) -> tablero.alternarFrente(f, col));
            tableros[numero - 1] = tablero;
            columna.add(tablero, BorderLayout.CENTER);
            return columna;
        }
        private JPanel crearPanelJugador(int numero) {
            PanelRedondeado panel = new PanelRedondeado(new BorderLayout(14, 0), Recursos.CREMA, Recursos.CAFE, 30);
            panel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            JLabel avatar = new JLabel(Recursos.cargarImagen(Recursos.DUENDES[numero == 1 ? 0 : 1], 72, 72,
                    Recursos.CREMA_OSCURO));
            panel.add(avatar, BorderLayout.WEST);

            JPanel datos = new JPanel(new GridLayout(2, 1));
            datos.setOpaque(false);
            datos.add(Recursos.crearEtiqueta(controller.getNombreJugador(numero), 28, true, Recursos.TEXTO));
            datos.add(Recursos.crearEtiqueta("Intentos: " + controller.getIntentosJugador(numero), 22, false,
                    Recursos.TEXTO));
            panel.add(datos, BorderLayout.CENTER);

            PanelRedondeado tiempo = new PanelRedondeado(new GridLayout(2, 1),
                    numero == 1 ? Recursos.VERDE : Recursos.ROJO,
                    Recursos.CAFE_OSCURO, 20);
            tiempo.setBorder(BorderFactory.createEmptyBorder(2, 16, 4, 16));
            JLabel lblTiempo = Recursos.crearEtiqueta("Tiempo:", 18, false, Recursos.CREMA);
            lblTiempo.setHorizontalAlignment(JLabel.CENTER);
            JLabel lblReloj = Recursos.crearEtiqueta(controller.getTextoTiempoInicial(), 30, true, Recursos.CREMA);
            lblReloj.setHorizontalAlignment(JLabel.CENTER);
            tiempo.add(lblTiempo);
            tiempo.add(lblReloj);
            panel.add(tiempo, BorderLayout.EAST);
            return panel;
        }
        private JPanel crearColumnaCentral() {
            JPanel central = new JPanel(new GridBagLayout());
            central.setOpaque(false);
            central.setPreferredSize(new Dimension(190, 100));
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.weighty = 1;

            PanelRedondeado vs = new PanelRedondeado(new GridBagLayout(), Recursos.CREMA, Recursos.CAFE, 200);
            vs.setPreferredSize(new Dimension(120, 120));
            vs.add(Recursos.crearEtiqueta("VS", 44, true, Recursos.CAFE_OSCURO));
            gc.gridy = 0;
            central.add(vs, gc);

            PanelRedondeado turno = new PanelRedondeado(new GridLayout(2, 1), Recursos.CREMA, Recursos.CAFE, 24);
            turno.setBorder(BorderFactory.createEmptyBorder(8, 14, 10, 14));
            JLabel lblTurno = Recursos.crearEtiqueta("Turno de:", 20, false, Recursos.TEXTO);
            lblTurno.setHorizontalAlignment(JLabel.CENTER);
            JLabel lblJugador = Recursos.crearEtiqueta(controller.getTextoTurno(), 24, true, Recursos.VERDE_OSCURO);
            lblJugador.setHorizontalAlignment(JLabel.CENTER);
            turno.add(lblTurno);
            turno.add(lblJugador);
            gc.gridy = 1;
            central.add(turno, gc);
            return central;
        }
    }
