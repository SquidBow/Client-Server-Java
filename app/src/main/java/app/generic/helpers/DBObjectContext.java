package app.generic.helpers;

import app.generic.interfaces.IDBObject;

public class DBObjectContext {

    public String table;
    public IDBObject object;

    public DBObjectContext(String table, IDBObject object) {
        this.table = table;
        this.object = object;
    }
}
