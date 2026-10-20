package org.example.ejemploconcurrencia1;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Caja caja = new Caja();

        //Runnable representa la tarea que ejecutara cada vendedor

        Runnable vendedor1 = () -> procesarVentas("Vendedor 1", caja);
        Runnable vendedor2 = () -> procesarVentas("Vendedor 2", caja);
        Runnable vendedor3 = () -> procesarVentas("Vendedor 3", caja);

        //Crear los tres hilos para llevar acabo las operaciones de los vendedores
        Thread hilo1 = new Thread(vendedor1);
        Thread hilo2 = new Thread(vendedor2);
        Thread hilo3 = new Thread(vendedor3);

        System.out.println("-- Inicio del proceso de ventas --");

        /**
         * hilo1.start() inicia la ejecución del hilo thread1, que ejecutará la tarea definida en el Runnable vendedor1.
         * Los tres vendedores comnezaran a procesar sus ventas de manera concurrente, es decir, al mismo tiempo.
         */
        //Vendedor1
        hilo1.start();
        //Vendedor2
        hilo2.start();
        //Vendedor3
        hilo3.start();

        /**
         * join() hace que el hilo main espere hasta que los demas terminen
         */
        hilo1.join();
        hilo2.join();
        hilo3.join();

        System.out.println("-- Resultados finales --");
        System.out.println("Cantidad de ventas: " + caja.getCantidadVentas());
        System.out.println("Total de ventas: " + caja.getTotalRecaudado());
    }


        private static void procesarVentas(String vendedor, Caja caja) {
            for(int i = 1; i <= 5; i++) {
                try {
                    /**
                     * Simular que procesamos una venta que tarda aproximandamente 500 milisegundos
                     */
                    Thread.sleep(200);
                    caja.registrarVenta(vendedor, 100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.out.println(vendedor + " fue interrumpido durante la venta ");
                    return;

                }
            }
        }

    }

