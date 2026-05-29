package app.logic;

public class LogicTuple<T> {

    public final T data;
    public final Context context;

    public LogicTuple(T data, Context context) {
        this.data = data;
        this.context = context;
    }
}
