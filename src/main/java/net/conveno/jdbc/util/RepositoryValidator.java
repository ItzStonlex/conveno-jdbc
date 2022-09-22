package net.conveno.jdbc.util;

import lombok.experimental.UtilityClass;
import net.conveno.jdbc.ConvenoAsynchronous;
import net.conveno.jdbc.ConvenoNonResponse;
import net.conveno.jdbc.ConvenoQuery;
import net.conveno.jdbc.ConvenoTransaction;

import java.lang.reflect.Method;
import java.util.*;

@UtilityClass
public class RepositoryValidator {

    private final byte ASYNC_MASK_FLAG = 0x01;

    private final byte QUERY_MASK_FLAG = 0x02;

    private final byte TRANS_MASK_FLAG = 0x04;

    private final byte RESP_MASK_FLAG = 0x08;

    private final Map<String, Byte> BITMASKS = new HashMap<>();

    private byte generateBitMask(Method method) {
        byte async = (method.isAnnotationPresent(ConvenoAsynchronous.class) ? ASYNC_MASK_FLAG : 0);
        byte query = (method.isAnnotationPresent(ConvenoQuery.class) ? QUERY_MASK_FLAG : 0);
        byte trans = (method.isAnnotationPresent(ConvenoTransaction.class) ? TRANS_MASK_FLAG : 0);
        byte resp = (!method.isAnnotationPresent(ConvenoNonResponse.class) ? RESP_MASK_FLAG : 0);

        return (byte) (async | query | trans | resp);
    }

    private boolean checkMask(Method method, byte flag) {
        String methodKey = method.toString().intern();

        if (!BITMASKS.containsKey(methodKey)) {
            BITMASKS.put(methodKey, generateBitMask(method));
        }

        return (BITMASKS.get(methodKey) & flag) == flag;
    }

    public boolean isAsynchronous(Method method) {
        return checkMask(method, ASYNC_MASK_FLAG);
    }

    public boolean isQuery(Method method) {
        return checkMask(method, QUERY_MASK_FLAG);
    }

    public boolean isTransaction(Method method) {
        return checkMask(method, TRANS_MASK_FLAG);
    }

    public boolean canResponseReturn(Method method) {
        return checkMask(method, RESP_MASK_FLAG);
    }

    public String toStringQuery(ConvenoQuery convenoQuery) {
        return convenoQuery.sql();
    }

    public String toStringQuery(Method method) {
        return toStringQuery(method.getDeclaredAnnotation(ConvenoQuery.class));
    }
}
