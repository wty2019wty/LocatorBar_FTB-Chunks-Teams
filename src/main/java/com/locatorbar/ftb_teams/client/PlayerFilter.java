package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.LocatorBarMod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端玩家过滤名单。
 * <p>
 * 隐藏名单保存在客户端配置 {@code hiddenPlayers} 中，每项支持三种写法：
 * <ul>
 *     <li>玩家名 —— 不区分大小写；</li>
 *     <li>UUID —— 精确匹配，玩家改名后依然有效；</li>
 *     <li>{@code UUID|玩家名} —— 游戏内过滤界面写入的形式，优先按 UUID 匹配。</li>
 * </ul>
 * 过滤只影响 HUD 显示，不会改变 FTB Teams 的队伍关系，也不会向服务端发送任何数据。
 */
public final class PlayerFilter {
    private PlayerFilter() {
    }

    /**
     * 配置中的一条隐藏记录。UUID 和玩家名至少有一个非空。
     */
    public record Entry(@Nullable UUID uuid, @Nullable String name) {
        public boolean matches(UUID playerId, @Nullable String playerName) {
            if (uuid != null) {
                // 同时记录了 UUID 和名字时，以 UUID 为准，避免同名玩家被误伤
                return uuid.equals(playerId);
            }
            return name != null && playerName != null && name.equalsIgnoreCase(playerName);
        }

        public String displayName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            return uuid == null ? "?" : uuid.toString();
        }

        public String identity() {
            if (uuid != null) {
                return "u:" + uuid;
            }
            return "n:" + (name == null ? "" : name.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * 一次 tick / 一次界面刷新内使用的不可变过滤视图，避免反复解析配置。
     */
    public record Filter(List<Entry> entries) {
        public boolean isHidden(UUID playerId, @Nullable String playerName) {
            for (Entry entry : entries) {
                if (entry.matches(playerId, playerName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 过滤界面中的一行。
     *
     * @param tracked true 表示该玩家当前能被定位条追踪；false 表示只存在于隐藏名单中
     */
    public record Candidate(@Nullable UUID uuid, @Nullable String name, boolean longRange, boolean tracked) {
        public String displayName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            return uuid == null ? "?" : uuid.toString();
        }

        public String identity() {
            if (uuid != null) {
                return "u:" + uuid;
            }
            return "n:" + (name == null ? "" : name.toLowerCase(Locale.ROOT));
        }
    }

    public static Filter load() {
        return new Filter(parseEntries());
    }

    /**
     * 过滤界面要展示的玩家：当前所有候选玩家（包括已隐藏的），
     * 再补上隐藏名单里目前没有被追踪到的记录，方便取消隐藏。
     */
    public static List<Candidate> candidates() {
        List<ClientTeamPositions.TrackedPlayer> tracked = ClientTeamPositions.candidates();
        Map<String, Candidate> result = new LinkedHashMap<>();

        for (ClientTeamPositions.TrackedPlayer player : tracked) {
            result.putIfAbsent("u:" + player.id(),
                    new Candidate(player.id(), player.name(), player.longRange(), true));
        }

        for (Entry entry : parseEntries()) {
            boolean alreadyListed = tracked.stream()
                    .anyMatch(player -> entry.matches(player.id(), player.name()));
            if (!alreadyListed) {
                result.putIfAbsent(entry.identity(),
                        new Candidate(entry.uuid(), entry.name(), false, false));
            }
        }

        List<Candidate> candidates = new ArrayList<>(result.values());
        candidates.sort(Comparator.comparing(
                candidate -> candidate.name() == null
                        ? "\uffff"
                        : candidate.name().toLowerCase(Locale.ROOT)));
        return candidates;
    }

    /**
     * 隐藏或显示一个玩家，并立即把更新后的名单写回客户端配置。
     */
    public static void setHidden(@Nullable UUID playerId, @Nullable String playerName, boolean hidden) {
        if (playerId == null && (playerName == null || playerName.isBlank())) {
            return;
        }

        List<String> entries = new ArrayList<>(ClientConfig.HIDDEN_PLAYERS.get());
        entries.removeIf(raw -> {
            Entry entry = parse(raw);
            if (entry == null) {
                return false;
            }
            if (entry.uuid() != null) {
                return playerId != null && entry.uuid().equals(playerId);
            }
            return playerName != null
                    && entry.name() != null
                    && entry.name().equalsIgnoreCase(playerName);
        });

        if (hidden) {
            if (playerId == null) {
                entries.add(playerName);
            } else if (playerName == null || playerName.isBlank()) {
                entries.add(playerId.toString());
            } else {
                entries.add(playerId + "|" + playerName);
            }
        }

        save(entries);
    }

    public static void clear() {
        save(List.of());
    }

    private static void save(List<String> entries) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry != null && !entry.isBlank()) {
                unique.add(entry.trim());
            }
        }
        ClientConfig.HIDDEN_PLAYERS.set(List.copyOf(unique));
        ClientConfig.SPEC.save();
        LocatorBarMod.LOGGER.debug("[LocatorBar] 玩家过滤名单已更新：{}", unique);
    }

    private static List<Entry> parseEntries() {
        List<Entry> entries = new ArrayList<>();
        for (String raw : ClientConfig.HIDDEN_PLAYERS.get()) {
            Entry entry = parse(raw);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    @Nullable
    private static Entry parse(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        int separator = value.indexOf('|');
        if (separator >= 0) {
            String left = value.substring(0, separator).trim();
            String right = value.substring(separator + 1).trim();

            UUID leftUuid = parseUuid(left);
            if (leftUuid != null) {
                return new Entry(leftUuid, right.isEmpty() ? null : right);
            }

            UUID rightUuid = parseUuid(right);
            if (rightUuid != null) {
                return new Entry(rightUuid, left.isEmpty() ? null : left);
            }
        }

        UUID uuid = parseUuid(value);
        return uuid != null ? new Entry(uuid, null) : new Entry(null, value);
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
