package net.conveno.jdbc.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.conveno.jdbc.ConvenoParam;
import net.conveno.jdbc.proxied.ProxiedRepository;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Properties;
import java.util.Set;

@UtilityClass
public class StringParser {

    private final String QUESTION_CHAR = "?";
    private final String STRING_PARAMETER_FORMAT = "'%s'";
    private final String REPLACEMENT_PARAMETER_FORMAT = "${%s}";

    private final String VARIABLE_SPLITTER = ".$";

    private String toString(Object value) {
        if (value == QUESTION_CHAR) {
            return QUESTION_CHAR;
        }

        return value instanceof Number ? value.toString() : String.format(STRING_PARAMETER_FORMAT, value);
    }

    private String setTable(ProxiedRepository repository, String sql) {
        String table = repository.getTable();

        if (table != null) {
            return sql.replace(String.format(REPLACEMENT_PARAMETER_FORMAT, "table"), table);
        }

        return sql;
    }

    @SneakyThrows
    private String setParam(String sql, String name, Object value) {
        String basePlaceholder = String.format(REPLACEMENT_PARAMETER_FORMAT, name);

        // check variables using contains.
        if (sql.contains(basePlaceholder + VARIABLE_SPLITTER)) {

            for (Field field : value.getClass().getDeclaredFields()) {
                String placeholder = basePlaceholder + VARIABLE_SPLITTER + field.getName();

                field.setAccessible(true);
                sql = sql.replace(placeholder, toString(field.get(value)));
            }
        }

        return sql.replace(basePlaceholder, toString(value));
    }

    public String parse(ProxiedRepository repository, String sql, Parameter[] parameters, Object[] initargs) {
        Class<ConvenoParam> convenoParamType = ConvenoParam.class;

        for (int idx = 0; idx < parameters.length; idx++) {
            Parameter parameter = parameters[idx];

            if (!parameter.isAnnotationPresent(convenoParamType)) {
                throw new IncompleteAnnotationException(convenoParamType, parameter.getName());
            }

            String name = parameter.getDeclaredAnnotation(convenoParamType).value();
            sql = setParam(sql, name, initargs[idx]);
        }

        return parseSystemProperties(setTable(repository, sql));
    }

    public String parseSystemProperties(String string) {
        Properties properties = System.getProperties();

        Set<String> keys = properties.stringPropertyNames();

        for (String key : keys) {
            string = string.replace(String.format(REPLACEMENT_PARAMETER_FORMAT, "system." + key), properties.getProperty(key));
        }

        return string;
    }

}
