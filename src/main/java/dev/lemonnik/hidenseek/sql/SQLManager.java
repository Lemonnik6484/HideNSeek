package dev.lemonnik.hidenseek.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class SQLManager {
    private static final Connection connection;

    static {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() throws SQLException {
        connection.prepareStatement(QueryBuilder.createTable("users", List.of(
                SQLRow.primaryAutoIncrement("systemId"),
                SQLRow.simple("username", SQLRow.Type.TEXT),
                SQLRow.unique("uuid", SQLRow.Type.TEXT)
        ))).execute();

        connection.prepareStatement(QueryBuilder.createTable("permissions", List.of(
                SQLRow.primary("userId", SQLRow.Type.INTEGER),
                SQLRow.simple("permissionLevel", SQLRow.Type.INTEGER)
        ))).execute();
    }
}
