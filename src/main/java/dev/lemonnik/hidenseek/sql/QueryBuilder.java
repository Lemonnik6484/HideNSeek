package dev.lemonnik.hidenseek.sql;

import java.util.List;

public class QueryBuilder {
    public static String createTable(String name, List<SQLValue> rows) {
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        builder.append(name);
        builder.append(" (");

        for (SQLValue row : rows) {
            builder.append(row.toStringAsSchema());
            builder.append(", ");
        }

        builder.delete(builder.length() - 2, builder.length());

        builder.append(");");
        return builder.toString();
    }
}
