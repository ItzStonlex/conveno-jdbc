package net.conveno.jdbc.response;

public interface ConvenoResponseAdapter<T> {

    T convert(ConvenoResponseLine responseLine);
}
