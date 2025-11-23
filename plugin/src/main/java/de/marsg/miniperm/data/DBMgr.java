package de.marsg.miniperm.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.bukkit.configuration.ConfigurationSection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.marsg.miniperm.MiniPerm;

public class DBMgr {

    private static DataSource dataSource;

    private static MiniPerm plugin;

    public static boolean setup(MiniPerm plugin) {

        DBMgr.plugin = plugin;

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("db");

        HikariConfig dbConfig = new HikariConfig();

        dbConfig.setDriverClassName("de.marsg.miniperm.lib.org.postgresql.Driver");
        dbConfig.setJdbcUrl(config.getString("jdbc_url"));
        dbConfig.setUsername(config.getString("user"));
        dbConfig.setPassword(config.getString("password"));
        dbConfig.setMaximumPoolSize(10);
        dbConfig.setMinimumIdle(2);
        dbConfig.setMaxLifetime(90000); // 90 seconds
        dbConfig.setIdleTimeout(60000); // 60 seconds
        dbConfig.setKeepaliveTime(30000); // 30 seconds

        dataSource = new HikariDataSource(dbConfig);

        return ensureTablesExist() && ensureDefaultGroup();
    }

    /*
     * Group related parts
     */

    public static int addGroup(String name, String prefix, int weight, boolean isDefault) {
        try (Connection connection = dataSource.getConnection()) {

            String query = """
                    INSERT INTO groups (name, prefix, weight, is_default)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (name) DO NOTHING;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

                pst.setString(1, name.toLowerCase());
                pst.setString(2, prefix);
                pst.setInt(3, weight);
                pst.setBoolean(4, isDefault);

                int rows = pst.executeUpdate();

                ResultSet keys = pst.getGeneratedKeys();

                if (rows == 1 && keys.next()) {
                    plugin.getLogger().info(String.format("[DB] Created group '%s' in DB", name));
                    return keys.getInt(1);
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to add group: " + e.getMessage());
        }
        return -1;
    }

    public static boolean deleteGroup(String name) {
        try (Connection connection = dataSource.getConnection()) {

            String query = """
                    DELETE FROM groups
                    WHERE name = ?;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setString(1, name.toLowerCase());

                int rows = pst.executeUpdate();

                if (rows == 1) {
                    plugin.getLogger().info(String.format("[DB] Deleted group '%s' in DB", name));
                    return true;
                } else if (rows == 0) {
                    plugin.getLogger().info(String.format("[DB] Deleting group '%s' failed. It does not exist", name));
                } else {
                    plugin.getLogger().warning(
                            String.format("[DB] Deleting group '%s' deleted more than one entry!. Total: %d", name, rows));
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to delete group: " + e.getMessage());
        }
        return false;
    }

    public static int getGroupId(String name) {
        try (Connection connection = dataSource.getConnection()) {

            String query = """
                    SELECT id FROM groups
                    WHERE name = ?;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setString(1, name.toLowerCase());

                ResultSet rst = pst.executeQuery();

                if (rst.next()) {
                    return rst.getInt(1);
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to get group id: " + e.getMessage());
        }
        return -1;
    }

    /**
     * @return MAP of: String name, Object[int id, String prefix, int weight,
     *         boolean
     *         isDefault]
     */
    public static Map<String, Object[]> getAllGroups() {
        try (Connection connection = dataSource.getConnection()) {
            String query = """
                    SELECT id, name, prefix, weight, is_default FROM groups;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                ResultSet rst = pst.executeQuery();

                Map<String, Object[]> result = HashMap.newHashMap(2);

                while (rst.next()) {
                    result.put(rst.getString("name"),
                            new Object[] { rst.getInt("id"), rst.getString("prefix"), rst.getInt("weight"),
                                    rst.getBoolean("is_default") });
                }

                return result;

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to get group id: " + e.getMessage());
        }

        return HashMap.newHashMap(0);
    }

    /*
     * Permissions stuff
     */

    public static boolean addPermission(int groupId, String permission) {
        try (Connection connection = dataSource.getConnection()) {

            String query = """
                    INSERT INTO permissions (groups_id, permission)
                    VALUES (?, ?)
                    ON CONFLICT (groups_id, permission) DO NOTHING;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setInt(1, groupId);
                pst.setString(2, permission);

                int rows = pst.executeUpdate();

                if (rows == 1) {
                    plugin.getLogger()
                            .info(String.format("[DB] Permission '%s' added to group '%d' in DB", permission, groupId));
                    return true;
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to add permission to group: " + e.getMessage());
        }
        return false;
    }

    public static boolean removePermission(int groupId, String permission) {
        try (Connection connection = dataSource.getConnection()) {

            String query = """
                    DELETE FROM  permissions
                    WHERE groups_id = ? AND permission = ?;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setInt(1, groupId);
                pst.setString(2, permission);

                int rows = pst.executeUpdate();

                if (rows == 1) {
                    plugin.getLogger()
                            .info(String.format("[DB] Permission '%s' removed from group '%d' in DB", permission, groupId));
                    return true;
                } else if (rows == 0) {
                    plugin.getLogger().info(String.format(
                            "[DB] Deleting permission '%s' failed for group %d. It does not exist", permission, groupId));
                } else {
                    plugin.getLogger()
                            .warning(String.format(
                                    "[DB] Deleting permission '%s' deleted more than one entry for group %d!. Total: %d",
                                    permission, groupId, rows));
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to remove permission from group: " + e.getMessage());
        }
        return false;
    }

    public static Set<String> getGroupsPermission(int groupId) {

        try (Connection connection = dataSource.getConnection()) {
            String query = """
                    SELECT permission FROM permissions
                    WHERE groups_id = ?;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setInt(1, groupId);

                ResultSet rst = pst.executeQuery();

                Set<String> result = HashSet.newHashSet(10);

                while (rst.next()) {
                    result.add(rst.getString(1));
                }

                return result;

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to get group id: " + e.getMessage());
        }

        return HashSet.newHashSet(0);
    }

    /*
     * User related thinks
     */

    public static boolean setUsersGroup(UUID uuid, int groupId, Instant expiresAt) {
        try (Connection connection = dataSource.getConnection()) {
            String query = """
                    INSERT INTO users (uuid, groups_id, expires_at)
                    VALUES (?, ?, ?)
                    ON CONFLICT (uuid) DO UPDATE
                    SET groups_id = EXCLUDED.groups_id,
                        expires_at = EXCLUDED.expires_at;
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setObject(1, uuid);
                pst.setInt(2, groupId);
                if (expiresAt == null) {
                    pst.setNull(3, Types.TIMESTAMP_WITH_TIMEZONE);
                }else{
                    pst.setTimestamp(3, Timestamp.from(expiresAt));
                }

                int rows = pst.executeUpdate();

                if (rows == 1) {
                    plugin.getLogger()
                            .info(String.format("[DB] Set a users group. UUID: %s; groupID: %d", uuid.toString(), groupId));
                    return true;
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to set user's group: " + e.getMessage());
        }
        return false;
    }

    /**
     * @param uuid
     * @return {String groupName, Instant expiresAt}
     */
    public static Object[] getUsersGroup(UUID uuid) {
        try (Connection connection = dataSource.getConnection()) {

            String query = """
                    SELECT g.name, u.expires_at
                    FROM groups g
                    JOIN users u ON g.id = u.groups_id
                    WHERE u.uuid = ?
                    AND (u.expires_at > CURRENT_TIMESTAMP OR u.expires_at IS NULL);
                    """;
            try (PreparedStatement pst = connection.prepareStatement(query)) {

                pst.setObject(1, uuid);

                ResultSet rst = pst.executeQuery();

                if (rst.next()) {
                    Timestamp expiryTimestamp = rst.getTimestamp(2);
                    Instant expiryInstant = (expiryTimestamp != null) ? expiryTimestamp.toInstant() : null;

                    return new Object[] { rst.getString(1), expiryInstant };
                }

            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to get user's group: " + e.getMessage());
        }
        return new Object[] {};
    }

    /*
     * the default creation functions
     */
    private static boolean ensureTablesExist() {
        try (Connection connection = dataSource.getConnection()) {
            // Ensure tabels exist
            String tables = """
                    CREATE TABLE IF NOT EXISTS groups (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(64) NOT NULL UNIQUE,
                        prefix VARCHAR(32) NOT NULL,
                        weight INT NOT NULL,
                        is_default BOOLEAN NOT NULL DEFAULT FALSE
                    );

                    CREATE TABLE IF NOT EXISTS users (
                        uuid UUID PRIMARY KEY,
                        groups_id INT NOT NULL,
                        expires_at TIMESTAMPTZ NULL,
                        CONSTRAINT fk_users_group FOREIGN KEY (groups_id)
                            REFERENCES groups (id)
                            ON UPDATE CASCADE
                            ON DELETE CASCADE
                    );

                    CREATE TABLE IF NOT EXISTS permissions (
                        id BIGSERIAL PRIMARY KEY,
                        groups_id INT NOT NULL,
                        permission VARCHAR(128) NOT NULL,
                        CONSTRAINT fk_permissions_group FOREIGN KEY (groups_id)
                            REFERENCES groups (id)
                            ON UPDATE CASCADE
                            ON DELETE CASCADE,
                        CONSTRAINT uq_group_permission UNIQUE (groups_id, permission)
                    );

                    CREATE INDEX IF NOT EXISTS idx_users_group_id ON users(groups_id);
                    CREATE INDEX IF NOT EXISTS idx_permissions_group_id ON permissions(groups_id);
                    CREATE INDEX IF NOT EXISTS idx_permissions_permission ON permissions(permission);
                    """;
            try (PreparedStatement pst = connection.prepareStatement(tables)) {

                pst.executeUpdate();
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to create default tables: " + e.getMessage());
        }
        return false;
    }

    private static boolean ensureDefaultGroup() {
        try (Connection connection = dataSource.getConnection()) {
            // Check if the default group already exists
            String checkQuery = "SELECT COUNT(*) FROM groups WHERE is_default = TRUE";
            try (PreparedStatement pst = connection.prepareStatement(checkQuery)) {

                ResultSet resultSet = pst.executeQuery();
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    // If no default group exist, create it
                    createDefaultGroup(connection);
                }
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] Failed to validate default group exists: " + e.getMessage());
        }
        return false;
    }

    private static void createDefaultGroup(Connection connection) throws SQLException {
        // Create the default group
        String insertQuery = "INSERT INTO groups (name, prefix, weight, is_default) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(insertQuery)) {
            pst.setString(1, "default");
            pst.setString(2, "§7[§8Player§7]§r");
            pst.setInt(3, 0);
            pst.setBoolean(4, true); // Mark as default
            pst.executeUpdate();
            plugin.getLogger().info("[DB] Default group was created.");
        }
    }

}
