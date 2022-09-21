package net.conveno.jdbc.response;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ConvenoResponseExecutor {

    Result execute() throws SQLException;
}
