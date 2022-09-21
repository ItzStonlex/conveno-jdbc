package net.conveno.jdbc.util;

import lombok.experimental.UtilityClass;
import net.conveno.jdbc.ConvenoAsynchronous;
import net.conveno.jdbc.ConvenoQuery;
import net.conveno.jdbc.ConvenoTransaction;

import java.lang.reflect.Method;

@UtilityClass
public class RepositoryValidator {

    public boolean isAsynchronous(Method method) {
        return method.isAnnotationPresent(ConvenoAsynchronous.class);
    }

    public boolean isQuery(Method method) {
        return method.isAnnotationPresent(ConvenoQuery.class);
    }

    public boolean isTransaction(Method method) {
        return method.isAnnotationPresent(ConvenoTransaction.class);
    }

    public String toStringQuery(ConvenoQuery convenoQuery) {
        return convenoQuery.sql();
    }

    public String toStringQuery(Method method) {
        return toStringQuery(method.getDeclaredAnnotation(ConvenoQuery.class));
    }
}
