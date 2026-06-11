package app.client.helpers;

public class RequestFilter {

    public String col;
    public String val;
    public String mode;

    public RequestFilter(String column, String value, String mode) {
        this.col = column;
        this.val = value;
        this.mode = mode;
    }
}
