package net.conveno.jdbc.util;

@FunctionalInterface
public interface SneakySupplier<T> {

    T get() throws Exception;

    static <T> T sneakyGet(SneakySupplier<T> supplier) {
        try {
            return supplier.get();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}