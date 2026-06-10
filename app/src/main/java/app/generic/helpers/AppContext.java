package app.generic.helpers;

public class AppContext<T> {

    public T data;
    public String user_role;
    public NetContext net_context;

    public AppContext(T data, String user_role, NetContext net_context) {
        this.data = data;
        this.user_role = user_role;
        this.net_context = net_context;
    }
}
