package app.generic.helpers;

public class Context {

    public NetContext net_context;
    public DBContext db_context = null;
    public DBObjectContext object_context = null;

    Context(NetContext network_context, DBContext database_context) {
        net_context = network_context;
        db_context = database_context;
    }

    Context(NetContext network_context, DBObjectContext object_context) {
        net_context = network_context;
        this.object_context = object_context;
    }
}
