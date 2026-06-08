package app.client.objects;

import app.generic.interfaces.IDBObject;
import java.util.HashMap;
import java.util.Map;

public class StoreProduct implements IDBObject {

    public Map<String, Object> storeProduct = new HashMap<>();

    public StoreProduct(
        String upc,
        String upc_prom,
        int id_product,
        double selling_price,
        int products_number,
        boolean promotional_product
    ) {
        storeProduct.put("upc", upc);
        storeProduct.put("upc_prom", upc_prom);
        storeProduct.put("id_product", id_product);
        storeProduct.put("selling_price", selling_price);
        storeProduct.put("products_number", products_number);
        storeProduct.put("promotional_product", promotional_product);
    }

    @Override
    public String getPrimaryKey() {
        return "upc";
    }

    @Override
    public Object getPrimaryValue() {
        return storeProduct.get(getPrimaryKey());
    }

    @Override
    public Map<String, Object> getMap() {
        return storeProduct;
    }
}
