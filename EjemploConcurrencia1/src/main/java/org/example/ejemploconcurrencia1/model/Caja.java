package org.example.ejemploconcurrencia1.model;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Caja {

    // volatile: el supervisor siempre lee el valor mas reciente (pero NO evita la condicion de carrera)
    private volatile int cantidadVentas = 0;
    private volatile int totalRecaudado = 0;

    /**
     * Lo que realmente vendio cada vendedor. ConcurrentHashMap es seguro entre hilos,
     * asi sirve para comparar contra lo que la caja logro registrar.
     */
    private final Map<String, Integer> totalPorVendedor = new ConcurrentHashMap<>();

    /**
     * Version CON synchronized: solo un vendedor a la vez puede registrar,
     * los demas esperan su turno. Los contadores siempre quedan correctos.
     *
     * Por que es synchronized:
     * Los 3 vendedores (hilos) comparten la misma caja y modifican los mismos datos
     * (cantidadVentas y totalRecaudado). Registrar una venta son varios pasos:
     * leer el valor, calcular y guardar. synchronized pone un candado (monitor) sobre
     * la caja: el primer hilo que entra lo toma y los demas esperan hasta que lo suelte.
     *
     * Razon:
     * Evitar la condicion de carrera. Sin synchronized dos vendedores pueden leer el
     * mismo total al mismo tiempo, y el ultimo en guardar borra la venta del otro.
     * Con synchronized ninguna venta se pierde y la caja siempre cuadra con lo vendido.
     */
    public synchronized String registrarVentaSincronizada(String vendedor, String producto, int cantidad,
                                                          int precioUnitario) throws InterruptedException {
        return registrar(vendedor, producto, cantidad, precioUnitario);
    }

    /**
     * Version SIN synchronized: varios vendedores pueden registrar al mismo tiempo.
     * Si dos leen el mismo contador antes de que el otro lo guarde, una venta se pierde.
     */
    public String registrarVentaSinSincronizar(String vendedor, String producto, int cantidad,
                                               int precioUnitario) throws InterruptedException {
        return registrar(vendedor, producto, cantidad, precioUnitario);
    }

    /**
     * El mismo codigo para ambas versiones, la unica diferencia es synchronized.
     * Leer, esperar y luego escribir hace visible la condicion de carrera.
     */
    private String registrar(String vendedor, String producto, int cantidad, int precioUnitario)
            throws InterruptedException {
        int subtotal = cantidad * precioUnitario;
        totalPorVendedor.merge(vendedor, subtotal, Integer::sum);

        // 1. Leer los valores actuales de la caja
        int ventasLeidas = cantidadVentas;
        int totalLeido = totalRecaudado;

        /**
         * 2. Simular que la caja tarda 300 milisegundos en registrar la venta.
         * Sin synchronized, en este tiempo otro vendedor puede leer los mismos valores.
         */
        Thread.sleep(300);

        // 3. Guardar los valores nuevos (puede pisar lo que guardo otro vendedor)
        cantidadVentas = ventasLeidas + 1;
        totalRecaudado = totalLeido + subtotal;

        return "Venta #" + cantidadVentas + " | " + vendedor + " vendio "
                + cantidad + " x " + producto + " (C$ " + precioUnitario + ")"
                + " = C$ " + subtotal
                + " | Total en caja: C$ " + totalRecaudado;
    }

    public int getCantidadVentas() {
        return cantidadVentas;
    }

    public int getTotalRecaudado() {
        return totalRecaudado;
    }

    public Map<String, Integer> getTotalPorVendedor() {
        // Copia ordenada por nombre del vendedor
        return new TreeMap<>(totalPorVendedor);
    }

    /**
     * Suma de lo que realmente vendieron todos los vendedores.
     */
    public int getTotalReal() {
        return totalPorVendedor.values().stream().mapToInt(Integer::intValue).sum();
    }
}
