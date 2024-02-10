package forward.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forward.Algorithms;

// TODO: Remove bridge once all features are in place
public final class TestBridge {
    private TestBridge() {
    }

    public static Algorithms createAlgorithms() {
        return new Algorithms();
    }

    public static <T> List<T> asMutable(List<T> list) {
        return new ArrayList<>(list);
    }

    public static Comparator<Integer> createComparator() {
        return Comparator.comparingInt(i -> i);
    }
}
