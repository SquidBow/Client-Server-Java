package app.client.objects;

import app.generic.interfaces.IDBObject;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

public class Employee implements IDBObject {

    public Map<String, Object> employee = new HashMap<>();

    public Employee(
        String id_employee,
        String empl_surname,
        String empl_name,
        String empl_patronymic,
        String empl_role,
        double salary,
        Date date_of_birth,
        Date date_of_start,
        String phone_number,
        String city,
        String street,
        String zip_code
    ) {
        employee.put("id_employee", id_employee);
        employee.put("empl_surname", empl_surname);
        employee.put("empl_name", empl_name);
        employee.put("empl_patronymic", empl_patronymic);
        employee.put("empl_role", empl_role);
        employee.put("salary", salary);
        employee.put("date_of_birth", date_of_birth);
        employee.put("date_of_start", date_of_start);
        employee.put("phone_number", phone_number);
        employee.put("city", city);
        employee.put("street", street);
        employee.put("zip_code", zip_code);
    }

    @Override
    public String getPrimaryKey() {
        return "id_employee";
    }

    @Override
    public Object getPrimaryValue() {
        return employee.get(getPrimaryKey());
    }

    @Override
    public Map<String, Object> getMap() {
        return employee;
    }
}
