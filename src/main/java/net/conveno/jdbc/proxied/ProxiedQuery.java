package net.conveno.jdbc.proxied;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.response.ConvenoResponseExecutor;
import net.conveno.jdbc.response.Result;
import net.conveno.jdbc.util.StringParser;

import java.io.Serializable;
import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.sql.Statement;

@Getter
@FieldDefaults(makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class ProxiedQuery implements Cloneable, Serializable {

    private transient ProxiedConnection connection;
    private String sql;

    @NonFinal
    private transient Statement statement;

    void prepare()
    throws SQLException {

        if (statement == null) {
            statement = connection.getConnection().createStatement();
        }
    }

    void uncached()
    throws SQLException {

        if (statement != null) {
            statement.closeOnCompletion();
        }
    }

    ConvenoResponseExecutor wrapResponse(ProxiedRepository repository, Parameter[] parameters, Object[] initargs) {
        String sql = StringParser.parse(repository, this.sql, parameters, initargs);
        return () -> Result.of(0, statement.executeQuery(sql));
    }

    ConvenoResponseExecutor wrapGeneratedKeysResponse(ProxiedRepository repository, Parameter[] parameters, Object[] initargs) {
        String sql = StringParser.parse(repository, this.sql, parameters, initargs);
        return () -> Result.of(statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS), statement.getGeneratedKeys());
    }

    @Override
    public ProxiedQuery clone() {
        return new ProxiedQuery(connection, sql, statement);
    }
}
