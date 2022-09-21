package net.conveno.jdbc.response;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.sql.ResultSet;

@Data(staticConstructor = "of")
@FieldDefaults(makeFinal = true)
public class Result {

    private int affectedRows;

    private ResultSet resultSet;
}
