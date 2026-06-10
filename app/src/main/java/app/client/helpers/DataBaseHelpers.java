package app.client.helpers;

import app.generic.helpers.DBContext;

public class DataBaseHelpers {

    public static String encodeDBContext(DBContext context) {
        String filters =
            context.filters.length == 0
                ? ""
                : String.join(":::", context.filters);

        return String.join(
            ";;;",
            context.table,
            filters,
            String.valueOf(context.limit),
            String.valueOf(context.offset),
            context.order_column == null ? "" : context.order_column,
            String.valueOf(context.order_ascending)
        );
    }

    public static String encodeDBObjectContext(
        String table,
        String primary_key,
        String[] col_names,
        String[] values
    ) {
        String map = "";

        for (int i = 0; i < col_names.length; i++) {
            if (i > 0) map += ":::";
            map += col_names[i] + "&&&" + values[i];
        }

        return String.join(";;;", table, primary_key, map);
    }
}
