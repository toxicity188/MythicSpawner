package kor.toxicity.mythicspawner.spawner;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.spawning.spawners.MythicSpawner;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public class Spawner {
    private static long id = 0;
    private final MythicBukkit bukkit = MythicBukkit.inst();
    private final MythicSpawner mythicSpawner;
    public Spawner(ConfigurationSection section) {
        var name = Objects.requireNonNull(section.getString("name"),"The key \"name\" does not exist.");
        Objects.requireNonNull(bukkit.getMobManager().getMythicMob(name),"The mob \"" + name + "\" doesn't exist.");
        var location = Objects.requireNonNull(section.getConfigurationSection("location"), "The key \"location\" does not exist.");
        var worldName = Objects.requireNonNull(location.getString("world"), "The key \"world\" does not exist in location.");
        var world = Objects.requireNonNull(Bukkit.getWorld(worldName), "The world \"" + worldName + "\" doesn't exist.");
        var spawnerName = Optional.ofNullable(section.getString("spawner-name")).orElse(new String(Base64.getEncoder().encode(name.getBytes(StandardCharsets.UTF_8))) + "_" + (++id));
        mythicSpawner = new MythicSpawner(
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
    }

    public void spawn() {
        bukkit.getSpawnerManager().addSpawnerToChunkLookupTable(mythicSpawner);
    }
    public void remove() {
        bukkit.getSpawnerManager().removeSpawnerFromChunkLookupTable(mythicSpawner);
    }
}
