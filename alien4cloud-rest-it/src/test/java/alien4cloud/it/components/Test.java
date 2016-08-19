package alien4cloud.it.components;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class Test {

    public static void main(String[] args) throws IOException {
        Map<Haha, String> map = Maps.newHashMap();
        Haha one = new Haha("one", "ho");
        Haha two = new Haha("one", "ho");
        two.setHi("hi-one");

        map.put(one, "This is one");
        System.out.println("One: " + map.get(one));
        System.out.println("Two: " + map.get(two));
        System.out.println("Full Map: " + map);
        map.remove(two);
        map.put(two, "THIS IS TWO");
        System.out.println("Full Map After: " + map);

    }

    @EqualsAndHashCode(of = { "ha", "ho" })
    @RequiredArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class Haha {
        @NonNull
        String ha;
        @NonNull
        String ho;
        String hi;
    }

}
