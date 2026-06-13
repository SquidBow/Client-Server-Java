package app.client.helpers;

public class ColumnData {

    public String name;
    public String type;
    public boolean nullable;

    public ColumnData(String name, String type, boolean nullable) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }
}
