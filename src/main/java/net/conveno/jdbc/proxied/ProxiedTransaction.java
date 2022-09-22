package net.conveno.jdbc.proxied;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import net.conveno.jdbc.response.ConvenoTransactionResponse;

import java.lang.reflect.Method;
import java.sql.SQLException;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
public class ProxiedTransaction {

    private ProxiedConnection connection;
    private ProxiedQuery[] proxiedQueries;

    @NonFinal
    private boolean canCommit;

    void begin() throws SQLException {

        canCommit = true;
        connection.getConnection().setAutoCommit(!canCommit);
    }

    void end() throws SQLException {

        canCommit = false;
        connection.getConnection().setAutoCommit(!canCommit);
    }

    void rollback() throws SQLException {

        canCommit = false;
        connection.getConnection().rollback();
    }

    void commit() throws SQLException {

        if (canCommit) {
            connection.getConnection().commit();
        }
    }

    public synchronized ConvenoTransactionResponse executeQueries(ProxiedRepository repository, Method method, Object[] args)
    throws SQLException {

        ConvenoTransactionResponse transactionResponse = new ConvenoTransactionResponse();
        begin();

        for (ProxiedQuery proxiedQuery : proxiedQueries) {
            try {
                ConvenoResponseExecutor executor = connection.execute(proxiedQuery, repository, method.getParameters(), args);
                transactionResponse.add(
                        new ConvenoResponse(connection.getRouter(), executor)
                );
            }
            catch (Exception exception) {
                rollback();
                exception.printStackTrace();

                break;
            }
        }

        commit();
        end();

        return transactionResponse;
    }
}
