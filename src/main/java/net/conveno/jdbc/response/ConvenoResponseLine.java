package net.conveno.jdbc.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConvenoResponseLine extends LinkedHashMap<Integer, Object> implements Cloneable {

    private static final Supplier<NoSuchElementException> NO_VALUE_PRESENT_SUPPLIER = (() -> new NoSuchElementException("no value present"));

    @Getter
    boolean firstLine, lastLine;

    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    ConvenoResponseLine next;

    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    Set<Integer> nullableIndexes;

    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    Map<String, Integer> indexByLabelsMap;

    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    Map<Integer, String> labelByIndexesMap;

    @NonFinal
    int currentIndex;

    public ConvenoResponseLine nextLine() {
        return next;
    }

    public int nextIndex() {
        if (size() > currentIndex) {
            currentIndex++;
        }

        return currentIndex;
    }

    public int findIndex(@NonNull String label) {
        return indexByLabelsMap.getOrDefault(label.toLowerCase(), -1);
    }

    public String findLabel(int index) {
        return labelByIndexesMap.get(index);
    }

    // *------------------------------------------------- * //

    public Set<Integer> getIndexes() {
        return super.keySet();
    }

    public Set<String> getLabels() {
        return getIndexes().stream().map(this::findLabel).collect(Collectors.toSet());
    }

    // *------------------------------------------------- * //
    public boolean contains(int index) {
        return containsKey(index);
    }

    public boolean contains(@NonNull String label) {
        return containsKey(findIndex(label));
    }

    public boolean isNullable(int index) {
        return nullableIndexes.contains(index);
    }

    public boolean isNullable(@NonNull String label) {
        return isNullable(findIndex(label));
    }

    public void set(int index, @NonNull Object value) {
        super.put(index, value);
    }

    public void set(@NonNull String label, @NonNull Object value) {
        super.put(findIndex(label), value);
    }

    private Optional<Object> lookup(int index) {
        if (index <= 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(get(index));
    }

    private <T> Optional<T> lookup(int index, Class<T> cls) {
        return lookup(index).map(cls::cast);
    }

    private <T> Optional<T> lookup(String label, Class<T> cls) {
        return lookup(findIndex(label), cls);
    }

    public Optional<Object> getObject(int index) {
        return lookup(index, Object.class);
    }

    public Optional<Object> getObject(@NonNull String label) {
        return lookup(label, Object.class);
    }

    public Optional<String> getString(int index) {
        return lookup(index, String.class);
    }

    public Optional<String> getString(@NonNull String label) {
        return lookup(label, String.class);
    }

    public Optional<Boolean> getBoolean(int index) {
        Object object = get(index);

        boolean returnValue = false;

        if (object instanceof Boolean) {
            returnValue = (boolean) object;
        }
        else if (object instanceof Number) {
            returnValue = ((Number) object).byteValue() == 1;
        }
        else if (object instanceof String) {
            returnValue = object.equals("true");
        }

        return Optional.of(returnValue);
    }

    public Optional<Boolean> getBoolean(@NonNull String label) {
        return getBoolean(findIndex(label));
    }

    public Optional<Long> getLong(int index) {
        return lookup(index, Number.class).map(Number::longValue);
    }

    public Optional<Long> getLong(@NonNull String label) {
        return lookup(label, Number.class).map(Number::longValue);
    }

    public Optional<Integer> getInt(int index) {
        return lookup(index, Number.class).map(Number::intValue);
    }

    public Optional<Integer> getInt(@NonNull String label) {
        return lookup(label, Number.class).map(Number::intValue);
    }

    public Optional<Double> getDouble(int index) {
        return lookup(index, Number.class).map(Number::doubleValue);
    }

    public Optional<Double> getDouble(@NonNull String label) {
        return lookup(label, Number.class).map(Number::doubleValue);
    }

    public Optional<Float> getFloat(int index) {
        return lookup(index, Number.class).map(Number::floatValue);
    }

    public Optional<Float> getFloat(@NonNull String label) {
        return lookup(label, Number.class).map(Number::floatValue);
    }

    public Optional<Short> getShort(int index) {
        return lookup(index, Number.class).map(Number::shortValue);
    }

    public Optional<Short> getShort(@NonNull String label) {
        return lookup(label, Number.class).map(Number::shortValue);
    }

    public Optional<Byte> getByte(int index) {
        return lookup(index, Number.class).map(Number::byteValue);
    }

    public Optional<Byte> getByte(@NonNull String label) {
        return lookup(label, Number.class).map(Number::byteValue);
    }

    public Optional<Date> getDate(int index) {
        Object object = get(index);

        Date returnValue = null;

        if (object instanceof Date) {
            returnValue = (Date) object;
        }
        else if (object instanceof Long) {
            returnValue = new Date((Long) object);
        }
        else if (object instanceof String) {
            try {
                returnValue = new Date(Long.parseLong(object.toString()));
            }
            catch (NumberFormatException exception) {
                returnValue = Date.valueOf(object.toString());
            }
        }

        return Optional.ofNullable(returnValue);
    }

    public Optional<Date> getDate(@NonNull String label) {
        return getDate(findIndex(label));
    }

    public Optional<Time> getTime(int index) {
        Object object = get(index);

        Time returnValue = null;

        if (object instanceof Time) {
            returnValue = (Time) object;
        }
        else if (object instanceof Long) {
            returnValue = new Time((Long) object);
        }
        else if (object instanceof String) {
            try {
                returnValue = new Time(Long.parseLong(object.toString()));
            }
            catch (NumberFormatException exception) {
                returnValue = Time.valueOf(object.toString());
            }
        }

        return Optional.ofNullable(returnValue);
    }

    public Optional<Time> getTime(@NonNull String label) {
        return getTime(findIndex(label));
    }

    public Optional<Timestamp> getTimestamp(int index) {
        Object object = get(index);

        Timestamp returnValue = null;

        if (object instanceof Timestamp) {
            returnValue = (Timestamp) object;
        }
        else if (object instanceof Long) {
            returnValue = new Timestamp((Long) object);
        }
        else if (object instanceof String) {
            try {
                returnValue = new Timestamp(Long.parseLong(object.toString()));
            }
            catch (NumberFormatException exception) {
                returnValue = Timestamp.valueOf(object.toString());
            }
        }

        return Optional.ofNullable(returnValue);
    }

    public Optional<Timestamp> getTimestamp(@NonNull String label) {
        return getTimestamp(findIndex(label));
    }

    public Optional<byte[]> getBlob(int index) {
        return lookup(index, byte[].class);
    }

    public Optional<byte[]> getBlob(@NonNull String label) {
        return lookup(label, byte[].class);
    }

    // *------------------------------------------------- * //

    public Object getNullableObject(int index) {
        return getObject(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Object getNullableObject(@NonNull String label) {
        return getObject(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public String getNullableString(int index) {
        return getString(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public String getNullableString(@NonNull String label) {
        return getString(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Boolean getNullableBoolean(int index) {
        return getBoolean(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Boolean getNullableBoolean(@NonNull String label) {
        return getBoolean(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Long getNullableLong(int index) {
        return getLong(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Long getNullableLong(@NonNull String label) {
        return getLong(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Integer getNullableInt(int index) {
        return getInt(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Integer getNullableInt(@NonNull String label) {
        return getInt(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Double getNullableDouble(int index) {
        return getDouble(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Double getNullableDouble(@NonNull String label) {
        return getDouble(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Float getNullableFloat(int index) {
        return getFloat(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Float getNullableFloat(@NonNull String label) {
        return getFloat(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Short getNullableShort(int index) {
        return getShort(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Short getNullableShort(@NonNull String label) {
        return getShort(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Byte getNullableByte(int index) {
        return getByte(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Byte getNullableByte(@NonNull String label) {
        return getByte(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Date getNullableDate(int index) {
        return getDate(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Date getNullableDate(@NonNull String label) {
        return getDate(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Time getNullableTime(int index) {
        return getTime(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Time getNullableTime(@NonNull String label) {
        return getTime(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Timestamp getNullableTimestamp(int index) {
        return getTimestamp(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Timestamp getNullableTimestamp(@NonNull String label) {
        return getTimestamp(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public byte[] getNullableBlob(int index) {
        return getBlob(index).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public byte[] getNullableBlob(@NonNull String label) {
        return getBlob(label).orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    // *------------------------------------------------- * //

    public Optional<Object> nextObject() {
        return getObject(nextIndex());
    }

    public Optional<String> nextString() {
        int current = nextIndex();
        return contains(current) ? getString(current) : Optional.empty();
    }

    public Optional<Boolean> nextBoolean() {
        int current = nextIndex();
        return contains(current) ? getBoolean(current) : Optional.empty();
    }

    public Optional<Long> nextLong() {
        int current = nextIndex();
        return contains(current) ? getLong(current) : Optional.empty();
    }

    public Optional<Integer> nextInt() {
        int current = nextIndex();
        return contains(current) ? getInt(current) : Optional.empty();
    }

    public Optional<Double> nextDouble() {
        int current = nextIndex();
        return contains(current) ? getDouble(current) : Optional.empty();
    }

    public Optional<Float> nextFloat() {
        int current = nextIndex();
        return contains(current) ? getFloat(current) : Optional.empty();
    }

    public Optional<Short> nextShort() {
        int current = nextIndex();
        return contains(current) ? getShort(current) : Optional.empty();
    }

    public Optional<Byte> nextByte() {
        int current = nextIndex();
        return contains(current) ? getByte(current) : Optional.empty();
    }

    public Optional<Date> nextDate() {
        int current = nextIndex();
        return contains(current) ? getDate(current) : Optional.empty();
    }

    public Optional<Time> nextTime() {
        int current = nextIndex();
        return contains(current) ? getTime(current) : Optional.empty();
    }

    public Optional<Timestamp> nextTimestamp() {
        int current = nextIndex();
        return contains(current) ? getTimestamp(current) : Optional.empty();
    }

    public Optional<byte[]> nextBlob() {
        int current = nextIndex();
        return contains(current) ? getBlob(current) : Optional.empty();
    }

    // *------------------------------------------------- * //

    public Object nextNullableObject() {
        return nextObject().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public String nextNullableString() {
        return nextString().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Boolean nextNullableBoolean() {
        return nextBoolean().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Long nextNullableLong() {
        return nextLong().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Integer nextNullableInt() {
        return nextInt().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Double nextNullableDouble() {
        return nextDouble().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Float nextNullableFloat() {
        return nextFloat().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Short nextNullableShort() {
        return nextShort().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Byte nextNullableByte() {
        return nextByte().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Date nextNullableDate() {
        return nextDate().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Time nextNullableTime() {
        return nextTime().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public Timestamp nextNullableTimestamp() {
        return nextTimestamp().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    public byte[] nextNullableBlob() {
        return nextBlob().orElseThrow(NO_VALUE_PRESENT_SUPPLIER);
    }

    @Override
    public ConvenoResponseLine clone() {
        return (ConvenoResponseLine) super.clone();
    }
}
