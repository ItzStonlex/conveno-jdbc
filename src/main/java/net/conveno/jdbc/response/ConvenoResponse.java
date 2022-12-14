package net.conveno.jdbc.response;

import lombok.Getter;
import lombok.experimental.FieldDefaults;
import sun.misc.Unsafe;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true)
public class ConvenoResponse extends ArrayList<ConvenoResponseLine> {

    private Unsafe unsafe;

    @Getter
    private int affectedRows;

    public ConvenoResponse(Unsafe unsafe, ConvenoResponseExecutor executor)
    throws SQLException {

        this.unsafe = unsafe;

        Result result = executor.execute();
        this.affectedRows = result.getAffectedRows();

        addAll(result.getResultSet().getMetaData(), result.getResultSet());
    }

    private void addAll(ResultSetMetaData metadata, ResultSet executionResult)
    throws SQLException {

        int columns = metadata.getColumnCount();
        int index = 0;

        ConvenoResponseLine prev = null;

        while (executionResult.next()) {

            ConvenoResponseLine responseLine = new ConvenoResponseLine(index == 0, index == (columns - 1));
            injectResponseLine(responseLine, executionResult, metadata, columns);

            if (prev != null) {
                prev.setNext(responseLine);
            }

            super.add(responseLine);

            prev = responseLine;
            index++;
        }
    }

    private void injectResponseLine(ConvenoResponseLine responseLine, ResultSet executionResult, ResultSetMetaData metadata, int columns)
    throws SQLException {

        // create caches data.
        Map<String, Integer> indexByLabelsMap = new HashMap<>();
        Set<Integer> nullableIndexes = new HashSet<>();

        // init columns data.
        for (int columnIndex = 1; columnIndex <= columns; columnIndex++) {
            String name = metadata.getColumnName(columnIndex);

            indexByLabelsMap.put(name.toLowerCase(), columnIndex);

            if (metadata.isNullable(columnIndex) == ResultSetMetaData.columnNullable) {
                nullableIndexes.add(columnIndex);
            }

            responseLine.set(columnIndex, executionResult.getObject(columnIndex));
        }

        // set metadata values for response-line.
        responseLine.setIndexByLabelsMap(indexByLabelsMap);
        responseLine.setNullableIndexes(nullableIndexes);
    }

    private <T> T unsafeAllocate(Class<T> adaptiveType) {
        try {
            @SuppressWarnings("unchecked") T allocateInstance = (T) unsafe.allocateInstance(adaptiveType);
            return allocateInstance;
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public <R, T extends ConvenoResponseAdapter<R>> R toFirst(Class<T> adaptiveType) {
        if (!isEmpty()) {
            return unsafeAllocate(adaptiveType).convert(first());
        }

        return null;
    }

    public <R, T extends ConvenoResponseAdapter<R>> R toLast(Class<T> adaptiveType) {
        if (!isEmpty()) {
            return unsafeAllocate(adaptiveType).convert(last());
        }

        return null;
    }

    public <R, T extends ConvenoResponseAdapter<R>> List<R> toList(Class<T> adaptiveType) {
        return stream().map(responseLine -> unsafeAllocate(adaptiveType).convert(responseLine)).collect(Collectors.toList());
    }

    public <R, T extends ConvenoResponseAdapter<R>> List<R> toList(Class<T> adaptiveType, Predicate<R> filter) {
        return stream().map(responseLine -> unsafeAllocate(adaptiveType).convert(responseLine)).filter(filter).collect(Collectors.toList());
    }

    public <R, T extends ConvenoResponseAdapter<R>> List<R> toList(long limit, Class<T> adaptiveType) {
        return stream().limit(limit).map(responseLine -> unsafeAllocate(adaptiveType).convert(responseLine)).collect(Collectors.toList());
    }

    public <R, T extends ConvenoResponseAdapter<R>> List<R> toList(long limit, Class<T> adaptiveType, Predicate<R> filter) {
        return stream().limit(limit).map(responseLine -> unsafeAllocate(adaptiveType).convert(responseLine)).filter(filter).collect(Collectors.toList());
    }

    public <R, T extends ConvenoResponseAdapter<R>> List<R> toList(int limit, Class<T> adaptiveType) {
        return toList((long) limit, adaptiveType);
    }

    public ConvenoResponseLine first() {
        return stream().findFirst().orElse(null);
    }

    public ConvenoResponseLine last() {
        if (!isEmpty()) {
            return get(size() - 1);
        }

        return null;
    }

}
