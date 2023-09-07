package kor.toxicity.mythicspawner.config;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.spawning.spawners.MythicSpawner;
import io.lumine.mythic.core.spawning.spawners.SpawnerManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static kor.toxicity.mythicspawner.MythicSpawner.*;

public class Spawner {
    private final MythicBukkit bukkit = MythicBukkit.inst();
    private final HashedSpawner mythicSpawner;
    private final long regenTime;
    private final Map<Long,Announce> announceMap = new HashMap<>();
    private boolean spawned = false;
    private long regenLeft;
    private Announce deathAnnounce;
    public Spawner(ConfigurationSection section) {
        var name = Objects.requireNonNull(section.getString("name"),"The key \"name\" does not exist.");
        bukkit.getMobManager().getMythicMob(name).orElseThrow(() -> new RuntimeException("The mob \"" + name + "\" doesn't exist."));
        var location = Objects.requireNonNull(section.getConfigurationSection("location"), "The key \"location\" does not exist.");
        var worldName = Objects.requireNonNull(location.getString("world"), "The key \"world\" does not exist in location.");
        var world = Objects.requireNonNull(Bukkit.getWorld(worldName), "The world \"" + worldName + "\" doesn't exist.");
        var spawnerName = Optional.ofNullable(section.getString("spawner-name")).orElse(new String(Base64.getEncoder().encode(name.getBytes(StandardCharsets.UTF_8))));
        mythicSpawner = new HashedSpawner(
                bukkit.getSpawnerManager(),
                spawnerName,
                new AbstractLocation(
                        BukkitAdapter.adapt(world),
                        location.getDouble("x"),
                        location.getDouble("y"),
                        location.getDouble("z"),
                        (float) location.getDouble("yaw"),
                        (float) location.getDouble("pitch")
                ),
                name
        );
        regenTime = Math.max(section.getLong("regen-time",60),1);
        regenLeft = regenTime;
        var announceConfig = section.getConfigurationSection("announce");
        if (announceConfig != null) {
            announceConfig.getKeys(false).forEach(s -> {
                var sec = announceConfig.getConfigurationSection(s);
                if (sec != null) {
                    try {
                        announceMap.put(Long.parseLong(s), new Announce(sec));
                    } catch (NullPointerException e) {
                        warn("Unable to load announce: " + s);
                        var msg = e.getMessage();
                        warn("Reason: " + (msg != null ? msg : "unknown"));
                    } catch (NumberFormatException e) {
                        warn("This is not an integer: " + s);
                    }
                }
            });
        }
        var deathConfig = section.getConfigurationSection("death-announce");
        if (deathConfig != null) {
            try {
                deathAnnounce = new Announce(deathConfig);
            } catch (Exception e) {
                warn("Unable to load death announce");
                var msg = e.getMessage();
                warn("Reason: " + (msg != null ? msg : "unknown"));
            }
        }
        bukkit.getSpawnerManager().listSpawners.remove(mythicSpawner);
    }
    public String getName() {
        return mythicSpawner.getName();
    }

    public void deathAnnounce() {
        if (deathAnnounce != null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                deathAnnounce.announce(onlinePlayer);
            }
        }
    }

    public Announce update() {
        if (spawned) return null;
        regenLeft--;
        if (regenLeft == 0) {
            spawned = true;
            bukkit.getSpawnerManager().listSpawners.add(mythicSpawner);
        }
        return announceMap.get(regenLeft);
    }
    public void initialize() {
        spawned = false;
        regenLeft = regenTime;
        bukkit.getSpawnerManager().listSpawners.remove(mythicSpawner);
    }

    public boolean isSpawned() {
        return spawned;
    }

    private static class HashedSpawner extends MythicSpawner {

        public HashedSpawner(SpawnerManager manager, String name, AbstractLocation location, String mobName) {
            super(manager, name, location, mobName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof MythicSpawner spawner) return getName().equals(spawner.getName());
            return false;
        }
    }
}
