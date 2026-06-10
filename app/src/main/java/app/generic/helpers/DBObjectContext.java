package app.generic.helpers;

import app.generic.objects.GenericObject;

public class DBObjectContext {

    public String table;
    public GenericObject object;

    public DBObjectContext(String table, GenericObject object) {
        this.table = table;
        this.object = object;
    }
}
