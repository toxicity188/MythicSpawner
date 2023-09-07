package kor.toxicity.mythicspawner.manager;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicPostReloadedEvent;
import kor.toxicity.mythicspawner.MythicSpawner;
import kor.toxicity.mythicspawner.config.Spawner;
import kor.toxicity.toxicitylibs.api.command.SenderType;
import kor.toxicity.toxicitylibs.plugin.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.*;

import static kor.toxicity.mythicspawner.MythicSpawner.*;

public class ConfigManager implements MythicSpawnerManager {

    private static final Map<String,Spawner> SPAWNERS = new HashMap<>();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void start(MythicSpawner spawner) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void reload(MythicPostReloadedEvent event) {
                ConfigManager.this.reload(spawner);
            }
        },spawner);
        spawner.getCommandAPI()
                .create("initialize")
                .setAliases(new String[]{"init","i"})
                .setDescription("initialize all spawners.")
                .setUsage("initialize")
                .setPermission(new String[]{"mythicspawner.initialize"})
                .setExecutor((c,a) -> {
                    SPAWNERS.values().forEach(Spawner::initialize);
                    spawner.getCommandAPI().message(c,"Successfully initialized.");
                })
                .build()

                .create("create")
                .setAliases(new String[]{"c"})
                .setDescription("create a new spawner to use your current location.")
                .setUsage("create <file> <key> <mob name>")
                .setPermission(new String[]{"mythicspawner.create"})
                .setLength(3)
                .setAllowedSender(new SenderType[]{SenderType.PLAYER})
                .setExecutor((c,a) -> {
                    if (MythicBukkit.inst().getMobManager().getMythicMob(a[2]).isEmpty()) {
                        spawner.getCommandAPI().message(c,"The mob named \"" + a[2] + "\" doesn't exist.");
                    } else {
                        Bukkit.getScheduler().runTaskAsynchronously(spawner,() -> {
                            var loc = ((Player) c).getLocation();
                            var dataFolder = spawner.getDataFolder();
                            if (!dataFolder.exists()) dataFolder.mkdir();
                            var spawners = new File(dataFolder,"spawners");
                            if (!spawners.exists()) spawners.mkdir();
                            var file = new File(spawners,a[0] + ".yml");
                            try {
                                if (!file.exists()) file.createNewFile();
                                var yaml = new YamlConfiguration();
                                var finalConfig = new MemoryConfiguration();
                                var locConfig = new MemoryConfiguration();
                                locConfig.set("world",loc.getWorld().getName());
                                locConfig.set("x",loc.x());
                                locConfig.set("y",loc.y());
                                locConfig.set("z",loc.z());
                                locConfig.set("yaw",loc.getYaw());
                                locConfig.set("pitch",loc.getPitch());
                                finalConfig.set("name", a[2]);
                                finalConfig.set("regen-time",60);
                                finalConfig.set("location", locConfig);
                                yaml.load(file);
                                yaml.set(a[1],finalConfig);
                                yaml.save(file);
                                spawner.getCommandAPI().message(c,"Successfully saved.");
                            } catch (Exception e) {
                                spawner.getCommandAPI().message(c,"Unable to save the file.");
                            }
                        });
                    }
                })
                .setTabCompleter((c,a) -> switch (a.length) {
                    case 1 -> {
                        var listFile = new File(spawner.getDataFolder(),"spawners").listFiles();
                        yield listFile != null ? Arrays.stream(listFile).map(s -> StringUtil.getFileName(s).name()).filter(s -> s.startsWith(a[0])).toList() : null;
                    }
                    case 3 -> MythicBukkit.inst().getMobManager().getMobNames().stream().filter(s -> s.startsWith(a[2])).toList();
                    default -> null;
                })
                .build()
                ;
    }

    @Override
    public void reload(MythicSpawner spawner) {
        SPAWNERS.values().forEach(Spawner::initialize);
        SPAWNERS.clear();
        spawner.loadFolder("spawners",(f,s) -> s.getKeys(false).forEach(k -> {
            var config = s.getConfigurationSection(k);
            if (config != null) {
                try {
                    var spawner1 = new Spawner(config);
                    SPAWNERS.put(spawner1.getName(),spawner1);
                } catch (Exception e) {
                    var msg = e.getMessage();
                    warn("Unable to load this spawner: " + k + " in file " + f.getName());
                    warn("Reason: " + (msg != null ? msg : "unknown"));
                }
            }
        }));
        spawner.getCommandAPI().message(Bukkit.getConsoleSender(), SPAWNERS.size() + " of spawners has successfully loaded.");
    }
    public static List<Spawner> getSpawners() {
        return new ArrayList<>(SPAWNERS.values());
    }
    public static Spawner getSpawner(String name) {
        return SPAWNERS.get(name);
    }

    @Override
    public void end(MythicSpawner spawner) {

    }
}
