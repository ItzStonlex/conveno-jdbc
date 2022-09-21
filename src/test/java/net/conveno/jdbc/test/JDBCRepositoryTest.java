package net.conveno.jdbc.test;

import net.conveno.jdbc.*;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoTransactionResponse;

@ConvenoTable(name = "users")
@ConvenoRepository(jdbc = "jdbc:h2:mem:default",
        username = "root",
        password = "password")
public interface JDBCRepositoryTest {

    /**
     * Request to create a table with parameters:
     * 1. `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT;
     * 2. `name` VARCHAR NOT NULL;
     * 3. `age` INT NOT NULL.
     *
     * Marked as asynchronous.
     * {@link ConvenoAsynchronous}
     */
    @ConvenoAsynchronous
    @ConvenoQuery(sql = "create table if not exists ${table} (" +
            "id int not null primary key auto_increment, " +
            "name varchar not null, " +
            "age int not null)")
    void createTable();

    /**
     * Getting a tables list.
     * @return - A response that contains a list
     *          of strings with the names of tables and their schemas.
     */
    @ConvenoCaching(scope = CacheScope.SINGLETON)
    @ConvenoQuery(sql = "show tables")
    ConvenoResponse getTablesResponse();

    /**
     * Search for all users entered
     * the table with a list limit.
     *
     * @param limit - Users list limit.
     * @return - A response that contains a list
     *          of users information labels.
     */
    @ConvenoCaching(scope = CacheScope.PROTOTYPE)
    @ConvenoQuery(sql = "select * from ${table} limit ${limit}")
    ConvenoResponse getUsersList(@ConvenoParam("limit") int limit);

    /**
     * Request to add a new user line to the
     * user table.
     *
     * Marked as asynchronous.
     * {@link ConvenoAsynchronous}
     *
     * @param userinfo - User information to add.
     * @return - A response that contains an inserted
     *          user generated key.
     */
    @ConvenoAsynchronous
    @ConvenoCaching(scope = CacheScope.PROTOTYPE)
    @ConvenoQuery(sql = "insert into ${table} (name, age) values (${user}.$name, ${user}.$age)")
    ConvenoResponse insert(@ConvenoParam("user") Userinfo userinfo);

    /**
     * Transaction to delete and add a new user line to the
     * user table.
     *
     * Marked as asynchronous.
     * {@link ConvenoAsynchronous}
     *
     * @param userinfo - User information to reinsert.
     * @return - A response that contains an inserted
     *          user generated key.
     */
    @ConvenoAsynchronous
    @ConvenoTransaction({
            @ConvenoQuery(sql = "delete from ${table} where name=${user}.$name and age=${user}.$age"),
            @ConvenoQuery(sql = "insert into ${table} (name, age) values (${user}.$name, ${user}.$age)"),
    })
    @ConvenoCaching(scope = CacheScope.PROTOTYPE)
    ConvenoTransactionResponse reinsert(@ConvenoParam("user") Userinfo userinfo);
}
