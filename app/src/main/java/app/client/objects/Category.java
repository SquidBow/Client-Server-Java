// package app.client.objects;
// import app.generic.interfaces.IDBObject;
// import java.util.HashMap;
// import java.util.Map;
// public class Category implements IDBObject {
//     public Map<String, Object> category = new HashMap<>();
//     public Category(int category_number, String category_name) {
//         category.put("category_number", category_number);
//         category.put("category_name", category_name);
//     }
//     @Override
//     public String getPrimaryKey() {
//         return "category_number";
//     }
//     @Override
//     public Object getPrimaryValue() {
//         return category.get(getPrimaryKey());
//     }
//     @Override
//     public Map<String, Object> getMap() {
//         return category;
//     }
// }
