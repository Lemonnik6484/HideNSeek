package dev.lemonnik.hidenseek.sql;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QueryBuilder {
    public static String createTable(String name, List<SQLRow> rows) {
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        builder.append(name);
        builder.append(" (");

        for (SQLRow row : rows) {
            builder.append(row.toStringAsSchema());
            builder.append(", ");
        }

        builder.delete(builder.length() - 2, builder.length());

        builder.append(");");
        return builder.toString();
    }

    public static String insert(String table, List<SQLRow> rows) {
        StringBuilder builder = new StringBuilder("INSERT INTO ");
        builder.append(table);

        builder.append(" (");
        for (SQLRow row : rows) {
            builder.append(row.name());
            builder.append(", ");
        }

        builder.delete(builder.length() - 2, builder.length());

        builder.append(") VALUES (");
        builder.repeat("?, ", rows.size());
        builder.delete(builder.length() - 2, builder.length());
        builder.append(");");

        return builder.toString();
    }

    public static String update(String table, List<SQLRow> setRows, @Nullable List<SQLRow> whereRows) {
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append(table);
        builder.append(" SET ");

        for (SQLRow row : setRows) {
            builder.append(row.name());
            builder.append(" = ?, ");
        }

        builder.delete(builder.length() - 2, builder.length());

        if (whereRows != null && !whereRows.isEmpty()) {
            builder.append(" WHERE ");

            for (SQLRow row : whereRows) {
                builder.append(row.name());
                builder.append(" = ? AND ");
            }

            builder.delete(builder.length() - 5, builder.length());
        }

        builder.append(";");
        return builder.toString();
    }

    /**
     * Makes a new SELECT query in the specified table. If {@code columns} is
     * {@code null}, it will default to {@code *}. The same goes for {@code whereRows}
     *
     * @param table the table to search
     * @param columns the rows to target; null = *
     * @param whereRows the rows to check where; null = *
     * @return the query
     */
    public static String select(String table, @Nullable List<SQLRow> columns, @Nullable List<SQLRow> whereRows) {
        StringBuilder builder = new StringBuilder("SELECT ");

        if (columns == null || columns.isEmpty()) {
            builder.append("*");
        } else {
            for (SQLRow row : columns) {
                builder.append(row.name());
                builder.append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());
        }

        builder.append(" FROM ");
        builder.append(table);

        if (whereRows != null && !whereRows.isEmpty()) {
            builder.append(" WHERE ");

            for (SQLRow row : whereRows) {
                builder.append(row.name());
                builder.append(" = ? AND ");
            }

            builder.delete(builder.length() - 5, builder.length());
        }

        builder.append(";");
        return builder.toString();
    }
}
