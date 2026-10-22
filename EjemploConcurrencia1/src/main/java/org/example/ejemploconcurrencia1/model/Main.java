package org.example.ejemploconcurrencia1.model;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        /**
         * Tres vendedores procesan 5 ventas cada uno de manera concurrente,
         * todos registrando en la misma caja, mientras el supervisor revisa el avance.
         * Primero con synchronized y luego sin synchronized para comparar.
         */
        System.out.println("========== CON synchronized ==========");
        new CajaCompartida(true).ejecutarVentas(3, 5);

        System.out.println();
        System.out.println("========== SIN synchronized ==========");
        new CajaCompartida(false).ejecutarVentas(3, 5);

        System.out.println();
        System.out.println("========== Por que synchronized ==========");
        System.out.println("Los 3 vendedores son hilos que comparten la misma caja.");
        System.out.println("synchronized deja que solo un hilo a la vez registre la venta; los demas esperan su turno.");
        System.out.println("Razon: evitar la condicion de carrera. Sin synchronized dos vendedores leen el mismo total");
        System.out.println("y el ultimo en guardar borra la venta del otro, por eso la caja no cuadra con lo vendido.");
    }
}
