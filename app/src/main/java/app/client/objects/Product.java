// package app.client.objects;
// import app.generic.interfaces.IDBObject;
// import java.util.HashMap;
// import java.util.Map;
// public class Product implements IDBObject {
//     public Map<String, Object> product = new HashMap<>();
//     public Product(
//         String upc,
//         String name,
//         int category,
//         double price,
//         int quantity,
//         String manufacturer,
//         String characteristics
//     ) {
//         product.put("upc", upc);
//         product.put("name", name);
//         product.put("category", category);
//         product.put("price", price);
//         product.put("quantity", quantity);
//         product.put("manufacturer", manufacturer);
//         product.put("characteristics", characteristics);
//     }
//     public String getPrimaryKey() {
//         return "upc";
//     }
//     public Object getPrimaryValue() {
//         return product.get(getPrimaryKey());
//     }
//     @Override
//     public Map<String, Object> getMap() {
//         return product;
//     }
// }
