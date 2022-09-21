package net.conveno.jdbc.test;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.conveno.jdbc.response.ConvenoResponseAdapter;
import net.conveno.jdbc.response.ConvenoResponseLine;

@Getter
@ToString
@EqualsAndHashCode
@FieldDefaults(makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class Userinfo implements ConvenoResponseAdapter<Userinfo> {

    @NonFinal
    private int id;

    private String name;
    private int age;

    @Override
    public Userinfo convert(ConvenoResponseLine responseLine) {
        int id = responseLine.getNullableInt("id");
        int age = responseLine.getInt("age").orElse(1);

        String name = responseLine.getNullableString("name");

        return new Userinfo(id, name, age);
    }
}
