package app.client.objects;

import app.generic.interfaces.IDBObject;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Check implements IDBObject {

    public Map<String, Object> check = new HashMap<>();

    public Check(
        String check_number,
        String id_employee,
        String card_number,
        Timestamp print_date,
        double sum_total,
        double vat
    ) {
        check.put("check_number", check_number);
        check.put("id_employee", id_employee);
        check.put("card_number", card_number);
        check.put("print_date", print_date);
        check.put("sum_total", sum_total);
        check.put("vat", vat);
    }

    @Override
    public String getPrimaryKey() {
        return "check_number";
    }

    @Override
    public Object getPrimaryValue() {
        return check.get(getPrimaryKey());
    }

    @Override
    public Map<String, Object> getMap() {
        return check;
    }
}
