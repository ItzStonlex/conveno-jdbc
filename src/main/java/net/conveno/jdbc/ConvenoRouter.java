package net.conveno.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.conveno.jdbc.proxied.ProxiedConnection;
import net.conveno.jdbc.proxied.ProxiedRepository;
import net.conveno.jdbc.util.SneakyCatcher;
import net.conveno.jdbc.util.StringParser;
import sun.misc.Unsafe;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@FieldDefaults(makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConvenoRouter {

    private static Unsafe unsafe;

    @SneakyThrows
    private static void getUnsafe() {
        if (unsafe == null) {

            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);

            unsafe = ((Unsafe) field.get(null));
        }
    }

    public static ConvenoRouter create() {
        getUnsafe();
        return new ConvenoRouter();
    }

    private Map<Class<?>, Connection> repositoriesConnections = new ConcurrentHashMap<>();

    private Map<Class<?>, Object> repositoriesProxyInstances = new ConcurrentHashMap<>();

    private <T> T toProxy(ClassLoader classLoader, ProxiedRepository repositoryProxy) {
        @SuppressWarnings("unchecked") T proxy = (T) Proxy.newProxyInstance(classLoader,
                new Class[]{repositoryProxy.getSourceType()}, repositoryProxy);

        return proxy;
    }

    private <T> T createRepository(Class<T> repositoryType) {
        Connection connection = getSqlConnection(repositoryType);

        ProxiedConnection connectionProxy = new ProxiedConnection(unsafe, connection);
        ProxiedRepository repositoryProxy = new ProxiedRepository(connectionProxy, repositoryType);

        return toProxy(repositoryType.getClassLoader(), repositoryProxy);
    }

    public <T> T getRepository(Class<T> repositoryType) {
        if (repositoriesProxyInstances.containsKey(repositoryType)) {
            @SuppressWarnings("unchecked") T cached
                    = (T) repositoriesProxyInstances.get(repositoryType);

            return cached;
        }

        if (!repositoryType.isInterface() || !repositoryType.isAnnotationPresent(ConvenoRepository.class)) {
            throw new IllegalArgumentException("Repository interface must marked by @ConvenoRepository");
        }

        T newInstance = createRepository(repositoryType);
        repositoriesProxyInstances.put(repositoryType, newInstance);

        return newInstance;
    }

    private DataSource createDataSource(ConvenoRepository repositoryAnnotation) {
        HikariDataSource dataSource = new HikariDataSource();

        String sign = ("@");
        String[] data = StringParser.parseSystemProperties(
                repositoryAnnotation.jdbc()
                        + sign + repositoryAnnotation.username()
                        + sign + repositoryAnnotation.password()).split(sign);

        dataSource.setJdbcUrl(data[0]);

        dataSource.setUsername(data[1]);
        dataSource.setPassword(data[2]);

        return dataSource;
    }

    private Connection getSqlConnection(Class<?> repositoryType) {
        AtomicReference<Connection> reference = new AtomicReference<>(
                repositoriesConnections.get(repositoryType)
        );

        SneakyCatcher.sneakyThrows(() -> {
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

    public String getRepositoryTable(Class<?> repositoryType) {
        if (!repositoriesProxyInstances.containsKey(repositoryType)) {
            throw new NullPointerException("Repository " + repositoryType + " is not found");
        }

        if (repositoryType.isAnnotationPresent(ConvenoTable.class)) {
            return repositoryType.getDeclaredAnnotation(ConvenoTable.class).name();
        }

        return null;
    }
}
