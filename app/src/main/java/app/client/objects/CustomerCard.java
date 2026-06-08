package app.client.objects;

import app.generic.interfaces.IDBObject;
import java.util.HashMap;
import java.util.Map;

public class CustomerCard implements IDBObject {

    public Map<String, Object> customerCard = new HashMap<>();

    public CustomerCard(
        String card_number,
        String cust_surname,
        String cust_name,
        String cust_patronymic,
        String phone_number,
        String city,
        String street,
        String zip_code,
        int percent
    ) {
        customerCard.put("card_number", card_number);
        customerCard.put("cust_surname", cust_surname);
        customerCard.put("cust_name", cust_name);
        customerCard.put("cust_patronymic", cust_patronymic);
        customerCard.put("phone_number", phone_number);
        customerCard.put("city", city);
        customerCard.put("street", street);
        customerCard.put("zip_code", zip_code);
        customerCard.put("percent", percent);
    }

    @Override
    public String getPrimaryKey() {
        return "card_number";
    }

    @Override
    public Object getPrimaryValue() {
        return customerCard.get(getPrimaryKey());
    }

    @Override
    public Map<String, Object> getMap() {
        return customerCard;
    }
}
