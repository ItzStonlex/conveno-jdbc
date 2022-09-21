<div align="center">

![logo](/images/CONVENO-jdbc.png)

[![MIT License](https://img.shields.io/github/license/pl3xgaming/Purpur?&logo=github)](LICENSE)

---

# Conveno JDBC

Proxied JDBC connection based on HikariCP

</div>

---

## FEEDBACK

- My Discord Server: **[Link](https://discord.gg/GmT9pUy8af)**
- My VKontakte Page: **[Link](https://vk.com/itzstonlex)**

---

## HOW TO USE?

To start using it, we need to create a Repository<br>
that will perform the functionality of the CRUD manager<br> 
of our connection to the database using the JDBC protocol.

Using example:

```java
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
    ConvenoTransactionResponse reinsert(@ConvenoParam("user") Userinfo userinfo);
}
```

The next step is to initialize our repository<br>
and connect to the database driver.

For reference: At the bottom level, the library<br>
uses com.zaxxar:HikariCP for proxied connection to your<br>
database's JDBC driver.

Using example:

```java
ConvenoRouter router = ConvenoRouter.create();
JDBCRepositoryTest repository = router.getRepository(JDBCRepositoryTest.class);
```

---

_You can see more detailed usage and tests by clicking on 
<a href="https://github.com/ItzStonlex/conveno-jdbc/tree/main/src/test/java/net/conveno/jdbc/test">this link</a>._

---

## PLEASE, SUPPORT ME


By clicking on this link, you can support me as a 
developer and motivate me to develop new open-source projects!

<a href="https://www.buymeacoffee.com/itzstonlex" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 174px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a>
