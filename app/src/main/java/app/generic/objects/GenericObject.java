package app.generic.objects;

import app.generic.interfaces.IDBObject;
import java.util.Map;

public class GenericObject implements IDBObject {

    public Map<String, Object> object_map;
    public String primary_key;

    public GenericObject(String primary_key, Map<String, Object> object_map) {
        this.primary_key = primary_key;
        this.object_map = object_map;
    }

    @Override
    public String getPrimaryKey() {
        return primary_key;
    }

    @Override
    public Object getPrimaryValue() {
        return object_map.get(getPrimaryKey());
    }

    @Override
    public Map<String, Object> getMap() {
        return object_map;
    }
}
