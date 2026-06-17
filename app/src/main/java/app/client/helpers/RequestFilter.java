package app.client.helpers;

public class RequestFilter {

    public String col;
    public String[] val;
    public String[] special;

    public RequestFilter(String column, String[] value, String[] special) {
        this.col = column;
        this.val = value;
        this.special = special;
    }
}
