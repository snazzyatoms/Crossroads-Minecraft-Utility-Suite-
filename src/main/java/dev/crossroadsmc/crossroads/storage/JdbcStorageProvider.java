package dev.crossroadsmc.crossroads.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.crossroadsmc.crossroads.CrossroadsPlugin;
import dev.crossroadsmc.crossroads.model.ModerationLogEntry;
import dev.crossroadsmc.crossroads.model.PlayerData;
import dev.crossroadsmc.crossroads.model.SavedLocation;
import dev.crossroadsmc.crossroads.util.PlayerDataCodec;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class JdbcStorageProvider implements StorageProvider {
    private final CrossroadsPlugin plugin;
    private final StorageType type;
    private HikariDataSource dataSource;

    public JdbcStorageProvider(CrossroadsPlugin plugin, StorageType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public void initialize() throws Exception {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("Crossroads-" + type.name());
        hikari.setMaximumPoolSize(plugin.getConfig().getInt("storage.pool.maximum-size", 6));
        hikari.setMinimumIdle(plugin.getConfig().getInt("storage.pool.minimum-idle", 1));
        hikari.setConnectionTimeout(plugin.getConfig().getLong("storage.pool.connection-timeout-ms", 10000L));

        if (type == StorageType.SQLITE) {
            File databaseFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite.file", "storage/crossroads.db"));
            File parent = databaseFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
        } else {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "crossroads");
            boolean ssl = plugin.getConfig().getBoolean("storage.mysql.use-ssl", false);
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8");
            hikari.setUsername(plugin.getConfig().getString("storage.mysql.username", "root"));
            hikari.setPassword(plugin.getConfig().getString("storage.mysql.password", ""));
        }

        this.dataSource = new HikariDataSource(hikari);
        createSchema();
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        String sql = "SELECT data FROM crossroads_players WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new PlayerData(uuid);
                }

                YamlConfiguration yaml = PlayerDataCodec.fromText(resultSet.getString("data"));
                return PlayerDataCodec.deserializePlayer(uuid, yaml);
            }
        } catch (SQLException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to load player data for " + uuid + ".", exception);
            return new PlayerData(uuid);
        }
    }

    @Override
    public void savePlayerData(PlayerData data) {
        String sql = "INSERT INTO crossroads_players (uuid, last_known_name, last_join_at, last_quit_at, data) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT(uuid) DO UPDATE SET last_known_name = excluded.last_known_name, last_join_at = excluded.last_join_at, "
            + "last_quit_at = excluded.last_quit_at, data = excluded.data";
        if (type == StorageType.MYSQL) {
            sql = "INSERT INTO crossroads_players (uuid, last_known_name, last_join_at, last_quit_at, data) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE last_known_name = VALUES(last_known_name), last_join_at = VALUES(last_join_at), "
                + "last_quit_at = VALUES(last_quit_at), data = VALUES(data)";
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setString(2, data.getLastKnownName());
            statement.setLong(3, data.getLastJoinAt());
            statement.setLong(4, data.getLastQuitAt());
            statement.setString(5, PlayerDataCodec.toText(PlayerDataCodec.serializePlayer(data)));
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save player data for " + data.getUuid() + ".", exception);
        }
    }

    @Override
    public Map<String, SavedLocation> loadWarps() {
        Map<String, SavedLocation> warps = new HashMap<>();
        String sql = "SELECT warp_name, data FROM crossroads_warps";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                YamlConfiguration yaml = PlayerDataCodec.fromText(resultSet.getString("data"));
                SavedLocation location = PlayerDataCodec.deserializeLocation(yaml, "location");
                if (location != null) {
                    warps.put(resultSet.getString("warp_name").toLowerCase(), location);
                }
            }
        } catch (SQLException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to load warps from storage.", exception);
        }
        return warps;
    }

    @Override
    public void saveWarps(Map<String, SavedLocation> warps) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM crossroads_warps")) {
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO crossroads_warps (warp_name, data) VALUES (?, ?)")) {
                    for (Map.Entry<String, SavedLocation> entry : warps.entrySet()) {
                        insert.setString(1, entry.getKey());
                        insert.setString(2, PlayerDataCodec.toText(PlayerDataCodec.serializeLocation(entry.getValue())));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save warps to storage.", exception);
        }
    }

    @Override
    public SavedLocation loadSpawn() {
        String sql = "SELECT value FROM crossroads_server_state WHERE state_key = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "spawn");
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                YamlConfiguration yaml = PlayerDataCodec.fromText(resultSet.getString("value"));
                return PlayerDataCodec.deserializeLocation(yaml, "location");
            }
        } catch (SQLException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to load spawn from storage.", exception);
            return null;
        }
    }

    @Override
    public void saveSpawn(SavedLocation spawn) {
        String sql = "INSERT INTO crossroads_server_state (state_key, value) VALUES (?, ?) "
            + "ON CONFLICT(state_key) DO UPDATE SET value = excluded.value";
        if (type == StorageType.MYSQL) {
            sql = "INSERT INTO crossroads_server_state (state_key, value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE value = VALUES(value)";
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "spawn");
            statement.setString(2, PlayerDataCodec.toText(PlayerDataCodec.serializeLocation(spawn)));
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save spawn to storage.", exception);
        }
    }

    @Override
    public void appendModerationLog(ModerationLogEntry entry) {
        String sql = "INSERT INTO crossroads_moderation_logs (occurred_at, action, actor_uuid, actor_name, target_uuid, target_name, details) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entry.getOccurredAt());
            statement.setString(2, entry.getAction());
            setUuid(statement, 3, entry.getActorUuid());
            statement.setString(4, entry.getActorName());
            setUuid(statement, 5, entry.getTargetUuid());
            statement.setString(6, entry.getTargetName());
            statement.setString(7, entry.getDetails());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to save moderation log entry.", exception);
        }
    }

    @Override
    public List<ModerationLogEntry> loadModerationLogs(UUID targetUuid, String targetName, int limit) {
        List<ModerationLogEntry> entries = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT occurred_at, action, actor_uuid, actor_name, target_uuid, target_name, details "
                + "FROM crossroads_moderation_logs "
        );
        if (targetUuid != null || (targetName != null && !targetName.isBlank())) {
            sql.append("WHERE ");
            if (targetUuid != null) {
                sql.append("target_uuid = ? ");
            } else {
                sql.append("LOWER(target_name) = LOWER(?) ");
            }
        }
        sql.append("ORDER BY occurred_at DESC LIMIT ?");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (targetUuid != null) {
                statement.setString(index++, targetUuid.toString());
            } else if (targetName != null && !targetName.isBlank()) {
                statement.setString(index++, targetName);
            }
            statement.setInt(index, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new ModerationLogEntry(
                        resultSet.getLong("occurred_at"),
                        resultSet.getString("action"),
                        parseUuid(resultSet.getString("actor_uuid")),
                        resultSet.getString("actor_name"),
                        parseUuid(resultSet.getString("target_uuid")),
                        resultSet.getString("target_name"),
                        resultSet.getString("details")
                    ));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Unable to load moderation logs.", exception);
        }
        return entries;
    }

    @Override
    public StorageType getType() {
        return type;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void createSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS crossroads_players ("
                + "uuid VARCHAR(36) PRIMARY KEY, "
                + "last_known_name VARCHAR(64), "
                + "last_join_at BIGINT NOT NULL DEFAULT 0, "
                + "last_quit_at BIGINT NOT NULL DEFAULT 0, "
                + "data " + textColumn() + " NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS crossroads_warps ("
                + "warp_name VARCHAR(64) PRIMARY KEY, "
                + "data " + textColumn() + " NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS crossroads_server_state ("
                + "state_key VARCHAR(64) PRIMARY KEY, "
                + "value " + textColumn() + " NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS crossroads_moderation_logs ("
                + "id " + autoIdColumn() + ", "
                + "occurred_at BIGINT NOT NULL, "
                + "action VARCHAR(32) NOT NULL, "
                + "actor_uuid VARCHAR(36), "
                + "actor_name VARCHAR(64) NOT NULL, "
                + "target_uuid VARCHAR(36), "
                + "target_name VARCHAR(64) NOT NULL, "
                + "details " + textColumn() + " NOT NULL)");
        }
    }

    private String textColumn() {
        return type == StorageType.MYSQL ? "LONGTEXT" : "TEXT";
    }

    private String autoIdColumn() {
        return type == StorageType.MYSQL ? "BIGINT PRIMARY KEY AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    private void setUuid(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, uuid.toString());
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
