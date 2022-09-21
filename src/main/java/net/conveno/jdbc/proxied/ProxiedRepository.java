package net.conveno.jdbc.proxied;

import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.ConvenoQuery;
import net.conveno.jdbc.ConvenoTable;
import net.conveno.jdbc.ConvenoTransaction;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import net.conveno.jdbc.util.RepositoryQueryParser;
import net.conveno.jdbc.util.RepositoryValidator;

import java.io.InvalidObjectException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

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

    private <T> T execute(Method method, SneakySupplier<T> supplier) {
        if (RepositoryValidator.isAsynchronous(method)) {
            return CompletableFuture.supplyAsync(() -> get(supplier), THREADS_POOL_EXECUTOR).join();
        }

        return get(supplier);
    }

    private ConvenoResponseExecutor toResponseExecutor(String sql)
    throws SQLException {

        ProxiedQuery proxiedQuery = connection.query(sql);
        proxiedQuery.prepare();

        return connection.execute(proxiedQuery);
    }

    private ProxiedQuery[] toTransactionQueries(Method method, Function<String, String> queryParsingFunction)
    throws SQLException {

        List<ProxiedQuery> transactionQueries = new ArrayList<>();

        ConvenoTransaction transactionAnnotation = method.getDeclaredAnnotation(ConvenoTransaction.class);

        for (ConvenoQuery queryAnnotation : transactionAnnotation.value()) {

            ProxiedQuery proxiedQuery = connection.query(queryParsingFunction.apply(queryAnnotation.sql()));
            proxiedQuery.prepare();

            transactionQueries.add(proxiedQuery);
        }

        return transactionQueries.toArray(new ProxiedQuery[0]);
    }

    @SuppressWarnings("SuspiciousInvocationHandlerImplementation")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();

        Class<?> returnType = method.getReturnType();
        boolean isResponseAwait = List.class.isAssignableFrom(returnType);

        return execute(method, () -> {

            List<ConvenoResponse> response = new ArrayList<>();

            if (RepositoryValidator.isQuery(method)) {
                ConvenoResponseExecutor responseExecutor = toResponseExecutor(
                        RepositoryQueryParser.parse(this, RepositoryValidator.toStringQuery(method), parameters, args)
                );

                if (isResponseAwait) {
                    response.add(new ConvenoResponse(connection.getRouter(), responseExecutor));
                }
                else {
                    responseExecutor.execute();
                }

            } else {

                if (RepositoryValidator.isTransaction(method)) {
                    ProxiedTransaction transaction = new ProxiedTransaction(connection, toTransactionQueries(method,
                            sql -> RepositoryQueryParser.parse(this, sql, parameters, args)));

                    response = transaction.executeQueries();

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
