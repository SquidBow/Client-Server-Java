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
        String[] primary_keys,
        String[] col_names,
        String[] values
    ) {
        String pk_string = String.join(":::", primary_keys);

        String map = "";

        for (int i = 0; i < col_names.length; i++) {
            if (i > 0) map += ":::";
            map += col_names[i] + "&&&" + values[i];
        }

        return String.join(";;;", table, pk_string, map);
    }

    // Since filters are gona be created on the client need to pass in the value to
    // the use later
    public static String createFilterStatementWord(
        String column,
        String[] word,
        String[] mode
    ) {
        String sql = " and (";

        for (int i = 0; i < word.length; i++) {
            if (i > 0) sql += " OR ";
            if (word[i].equals("NULL")) {
                sql += column + " is null";
            } else if (mode[i].equals("Exact")) {
                sql += column + " = ?";
            } else {
                sql += column + " like ?";
            }
        }

        sql += ")&&&";

        for (int i = 0; i < word.length; i++) {
            if (i > 0) sql += "&&&";
            if (word[i].equals("NULL")) {
            } else if (mode[i].equals("Exact")) {
                sql += word[i];
            } else {
                sql += word[i];
            }
        }

        return sql;
    }

    public static String createFilterStatementInteger(
        String column,
        String[] val,
        String[] mode
    ) {
        String sql = " and (";

        for (int i = 0; i < val.length; i++) {
            if (i > 0) sql += " OR ";
            if (val[i].equals("NULL")) {
                sql += column + " is null";
            } else {
                sql += column + " " + mode[i] + " ?";
            }
        }

        sql += ")&&&";

        for (int i = 0; i < val.length; i++) {
            if (i > 0) sql += "&&&";

            if (!val[i].equals("NULL")) {
                sql += val[i];
            }
        }

        return sql;
    }

    //So it doesn't habe to translate into int and back to string
    public static String createFilterStatementDate(
        String column,
        String[] val,
        String[] mode
    ) {
        String sql = " and (";

        for (int i = 0; i < val.length; i++) {
            if (i > 0) sql += " OR ";
            if (val[i].equals("NULL")) {
                sql += column + " is null";
            } else if (mode[i].equals("Start")) {
                sql += column + " >= ?";
            } else {
                sql += column + " <= ?";
            }
        }

        sql += ")&&&";

        for (int i = 0; i < val.length; i++) {
            if (i > 0) sql += "&&&";
            if (val[i].equals("NULL")) {
            } else if (mode[i].equals("Start")) {
                sql += val[i];
            } else {
                sql += val[i];
            }
        }

        return sql;
    }
}
