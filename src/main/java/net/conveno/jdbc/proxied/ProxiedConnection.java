package net.conveno.jdbc.proxied;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.conveno.jdbc.CacheScope;
import net.conveno.jdbc.ConvenoRouter;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import sun.misc.Unsafe;

import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
public class ProxiedConnection {

    private static final List<String> FETCH_PREFIXES = Arrays.asList("select", "show");

    private Unsafe unsafe;
    private Connection connection;

    private Map<String, ProxiedQuery> cache = new HashMap<>();

    public ProxiedQuery query(CacheScope scope, String sql) {

        if (scope == null) {
            return new ProxiedQuery(this, sql);
        }

        return scope.processGet(this, sql, cache);
    }

    ConvenoResponseExecutor execute(ProxiedQuery query, ProxiedRepository repository, Parameter[] parameters, Object[] initargs) {

        if (FETCH_PREFIXES.stream().anyMatch(prefix -> query.getSql().toLowerCase().startsWith(prefix))) {
            return query.wrapResponse(repository, parameters, initargs);
        }

        return query.wrapGeneratedKeysResponse(repository, parameters, initargs);
    }
}
