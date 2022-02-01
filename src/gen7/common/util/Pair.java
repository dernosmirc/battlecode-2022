package gen7.common.util;

public class Pair<K, V> {
    public K a;
    public V b;

    public Pair(K key, V value) {
        this.a = key;
        this.b = value;
    }

    public K getA() {
        return a;
    }

    public V getB() {
        return b;
    }
}
