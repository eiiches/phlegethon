package net.thisptr.phlegethon.misc;

public class Pair<T, U> {
    public final T _1;
    public final U _2;

    private Pair(T _1, U _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static <T, U> Pair<T, U> of(T _1, U _2) {
        return new Pair<>(_1, _2);
    }
}
