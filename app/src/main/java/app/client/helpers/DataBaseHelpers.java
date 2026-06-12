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

    // Since filters are gona be created on the client need to pass in the value to
    // the use later
    public static String createFilterStatementWord(
        String column,
        String word,
        boolean exact
    ) {
        String sql = " and " + column;

        if (exact) {
            sql += " = ?";

            return sql + "&&&" + word + "";
        } else {
            sql += " like ?";

            return sql + "&&&%" + word + "%";
        }
    }

    public static String createFilterStatementInteger(
        String column,
        String val,
        String mode
    ) {
        String sql = " and " + column;

        sql += " " + mode + " ?";

        return sql + "&&&" + val;
    }

    //So it doesn't habe to translate into int and back to string
    public static String createFilterStatementDate(
        String column,
        String val,
        String mode
    ) {
        String sql = " and " + column;

        if (mode.equals("Start")) {
            sql += " >= ?";
        } else {
            sql += " <= ?";
        }

        java.time.format.DateTimeFormatter inputFormat =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");

        java.time.format.DateTimeFormatter dbFormat =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String dbDate = java.time.LocalDate.parse(val, inputFormat).format(
            dbFormat
        );

        return sql + "&&&" + dbDate;
    }
}
