package app.generic.helpers;

public class DBContext {

    public String table;
    public String order_column;
    public boolean order_ascending;
    public String[] filters;

    // Пагінація!!!
    public int limit;
    public int offset;

    public DBContext(
        String table,
        String[] filters,
        int limit,
        int offset,
        String order_column,
        boolean order_ascending
    ) {
        this.table = table;
        this.filters = filters;
        this.limit = limit;
        this.offset = offset;
        this.order_column = order_column;
        this.order_ascending = order_ascending;
    }
}
