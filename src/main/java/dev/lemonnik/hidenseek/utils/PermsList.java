package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.sql.QueryBuilder;
import dev.lemonnik.hidenseek.sql.SQLManager;
import net.minestom.server.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

public class PermsList {
    public static void set(Player player) {
        set(player.getUuid(), player.getPermissionLevel());
    }

    public static void set(UUID uuid, int permissionLevel) {
        try {
            var update = SQLManager.conn.prepareStatement(QueryBuilder.update(
                    "permissions",
                    List.of(SQLManager.ROW_PERMISSION_LEVEL),
                    List.of(SQLManager.ROW_UUID)
            ));

            update.setInt(1, permissionLevel);
            update.setString(2, uuid.toString());

            int rowsUpdated = update.executeUpdate();

            if (rowsUpdated == 0) {
                String insertSql = QueryBuilder.insert(
                        "permissions",
                        List.of(SQLManager.ROW_UUID, SQLManager.ROW_PERMISSION_LEVEL)
                );

                PreparedStatement insert = SQLManager.conn.prepareStatement(insertSql);
                insert.setString(1, uuid.toString());
                insert.setInt(2, permissionLevel);
                insert.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getLevel(UUID uuid) {
        try {
            String sql = QueryBuilder.select(
                    "permissions",
                    List.of(SQLManager.ROW_PERMISSION_LEVEL),
                    List.of(SQLManager.ROW_UUID)
            );

            PreparedStatement statement = SQLManager.conn.prepareStatement(sql);
            statement.setString(1, uuid.toString());

            ResultSet result = statement.executeQuery();

            if (!result.next()) return 0;

            return result.getInt(SQLManager.ROW_PERMISSION_LEVEL.name());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
