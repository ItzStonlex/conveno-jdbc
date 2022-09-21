package net.conveno.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.conveno.jdbc.proxied.ProxiedConnection;
import net.conveno.jdbc.proxied.ProxiedRepository;
import sun.misc.Unsafe;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@FieldDefaults(makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConvenoRouter {

    @FunctionalInterface
    private interface SQLExceptionHandler {

        void catching() throws SQLException;
    }

    @SneakyThrows
    public static ConvenoRouter create() {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);

        Unsafe unsafe = ((Unsafe) field.get(null));

        return new ConvenoRouter(unsafe);
    }

    private Map<Class<?>, Connection> repositoriesConnections = new ConcurrentHashMap<>();

    @Getter
    private Unsafe unsafe;

    private <T> T toProxy(ClassLoader classLoader, ProxiedRepository repositoryProxy) {
        @SuppressWarnings("unchecked") T proxy = (T) Proxy.newProxyInstance(classLoader,
                new Class[]{repositoryProxy.getSourceType()}, repositoryProxy);

        return proxy;
    }

    public <T> T getRepository(Class<T> repositoryType) {
        if (!repositoryType.isInterface() || !repositoryType.isAnnotationPresent(ConvenoRepository.class)) {
            throw new IllegalArgumentException("Repository interface must marked by @ConvenoRepository");
        }

        Connection connection = getSqlConnection(repositoryType);

        ProxiedConnection connectionProxy = new ProxiedConnection(this, connection);
        ProxiedRepository repositoryProxy = new ProxiedRepository(connectionProxy, repositoryType);

        return toProxy(repositoryType.getClassLoader(), repositoryProxy);
    }

    private void sneakyThrows(SQLExceptionHandler exceptionHandler) {
        try {
            exceptionHandler.catching();
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private DataSource createDataSource(ConvenoRepository repositoryAnnotation) {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(repositoryAnnotation.jdbc());

        dataSource.setUsername(repositoryAnnotation.username());
        dataSource.setPassword(repositoryAnnotation.password());

        return dataSource;
    }

    private Connection getSqlConnection(Class<?> repositoryType) {
        AtomicReference<Connection> reference = new AtomicReference<>(
                repositoriesConnections.get(repositoryType)
        );

        sneakyThrows(() -> {
            Connection connection = reference.get();

            if (connection == null || !connection.isValid(1000)) {

                DataSource dataSource = createDataSource(repositoryType.getDeclaredAnnotation(ConvenoRepository.class));
                reference.set(dataSource.getConnection());
            }
        });

        Connection result = reference.get();
        repositoriesConnections.put(repositoryType, result);

        return result;
    }
}
