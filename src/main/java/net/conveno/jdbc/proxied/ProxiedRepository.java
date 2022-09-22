package net.conveno.jdbc.proxied;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.*;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import net.conveno.jdbc.util.RepositoryValidator;
import net.conveno.jdbc.util.SneakySupplier;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@FieldDefaults(makeFinal = true)
public class ProxiedRepository implements MethodInterceptor {

    private static final ExecutorService THREADS_POOL_EXECUTOR = Executors.newCachedThreadPool();

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

    @SneakyThrows
    private <T> T execute(Method method, SneakySupplier<T> supplier) {
        if (RepositoryValidator.isAsynchronous(method)) {
            ConvenoAsynchronous asynchronous = method.getDeclaredAnnotation(ConvenoAsynchronous.class);

            if (asynchronous.onlySubmit()) {
                THREADS_POOL_EXECUTOR.submit(() -> SneakySupplier.sneakyGet(supplier));
                return null;
            }

            CompletableFuture<T> completableFuture = CompletableFuture.supplyAsync(() -> SneakySupplier.sneakyGet(supplier), THREADS_POOL_EXECUTOR);
            return asynchronous.join() ? completableFuture.join() : completableFuture.get();
        }

        return SneakySupplier.sneakyGet(supplier);
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

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) {

        Class<?> returnType = method.getReturnType();
        boolean isResponseAwait = RepositoryValidator.canResponseReturn(method);

        if (isResponseAwait && !List.class.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException("Method marked @ConvenoNonResponse, then he`s must be return void - " + method);
        }

        return execute(method, () -> {
            Object response = null;

            if (RepositoryValidator.isQuery(method)) {

                String sql = RepositoryValidator.toStringQuery(method);
                ConvenoResponseExecutor responseExecutor = toResponseExecutor(sql, method, args);

                if (isResponseAwait) {
                    response = new ConvenoResponse(connection.getUnsafe(), responseExecutor);
                } else {
                    responseExecutor.execute();
                }

            } else if (RepositoryValidator.isTransaction(method)) {

                ProxiedTransaction transaction = new ProxiedTransaction(connection, toTransactionQueries(method));
                response = transaction.executeQueries(this, method, args);

            } else {
                throw new IllegalArgumentException("Method is not marked @ConvenoQuery - " + method);
            }

            return isResponseAwait ? response : null;
        });
    }
}
