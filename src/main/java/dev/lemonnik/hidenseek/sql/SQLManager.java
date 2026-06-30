package dev.lemonnik.hidenseek.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLManager {
    public static final SQLRow ROW_UUID = SQLRow.simple("uuid", SQLRow.Type.TEXT);
    public static final SQLRow ROW_PERMISSION_LEVEL = SQLRow.simple("permissionLevel", SQLRow.Type.INTEGER);

    public static final SQLRow ROW_WORLD_ID = SQLRow.primary("worldId", SQLRow.Type.TEXT);
    public static final SQLRow ROW_X = SQLRow.simple("posX", SQLRow.Type.REAL);
    public static final SQLRow ROW_Y = SQLRow.simple("posY", SQLRow.Type.REAL);
    public static final SQLRow ROW_Z = SQLRow.simple("posZ", SQLRow.Type.REAL);

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

        conn.prepareStatement(QueryBuilder.createTable(
                "spawns",
                List.of(ROW_WORLD_ID, ROW_X, ROW_Y, ROW_Z)
        )).execute();
    }

    public static void insertOrUpdate(String table, List<SQLRow> keys, List<Object> values) throws SQLException {
        List<SQLRow> whereRows = keys.stream().filter(SQLRow::isPrimaryKey).toList(); // primary keys
        List<SQLRow> setRows = keys.stream().filter(r -> !r.isPrimaryKey()).toList(); // not primary keys

        List<Object> setValues = new ArrayList<>();
        List<Object> whereValues = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).isPrimaryKey()) whereValues.add(values.get(i));
            else setValues.add(values.get(i));
        }

        var update = SQLManager.conn.prepareStatement(QueryBuilder.update(table, setRows, whereRows));
        fillStatement(update, setValues);
        fillStatement(update, whereValues);

        var linesUpdated = update.executeUpdate();
        if (linesUpdated == 0) {
            var insert = SQLManager.conn.prepareStatement(QueryBuilder.insert(table, keys));
            fillStatement(insert, values);
            insert.execute();
        }
    }

    public static void fillStatement(PreparedStatement statement, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            Object o = values.get(i);

            switch (o) {
                case Integer cast -> statement.setInt(i + 1, cast);
                case Double cast -> statement.setDouble(i + 1, cast);
                case String cast -> statement.setString(i + 1, cast);
                case Boolean cast -> statement.setBoolean(i + 1, cast);
                case null, default -> throw new RuntimeException("what is this thing \"%s\"".formatted(o));
            }
        }
    }
}
