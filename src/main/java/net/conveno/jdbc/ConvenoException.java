package net.conveno.jdbc;

import java.lang.reflect.Method;

public class ConvenoException extends RuntimeException {

    public ConvenoException(Method method, String message) {
        super(message + " - " + method);
    }
}
