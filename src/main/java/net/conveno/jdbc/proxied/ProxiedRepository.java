package net.conveno.jdbc.proxied;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.*;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import net.conveno.jdbc.util.RepositoryValidator;

import java.io.InvalidObjectException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@FieldDefaults(makeFinal = true)
public class ProxiedRepository implements InvocationHandler {

    private static final ExecutorService THREADS_POOL_EXECUTOR = Executors.newCachedThreadPool();

    private interface SneakySupplier<T> {

        T get() throws Exception;
    }

    private ProxiedConnection connection;

    @Getter
    private Class<?> sourceType;

    @NonFinal
    @Getter
    private String table;

    public ProxiedRepository(ProxiedConnection connection, Class<?> sourceType) {
        this.connection = connection;
        this.sourceType = sourceType;

        if (sourceType.isAnnotationPresent(ConvenoTable.class)) {
            this.table = sourceType.getDeclaredAnnotation(ConvenoTable.class).name();
        }
    }

    private <T> T get(SneakySupplier<T> supplier) {
        try {
            return supplier.get();
        }
        catch (Exception exception) {
            exception.printStackTrace();

            return null;
        }
    }

    @SneakyThrows
    private <T> T execute(Method method, SneakySupplier<T> supplier) {
        if (RepositoryValidator.isAsynchronous(method)) {

            ConvenoAsynchronous asynchronous = method.getDeclaredAnnotation(ConvenoAsynchronous.class);

            if (asynchronous.onlySubmit()) {
                THREADS_POOL_EXECUTOR.submit(() -> get(supplier));
            }

            CompletableFuture<T> completableFuture = CompletableFuture.supplyAsync(() -> get(supplier), THREADS_POOL_EXECUTOR);
            return asynchronous.join() ? completableFuture.join() : completableFuture.get();
        }

        return get(supplier);
    }

    private CacheScope getCacheScope(Method method) {
        ConvenoCaching caching = method.getDeclaredAnnotation(ConvenoCaching.class);
        return caching != null ? caching.scope() : null;
    }

    private ConvenoResponseExecutor toResponseExecutor(String sql, Method method, Object[] args)
    throws SQLException {

        CacheScope cacheScope = getCacheScope(method);

        ProxiedQuery proxiedQuery = connection.query(cacheScope, sql);
        proxiedQuery.prepare();

        if (cacheScope == null) {
            proxiedQuery.uncached();
        }

        return connection.execute(proxiedQuery, this, method.getParameters(), args);
    }

    private ProxiedQuery[] toTransactionQueries(Method method)
    throws SQLException {

        List<ProxiedQuery> transactionQueries = new ArrayList<>();

        ConvenoTransaction transactionAnnotation = method.getDeclaredAnnotation(ConvenoTransaction.class);
        CacheScope cacheScope = getCacheScope(method);

        for (ConvenoQuery queryAnnotation : transactionAnnotation.value()) {

            ProxiedQuery proxiedQuery = connection.query(cacheScope, queryAnnotation.sql());
            proxiedQuery.prepare();

            if (cacheScope == null) {
                proxiedQuery.uncached();
            }

            transactionQueries.add(proxiedQuery);
        }

        return transactionQueries.toArray(new ProxiedQuery[0]);
    }

    @SuppressWarnings("SuspiciousInvocationHandlerImplementation")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        Class<?> returnType = method.getReturnType();
        boolean isResponseAwait = RepositoryValidator.canResponseReturn(method);

        if (isResponseAwait && !List.class.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException("Method marked @ConvenoNonResponse, then he`s must be return void - " + method);
        }

        return execute(method, () -> {
            List<ConvenoResponse> response = new ArrayList<>();

            if (RepositoryValidator.isQuery(method)) {

                String sql = RepositoryValidator.toStringQuery(method);
                ConvenoResponseExecutor responseExecutor = toResponseExecutor(sql, method, args);

                if (isResponseAwait) {
                    response.add(new ConvenoResponse(connection.getRouter(), responseExecutor));
                }
                else {
                    responseExecutor.execute();
                }

            } else {

                if (RepositoryValidator.isTransaction(method)) {
                    ProxiedTransaction transaction = new ProxiedTransaction(connection, toTransactionQueries(method));

                    response = transaction.executeQueries(this, method, args);

                } else {
                    throw new InvalidObjectException(method.toString());
                }
            }

            if (isResponseAwait) {
                if (!response.isEmpty() && returnType.isAssignableFrom(ConvenoResponse.class)) {
                    return response.get(0);
                }

                return response;
            }

            return null;
        });
    }
}
