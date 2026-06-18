package app.generic.helpers;

public class Tuple<T, D> {

    public T first;
    public D second;

    public Tuple(T first, D second) {
        this.first = first;
        this.second = second;
    }
}
