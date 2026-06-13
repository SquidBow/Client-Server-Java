package app.client.helpers;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class Conversions {

    @SuppressWarnings("unchecked")
    public static RequestFilter nodeToFilter(Node node) {
        HBox row = (HBox) node;

        ComboBox<String> column_field = (ComboBox<String>) row
            .getChildren()
            .get(0);

        TextField value_field = (TextField) row.getChildren().get(1);

        ComboBox<String> special_filed = (ComboBox<String>) row
            .getChildren()
            .get(2);

        return new RequestFilter(
            column_field.getValue(),
            value_field.getText(),
            special_filed.getValue()
        );
    }

    public static boolean verifyAgainstTypeLoose(
        String value,
        ColumnData data
    ) {
        if (value.equals("NULL") && data.nullable) return true;

        if (data.type.equals("TEXT")) {
            return true;
        }

        if (value.isBlank()) return false;

        if (data.type.equals("INTEGER") || data.type.equals("REAL")) {
            if (
                value.length() == 1 && !Character.isDigit(value.charAt(0))
            ) return false;

            if (value.equals("-.")) return false;

            boolean dot = false;
            int start = value.charAt(0) == '-' ? 1 : 0;

            for (char val : value.substring(start).toCharArray()) {
                if (val == '.') {
                    if (dot == true) return false;
                    else dot = true;

                    continue;
                }

                if (!Character.isDigit(val)) return false;
            }

            return true;
        } else if (data.type.equals("DATE")) {
            java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");

            try {
                java.time.LocalDate.parse(value, formatter);
                return true;
            } catch (java.time.format.DateTimeParseException e) {
                return false;
            }
        }

        return false;
    }

    public static boolean verifyAgainstTypeStrict(String value, String type) {
        if (value.isBlank()) return false;

        if (type.equals("INTEGER") || type.equals("REAL")) {
            if (
                value.length() == 1 && !Character.isDigit(value.charAt(0))
            ) return false;

            if (value.equals("-.")) return false;

            boolean dot = false;
            int start = value.charAt(0) == '-' ? 1 : 0;

            for (char val : value.substring(start).toCharArray()) {
                if (val == '.') {
                    if (type.equals("INTEGER")) return false;

                    if (dot == true) return false;
                    else dot = true;

                    continue;
                }

                if (!Character.isDigit(val)) return false;
            }

            return true;
        } else if (type.equals("DATE")) {
            java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");

            try {
                java.time.LocalDate.parse(value, formatter);
                return true;
            } catch (java.time.format.DateTimeParseException e) {
                return false;
            }
        }

        return false;
    }
}
