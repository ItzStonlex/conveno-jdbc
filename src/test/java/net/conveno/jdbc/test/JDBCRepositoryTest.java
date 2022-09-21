package net.conveno.jdbc.test;

import net.conveno.jdbc.*;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoTransactionResponse;

@ConvenoTable(name = "users")
@ConvenoRepository(jdbc = "jdbc:h2:mem:default",
        username = "root",
        password = "password")
public interface JDBCRepositoryTest {

    @ConvenoAsynchronous
    @ConvenoQuery(sql = "create table if not exists ${table} (" +
            "id int not null primary key auto_increment, " +
            "name varchar not null, " +
            "age int not null)")
    void createTable();

    @ConvenoQuery(sql = "show tables")
    ConvenoResponse getTablesResponse();

    @ConvenoQuery(sql = "select * from ${table} limit ${limit}")
    ConvenoResponse getUsersList(@ConvenoParam("limit") int limit);

    @ConvenoAsynchronous
    @ConvenoQuery(sql = "insert into ${table} (name, age) values (${user}.$name, ${user}.$age)")
    ConvenoResponse insert(@ConvenoParam("user") Userinfo userinfo);

    @ConvenoAsynchronous
    @ConvenoTransaction({
            @ConvenoQuery(sql = "delete from ${table} where name=${user}.$name and age=${user}.$age"),
            @ConvenoQuery(sql = "insert into ${table} (name, age) values (${user}.$name, ${user}.$age)"),
    })
    ConvenoTransactionResponse reinsert(@ConvenoParam("user") Userinfo userinfo);
}
