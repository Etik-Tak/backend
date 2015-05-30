package dk.etiktak.backend.util;

import java.util.ArrayList;
import java.util.List;

public class Lists {
    public static <E> List<E> asList(Iterable<E> iter) {
        List<E> list = new ArrayList<E>();
        for (E item : iter) {
            list.add(item);
        }
        return list;
    }
}
