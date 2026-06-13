package app.generic.interfaces;

import java.util.Map;

public interface IDBObject {
    Map<String, Object> getMap();

    String[] getPrimaryKeys();

    Object[] getPrimaryValues();
}
