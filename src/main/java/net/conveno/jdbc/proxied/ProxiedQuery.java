package net.conveno.jdbc.proxied;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import net.conveno.jdbc.response.Result;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

@Getter
@FieldDefaults(makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class ProxiedQuery implements Closeable, Cloneable, Serializable {

    private transient ProxiedConnection connection;
    private String sql;

    @NonFinal
    private transient PreparedStatement preparedStatement;

    void prepare()
    throws SQLException {

        if (preparedStatement == null) {
            preparedStatement = connection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
    }

    void setArguments(Object[] initargs)
    throws SQLException {

        preparedStatement.clearParameters();

        int idx = 1;
        for (Object argument : initargs) {

            if (argument == null) {
                preparedStatement.setNull(idx, Types.NULL);
            }
            else {
                preparedStatement.setObject(idx, argument);
            }

            idx++;
        }
    }

    ConvenoResponseExecutor wrapResponse() {
        return () -> Result.of(0, preparedStatement.executeQuery());
    }

    ConvenoResponseExecutor wrapGeneratedKeysResponse() {
        return () -> Result.of(preparedStatement.executeUpdate(), preparedStatement.getGeneratedKeys());
    }

    @SneakyThrows
    @Override
    public void close() throws IOException {
        preparedStatement.close();
    }

    @Override
    public ProxiedQuery clone() {
        return new ProxiedQuery(connection, sql, preparedStatement);
    }
}
