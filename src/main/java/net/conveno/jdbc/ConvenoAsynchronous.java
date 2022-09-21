package net.conveno.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConvenoAsynchronous {

    // TODO - 21.09.2022 - добавь сюда еще типа
    //  boolean join() default true;
    //  и если возвращает false, то не джоинить потоки
}
