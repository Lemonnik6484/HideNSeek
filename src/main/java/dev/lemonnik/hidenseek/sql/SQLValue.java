package dev.lemonnik.hidenseek.sql;

public record SQLValue(String name, Type type, boolean nullable, boolean isPrimaryKey, boolean unique, boolean isAutoIncrement) {
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
        if (isAutoIncrement) builder.append(" AUTOINCREMENT");

        return "%s %s%s".formatted(
                name,
                type.name(),
                builder.toString()
        );
    }
}
