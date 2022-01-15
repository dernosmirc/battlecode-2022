package gen3_2.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import static gen3_2.RobotPlayer.rc;
import static gen3_2.RobotPlayer.DEBUG;

@SuppressWarnings("unchecked")
public class Vector<T> implements Iterable<T> {
    private final Random random = new Random(rc.getID());
    private final T[] container;
    public final int maxSize;
    public int length = 0;

    private void log(String log) {
        if (DEBUG) {
            System.out.println(log);
        }
    }

    public Vector (int maxSize) {
        container = (T[]) new Object[maxSize];
        this.maxSize = maxSize;
    }

    public Vector (T[] arr){
        container = arr.clone();
        this.maxSize = arr.length;
        this.length = arr.length;
    }

    public void set(int i, T value){
        if (i >= this.length){
            log("Vector Size Exceeded");
        } else {
            container[i] = value;
        }
    }

    public void add(T val) {
        if (length == maxSize) {
            log("Insufficient size");
        } else {
            container[length++] = val;
        }
    }

    public T get(int i){
        if (i >= this.length) {
            log("Vector Size Exceeded");
            return null;
        } else {
            return container[i];
        }
    }

    public boolean has(T a) {
        for (int i = 0; i < length; i++) {
            if (a.equals(container[i])) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(T a) {
        for (int i = 0; i < length; i++) {
            if (a.equals(container[i])) {
                return i;
            }
        }
        return -1;
    }

    public void sort(Comparator<T> c) {
        Arrays.sort(container, 0, length, c);
    }

    public T minElement(Comparator<T> c) {
        if (length == 0) return null;
        T min = container[0];
        for (int i = 1; i < length; i++) {
            if (c.compare(min, container[i]) < 0) {
                min = container[i];
            }
        }
        return min;
    }

    public T maxElement(Comparator<T> c) {
        if (length == 0) return null;
        T max = container[0];
        for (int i = 1; i < length; i++) {
            if (c.compare(max, container[i]) > 0) {
                max = container[i];
            }
        }
        return max;
    }

    @Override
    public Iterator<T> iterator() {
        return new VectorIterator();
    }

    public class VectorIterator implements Iterator<T> {
        int curr;
        VectorIterator() {
            curr = -1;
        }

        @Override
        public boolean hasNext() {
            return curr+1 < length;
        }

        @Override
        public T next() {
            curr++;
            return container[curr];
        }
    }
}

