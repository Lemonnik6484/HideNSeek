package dev.lemonnik.hidenseek.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class SQLManager {
    public static final SQLRow ROW_UUID = SQLRow.simple("uuid", SQLRow.Type.TEXT);
    public static final SQLRow ROW_PERMISSION_LEVEL = SQLRow.simple("permissionLevel", SQLRow.Type.INTEGER);

    public static final Connection conn;

    static {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:database.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() throws SQLException {
        conn.prepareStatement(QueryBuilder.createTable(
                "permissions",
                List.of(ROW_UUID, ROW_PERMISSION_LEVEL)
        )).execute();
    }
}
