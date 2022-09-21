package net.conveno.jdbc.proxied;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.conveno.jdbc.ConvenoRouter;
import net.conveno.jdbc.response.ConvenoResponseExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
public class ProxiedConnection {

    private static final List<String> FETCH_PREFIXES = Arrays.asList("select", "show");

    private ConvenoRouter router;
    private Connection connection;

    private Map<String, ProxiedQuery> cache = new IdentityHashMap<>();

    public ProxiedQuery query(String sql) {
        return cache.computeIfAbsent(sql, __ -> new ProxiedQuery(this, sql.intern()));
    }

    ConvenoResponseExecutor execute(ProxiedQuery query, Object... initargs)
    throws SQLException {

        ProxiedQuery clone = query.clone();

        clone.setArguments(initargs);

        if (FETCH_PREFIXES.stream().anyMatch(prefix -> query.getSql().toLowerCase().startsWith(prefix))) {
            return clone.wrapResponse();
        }

        return clone.wrapGeneratedKeysResponse();
    }
}
