package net.conveno.jdbc;

import net.conveno.jdbc.proxied.ProxiedConnection;
import net.conveno.jdbc.proxied.ProxiedQuery;

import java.util.Map;

public enum CacheScope {

    SINGLETON {

        @Override
        public ProxiedQuery processGet(ProxiedConnection connection, String sql, Map<String, ProxiedQuery> map) {
            return map.computeIfAbsent(sql, __ -> new ProxiedQuery(connection, sql));
        }
    },

    PROTOTYPE {

        @Override
        public ProxiedQuery processGet(ProxiedConnection connection, String sql, Map<String, ProxiedQuery> map) {
            return map.computeIfAbsent(sql, __ -> new ProxiedQuery(connection, sql)).clone();
        }
    },
    ;

    public abstract ProxiedQuery processGet(ProxiedConnection connection, String sql, Map<String, ProxiedQuery> map);
}
