package net.conveno.jdbc.util;

import lombok.NonNull;

@FunctionalInterface
public interface SneakyCatcher {

    void catching() throws Exception;

    static void sneakyThrows(@NonNull SneakyCatcher catcher) {
        try {
            catcher.catching();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}