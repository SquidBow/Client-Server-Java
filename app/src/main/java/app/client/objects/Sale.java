package app.client.objects;

import app.generic.interfaces.IDBObject;
import java.util.HashMap;
import java.util.Map;

public class Sale implements IDBObject {

    public Map<String, Object> sale = new HashMap<>();

    public Sale(
        String upc,
        String check_number,
        int product_number,
        double selling_price
    ) {
        sale.put("upc", upc);
        sale.put("check_number", check_number);
        sale.put("product_number", product_number);
        sale.put("selling_price", selling_price);
    }

    @Override
    public String getPrimaryKey() {
        return "upc";
    }

    @Override
    public Object getPrimaryValue() {
        return sale.get(getPrimaryKey());
    }

    @Override
    public Map<String, Object> getMap() {
        return sale;
    }
}
