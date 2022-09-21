package net.conveno.jdbc.test;

import net.conveno.jdbc.ConvenoRouter;
import net.conveno.jdbc.response.ConvenoResponse;
import net.conveno.jdbc.response.ConvenoResponseLine;
import net.conveno.jdbc.response.ConvenoTransactionResponse;

import java.util.List;

public class BootstrapTest {

    private static void printTables(JDBCRepositoryTest repository) {
        ConvenoResponse tablesResponse = repository.getTablesResponse();

        System.out.println("Tables size: " + tablesResponse.size());

        for (ConvenoResponseLine responseLine : tablesResponse) {
            System.out.println("Found a table: " + responseLine.getNullableString("table_name"));
        }
    }

    public static void main(String[] args) {
        ConvenoRouter convenoRouter = ConvenoRouter.create();
        JDBCRepositoryTest repository = convenoRouter.getRepository(JDBCRepositoryTest.class);

        // Create a table.
        repository.createTable();

        // Print available database tables.
        printTables(repository);

        // Test insert queries.
        repository.insert(new Userinfo("David", 20));
        repository.insert(new Userinfo("Mark", 10));
        repository.insert(new Userinfo("Johan", 32));

        // Test insert query response data.
        Userinfo misha = new Userinfo("Misha", 18);
        ConvenoResponse insert = repository.insert(misha);

        int userID = insert.first().getNullableInt(1);
        System.out.println("Misha User ID - " + userID);

        // Get list of users by internal Userinfo conveno-adapter.
        List<Userinfo> usersList = repository.getUsersList(3)
                .toList(Userinfo.class);

        System.out.println(usersList);

        // Reinsert misha user.
        ConvenoTransactionResponse reinsert = repository.reinsert(misha);

        for (ConvenoResponse response : reinsert) {
            for (ConvenoResponseLine responseLine : response) {
                System.out.println(responseLine.getNullableInt(1));
            }
        }
    }
}
