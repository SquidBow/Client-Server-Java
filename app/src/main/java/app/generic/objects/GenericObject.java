package app.generic.objects;

import app.generic.interfaces.IDBObject;
import java.util.Map;

public class GenericObject implements IDBObject {

    public Map<String, Object> object_map;
    public String[] primary_keys;

    public GenericObject(
        String[] primary_keys,
        Map<String, Object> object_map
    ) {
        this.primary_keys = primary_keys;
        this.object_map = object_map;
    }

    @Override
    public String[] getPrimaryKeys() {
        return primary_keys;
    }

    @Override
    public Object[] getPrimaryValues() {
        String[] keys = getPrimaryKeys();
        Object[] vals = new Object[keys.length];

        int i = 0;
        for (String key : keys) {
            vals[i++] = object_map.get(key);
        }

        return vals;
    }

    @Override
    public Map<String, Object> getMap() {
        return object_map;
    }
}
