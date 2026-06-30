package dev.lemonnik.hidenseek.sql;

/**
 * Abstraction layer for SQL rows. It is recommended to use this class's
 * helper methods over its constructor for less typing.
 *
 * @param name the name of the row
 * @param type the type of values inside the row
 * @param nullable whether the row is nullable
 * @param isPrimaryKey whether the row is the primary key for the table
 * @param unique whether cells in this row must be unique
 * @param autoIncrement whether the values of the cells in this row will automatically increment
 */
public record SQLRow(String name, Type type, boolean nullable, boolean isPrimaryKey, boolean unique, boolean autoIncrement) {
    public enum Type {
        INTEGER,
        TEXT,
        BOOLEAN
    }

    public String toStringAsSchema() {
        StringBuilder builder = new StringBuilder();

        if (isPrimaryKey) builder.append(" PRIMARY KEY");
        if (!nullable) builder.append(" NOT NULL");
        if (unique) builder.append(" UNIQUE");
        if (autoIncrement) builder.append(" AUTOINCREMENT");

        return "%s %s%s".formatted(
                name,
                type.name(),
                builder.toString()
        );
    }

    //
    // creation methods
}
