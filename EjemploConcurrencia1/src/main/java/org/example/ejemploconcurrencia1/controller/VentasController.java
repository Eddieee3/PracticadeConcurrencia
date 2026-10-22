package org.example.ejemploconcurrencia1.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.ejemploconcurrencia1.model.Caja;
import org.example.ejemploconcurrencia1.model.CajaCompartida;
import org.example.ejemploconcurrencia1.model.CajaCompartida.EstadoVendedor;

import java.util.ArrayList;
import java.util.List;

public class VentasController {

    private static final int VENDEDORES = 3;
    private static final int VENTAS_POR_VENDEDOR = 5;

    @FXML
    private HBox contenedorVendedores;

    @FXML
    private Label lblEstado;

    @FXML
    private Label lblSupervisor;

    @FXML
    private Label lblCantidadVentas;

    @FXML
    private Label lblVendido;

    @FXML
    private Label lblCajaVentas;

    @FXML
    private Label lblTotal;

    @FXML
    private RadioButton rbSincronizado;

    @FXML
    private RadioButton rbSinSincronizar;

    @FXML
    private VBox panelExplicacion;

    @FXML
    private Label lblTituloExplicacion;

    @FXML
    private Label lblExplicacion;

    @FXML
    private ProgressBar barraProgreso;

    @FXML
    private TextArea txtRegistro;

    @FXML
    private Button btnIniciar;

    @FXML
    private Button btnReiniciar;

    private final List<TarjetaVendedor> tarjetas = new ArrayList<>();

    /**
     * JavaFX lo llama al cargar la vista: crea una tarjeta por vendedor.
     */
    @FXML
    private void initialize() {
        for (int i = 1; i <= VENDEDORES; i++) {
            TarjetaVendedor tarjeta = new TarjetaVendedor(i);
            tarjetas.add(tarjeta);
            contenedorVendedores.getChildren().add(tarjeta.contenedor);
        }
        reiniciarInterfaz();
        mostrarExplicacion();
    }

    /**
     * Explica por que se usa synchronized (o que pasa sin el) segun el modo elegido.
     */
    @FXML
    private void mostrarExplicacion() {
        panelExplicacion.getStyleClass().remove("panel-explicacion-error");

        if (rbSincronizado.isSelected()) {
            lblTituloExplicacion.setText("Por que es synchronized?");
            lblExplicacion.setText("Los 3 vendedores son hilos que comparten la misma caja. "
                    + "synchronized pone un candado: solo un vendedor a la vez puede registrar su venta "
                    + "y los demas esperan su turno (\"Esperando la caja\").\n"
                    + "Razon: evitar la condicion de carrera, asi ninguna venta se pierde "
                    + "y la caja siempre cuadra con lo vendido.");
        } else {
            panelExplicacion.getStyleClass().add("panel-explicacion-error");
            lblTituloExplicacion.setText("Que pasa sin synchronized?");
            lblExplicacion.setText("Los 3 vendedores entran a la caja al mismo tiempo. "
                    + "Dos pueden leer el mismo total antes de que el otro lo guarde, "
                    + "y el ultimo en guardar borra la venta del otro.\n"
                    + "Resultado: condicion de carrera, la caja registra menos ventas y menos dinero del que se vendio.");
        }
    }

    /**
     * Borra el registro, el progreso y los resultados, y deja a todos los vendedores esperando.
     */
    private void limpiar() {
        txtRegistro.clear();
        barraProgreso.setProgress(0);
        lblSupervisor.setText("Supervisor: sin iniciar");
        for (TarjetaVendedor tarjeta : tarjetas) {
            tarjeta.mostrar(EstadoVendedor.ESPERANDO, 0, 0);
        }
        actualizarTotales();
        mostrarCaja(0, 0);
        lblTotal.getStyleClass().remove("total-error");
    }

    /**
     * Deja la ventana como al abrirla: limpia los datos y habilita los botones.
     */
    @FXML
    protected void reiniciarInterfaz() {
        limpiar();
        lblEstado.setText("Elija el modo y presione \"Iniciar ventas\" para que los vendedores empiecen a vender");
        habilitarControles(true);
    }

    private void habilitarControles(boolean habilitar) {
        btnIniciar.setDisable(!habilitar);
        btnReiniciar.setDisable(!habilitar);
        rbSincronizado.setDisable(!habilitar);
        rbSinSincronizar.setDisable(!habilitar);
    }

    /**
     * Suma lo que lleva cada tarjeta: es lo que los vendedores realmente vendieron.
     */
    private void actualizarTotales() {
        int ventas = 0;
        int total = 0;
        for (TarjetaVendedor tarjeta : tarjetas) {
            ventas += tarjeta.ventas;
            total += tarjeta.vendido;
        }
        lblCantidadVentas.setText("Vendidas: " + ventas + "/" + (VENDEDORES * VENTAS_POR_VENDEDOR));
        lblVendido.setText("C$ " + total);
    }

    /**
     * Lo que la caja logro registrar (sin synchronized puede ser menos de lo vendido).
     */
    private void mostrarCaja(int ventasRegistradas, int totalRegistrado) {
        lblCajaVentas.setText("Registradas: " + ventasRegistradas);
        lblTotal.setText("C$ " + totalRegistrado);
    }

    @FXML
    protected void onIniciarVentas() {
        boolean sincronizado = rbSincronizado.isSelected();

        limpiar();
        habilitarControles(false);
        lblEstado.setText("Ventas en proceso " + (sincronizado ? "CON" : "SIN") + " synchronized...");

        /**
         * Los avisos llegan desde los hilos vendedores y el supervisor,
         * por eso Platform.runLater() los pasa al hilo de JavaFX.
         */
        CajaCompartida.ObservadorVentas observador = new CajaCompartida.ObservadorVentas() {
            @Override
            public void mensaje(String texto) {
                Platform.runLater(() -> txtRegistro.appendText(texto + "\n"));
            }

            @Override
            public void progreso(double porcentaje) {
                Platform.runLater(() -> barraProgreso.setProgress(porcentaje));
            }

            @Override
            public void estadoVendedor(int numero, EstadoVendedor estado, int ventasRealizadas, int totalVendido) {
                Platform.runLater(() -> {
                    tarjetas.get(numero - 1).mostrar(estado, ventasRealizadas, totalVendido);
                    actualizarTotales();
                });
            }

            @Override
            public void estadoSupervisor(String texto) {
                Platform.runLater(() -> lblSupervisor.setText(texto));
            }

            @Override
            public void estadoCaja(int ventasRegistradas, int totalRegistrado) {
                Platform.runLater(() -> mostrarCaja(ventasRegistradas, totalRegistrado));
            }
        };

        /**
         * Las ventas se ejecutan en un hilo aparte para no congelar la interfaz.
         */
        Thread hiloVentas = new Thread(() -> {
            CajaCompartida cajaCompartida = new CajaCompartida(sincronizado);
            try {
                cajaCompartida.ejecutarVentas(VENDEDORES, VENTAS_POR_VENDEDOR, observador);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Caja caja = cajaCompartida.getCaja();
            int ventasCaja = caja.getCantidadVentas();
            int totalCaja = caja.getTotalRecaudado();
            boolean cuadra = totalCaja == caja.getTotalReal();

            Platform.runLater(() -> {
                mostrarCaja(ventasCaja, totalCaja);
                // Si la caja no cuadra con lo vendido, el total se pinta en rojo
                if (!cuadra) {
                    lblTotal.getStyleClass().add("total-error");
                }
                lblEstado.setText(cuadra
                        ? "Ventas finalizadas: la caja cuadra con lo vendido"
                        : "Ventas finalizadas: la caja NO cuadra, se perdieron ventas sin synchronized");
                habilitarControles(true);
            });
        });
        hiloVentas.setDaemon(true);
        hiloVentas.start();
    }

    /**
     * Tarjeta visual de un vendedor: nombre, estado, barra de avance, ventas y monto vendido.
     */
    private static class TarjetaVendedor {
        private final VBox contenedor = new VBox(8);
        private final Label lblEstado = new Label();
        private final ProgressBar barra = new ProgressBar(0);
        private final Label lblVentas = new Label();
        private final Label lblVendido = new Label();
        private int ventas;
        private int vendido;

        TarjetaVendedor(int numero) {
            Label titulo = new Label("Vendedor " + numero);
            titulo.getStyleClass().add("tarjeta-titulo");
            lblEstado.getStyleClass().add("estado");
            lblVendido.getStyleClass().add("vendido");
            barra.setMaxWidth(Double.MAX_VALUE);

            contenedor.getStyleClass().add("tarjeta");
            contenedor.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(contenedor, Priority.ALWAYS);
            contenedor.getChildren().addAll(titulo, lblEstado, barra, lblVentas, lblVendido);
        }

        void mostrar(EstadoVendedor estado, int ventas, int vendido) {
            this.ventas = ventas;
            this.vendido = vendido;

            // Cambia el color del estado segun la clase CSS (estado-esperando, estado-atendiendo, ...)
            lblEstado.setText(estado.getTexto());
            lblEstado.getStyleClass().removeIf(clase -> clase.startsWith("estado-"));
            lblEstado.getStyleClass().add("estado-" + estado.name().toLowerCase());

            barra.setProgress((double) ventas / VENTAS_POR_VENDEDOR);
            lblVentas.setText("Ventas: " + ventas + "/" + VENTAS_POR_VENDEDOR);
            lblVendido.setText("Vendido: C$ " + vendido);
        }
    }
}
