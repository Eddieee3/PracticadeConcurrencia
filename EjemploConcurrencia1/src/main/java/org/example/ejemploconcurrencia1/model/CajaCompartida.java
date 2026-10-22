package org.example.ejemploconcurrencia1.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class CajaCompartida {

    /**
     * Los pasos por los que pasa cada vendedor durante la simulacion.
     */
    public enum EstadoVendedor {
        ESPERANDO("Esperando"),
        ATENDIENDO("Atendiendo cliente"),
        ESPERANDO_CAJA("Esperando la caja"),
        REGISTRANDO("Registrando venta"),
        TERMINADO("Termino");

        private final String texto;

        EstadoVendedor(String texto) {
            this.texto = texto;
        }

        public String getTexto() {
            return texto;
        }
    }

    /**
     * Permite que quien ejecuta las ventas (consola o interfaz) reciba
     * los mensajes, el progreso y el estado de cada hilo.
     */
    public interface ObservadorVentas {
        void mensaje(String texto);

        void progreso(double porcentaje);

        default void estadoVendedor(int numero, EstadoVendedor estado, int ventasRealizadas, int totalVendido) {
        }

        default void estadoSupervisor(String texto) {
        }

        default void estadoCaja(int ventasRegistradas, int totalRegistrado) {
        }
    }

    // Catalogo de la tienda: nombre del producto y su precio en C$
    private static final String[] PRODUCTOS = {
            "Gaseosa", "Pan", "Arroz (lb)", "Frijoles (lb)", "Queso (lb)", "Leche", "Cafe"
    };
    private static final int[] PRECIOS = {35, 20, 18, 30, 90, 40, 75};

    private final Caja caja = new Caja();

    // true: la caja se usa con synchronized, false: sin synchronized
    private final boolean sincronizado;

    // Ventas que los vendedores realmente hicieron (AtomicInteger nunca pierde un incremento)
    private final AtomicInteger ventasRealizadas = new AtomicInteger();

    public CajaCompartida(boolean sincronizado) {
        this.sincronizado = sincronizado;
    }

    public void ejecutarVentas(int cantidadVendedores, int ventasPorVendedor)
            throws InterruptedException {
        ejecutarVentas(cantidadVendedores, ventasPorVendedor, new ObservadorVentas() {
            @Override
            public void mensaje(String texto) {
                System.out.println(texto);
            }

            @Override
            public void progreso(double porcentaje) {
            }

            @Override
            public void estadoSupervisor(String texto) {
                System.out.println(texto);
            }
        });
    }

    /**
     * Crea un hilo por vendedor, todos registran sus ventas en la misma caja.
     * Un hilo supervisor revisa periodicamente el avance hasta que todos terminan.
     * join() espera a que vendedores y supervisor terminen antes de regresar.
     */
    public void ejecutarVentas(int cantidadVendedores, int ventasPorVendedor,
                               ObservadorVentas observador) throws InterruptedException {
        List<Thread> vendedores = new ArrayList<>();
        int ventasEsperadas = cantidadVendedores * ventasPorVendedor;

        for (int i = 1; i <= cantidadVendedores; i++) {
            int numero = i;
            String vendedor = "Vendedor " + numero;
            //Runnable representa la tarea que ejecutara cada vendedor
            Runnable tarea = () -> procesarVentas(numero, vendedor, ventasPorVendedor, observador);
            vendedores.add(new Thread(tarea, vendedor));
        }

        /**
         * El supervisor no vende, solo observa: cada 300 ms consulta la caja
         * y revisa que vendedores siguen activos con isAlive().
         */
        Thread supervisor = new Thread(() -> {
            observador.mensaje("Supervisor: inicia la supervision de " + cantidadVendedores + " vendedores");

            while (vendedores.stream().anyMatch(Thread::isAlive)) {
                long activos = vendedores.stream().filter(Thread::isAlive).count();
                int vendidas = ventasRealizadas.get();

                observador.estadoSupervisor("Supervisor: vendidas " + vendidas + "/" + ventasEsperadas
                        + " | En caja: " + caja.getCantidadVentas()
                        + " | Vendedores activos: " + activos);
                observador.estadoCaja(caja.getCantidadVentas(), caja.getTotalRecaudado());
                observador.progreso((double) vendidas / ventasEsperadas);

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    observador.mensaje("Supervisor: fue interrumpido");
                    return;
                }
            }

            observador.progreso(1.0);
            observador.estadoCaja(caja.getCantidadVentas(), caja.getTotalRecaudado());

            /**
             * Compara lo que los vendedores vendieron contra lo que la caja registro.
             * Con synchronized siempre coincide; sin synchronized se pierden ventas.
             */
            String resultado;
            if (caja.getCantidadVentas() == ventasRealizadas.get()
                    && caja.getTotalRecaudado() == caja.getTotalReal()) {
                resultado = "Supervisor: todas las ventas se registraron correctamente";
            } else {
                resultado = "Supervisor: ALERTA, condicion de carrera. Se vendieron " + ventasRealizadas.get()
                        + " pero la caja registro " + caja.getCantidadVentas();
            }
            observador.estadoSupervisor(resultado);
        }, "Supervisor");

        observador.mensaje("-- Inicio del proceso de ventas "
                + (sincronizado ? "CON" : "SIN") + " synchronized --");

        for (Thread vendedor : vendedores) {
            vendedor.start();
        }
        supervisor.start();

        for (Thread vendedor : vendedores) {
            vendedor.join();
        }
        supervisor.join();

        observador.mensaje("-- Resultados finales --");
        for (Map.Entry<String, Integer> entrada : caja.getTotalPorVendedor().entrySet()) {
            observador.mensaje(entrada.getKey() + ": C$ " + entrada.getValue());
        }
        observador.mensaje("Ventas realizadas: " + ventasRealizadas.get()
                + " | Registradas en caja: " + caja.getCantidadVentas());
        observador.mensaje("Total vendido: C$ " + caja.getTotalReal()
                + " | Total en caja: C$ " + caja.getTotalRecaudado());

        int ventasPerdidas = ventasRealizadas.get() - caja.getCantidadVentas();
        if (ventasPerdidas > 0) {
            observador.mensaje("Se perdieron " + ventasPerdidas + " ventas (C$ "
                    + (caja.getTotalReal() - caja.getTotalRecaudado()) + ") por no usar synchronized");
        }
    }

    /**
     * Cada vendedor atiende a sus clientes: elige un producto al azar del catalogo
     * y una cantidad entre 1 y 5, y registra la venta en la caja compartida.
     * En cada paso avisa su estado para que la interfaz lo muestre.
     */
    private void procesarVentas(int numero, String vendedor, int ventasPorVendedor,
                                ObservadorVentas observador) {
        ThreadLocalRandom aleatorio = ThreadLocalRandom.current();
        int totalVendido = 0;

        for (int i = 0; i < ventasPorVendedor; i++) {
            try {
                /**
                 * Atender al cliente tarda entre 300 y 900 milisegundos, asi los vendedores
                 * avanzan a ritmos distintos y el supervisor lo puede notar.
                 */
                observador.estadoVendedor(numero, EstadoVendedor.ATENDIENDO, i, totalVendido);
                Thread.sleep(aleatorio.nextInt(300, 900));

                int indice = aleatorio.nextInt(PRODUCTOS.length);
                int cantidad = aleatorio.nextInt(1, 6);

                String ticket;
                if (sincronizado) {
                    /**
                     * synchronized (caja) usa el mismo candado que registrarVentaSincronizada(),
                     * asi se ve cuando un vendedor queda esperando porque otro esta usando la caja.
                     */
                    observador.estadoVendedor(numero, EstadoVendedor.ESPERANDO_CAJA, i, totalVendido);
                    synchronized (caja) {
                        observador.estadoVendedor(numero, EstadoVendedor.REGISTRANDO, i, totalVendido);
                        ticket = caja.registrarVentaSincronizada(vendedor, PRODUCTOS[indice], cantidad, PRECIOS[indice]);
                    }
                } else {
                    // Sin synchronized nadie espera: varios vendedores registran al mismo tiempo
                    observador.estadoVendedor(numero, EstadoVendedor.REGISTRANDO, i, totalVendido);
                    ticket = caja.registrarVentaSinSincronizar(vendedor, PRODUCTOS[indice], cantidad, PRECIOS[indice]);
                }

                totalVendido += cantidad * PRECIOS[indice];
                ventasRealizadas.incrementAndGet();
                observador.mensaje(ticket);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                observador.mensaje(vendedor + " fue interrumpido durante la venta");
                return;
            }
        }

        observador.estadoVendedor(numero, EstadoVendedor.TERMINADO, ventasPorVendedor, totalVendido);
        observador.mensaje(vendedor + " termino sus " + ventasPorVendedor + " ventas");
    }

    public Caja getCaja() {
        return caja;
    }
}
