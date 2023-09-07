package kor.toxicity.mythicspawner;

import kor.toxicity.mythicspawner.manager.ConfigManager;
import kor.toxicity.mythicspawner.manager.MythicSpawnerManager;
import kor.toxicity.mythicspawner.manager.TimerManager;
import kor.toxicity.toxicitylibs.api.command.CommandAPI;
import kor.toxicity.toxicitylibs.plugin.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class MythicSpawner extends JavaPlugin {
    private static MythicSpawner spawner;
    private static final List<MythicSpawnerManager> MANAGERS = new ArrayList<>();
    public static void send(String message) {
        spawner.getLogger().info(message);
    }
    public static void warn(String message) {
        spawner.getLogger().warning(message);
    }
    @Override
    public void onEnable() {
        spawner = this;
        var manager = Bukkit.getPluginManager();
        for (String s : new String[]{"ToxicityLibs", "MythicMobs"}) {
            if (!manager.isPluginEnabled(s)) {
                warn("Plugin not found: " + s);
                manager.disablePlugin(this);
            }
        }
        MANAGERS.add(new ConfigManager());
        MANAGERS.add(new TimerManager());

        var command = getCommand("mythicmobsspawner");
        if (command != null) command.setExecutor(commandAPI.createTabExecutor());

        MANAGERS.forEach(m -> m.start(this));
        Bukkit.getScheduler().runTask(this,() -> {
            load();
            send("Plugin enabled.");
        });
    }
    private void load() {
        MANAGERS.forEach(m -> m.reload(this));
    }

    private final CommandAPI commandAPI = new CommandAPI("<gradient:blue-aqua>[MythicMobsSpawner]")
            .setCommandPrefix("mms")
            .setUnknownCommandMessage("<color:red>unknown command. type /mms help to find command.")
            .setNotCommandMessage("<color:red>/mms help to find command.")

            .getHelpBuilder()
            .setPermission(new String[]{"mythicspawner.help"})
            .setDescription("show all sub-command in MythicMobsSpawner.")
            .build()

            .create("reload")
            .setAliases(new String[]{"re","rl"})
            .setDescription("reload this plugin.")
            .setUsage("reload")
            .setPermission(new String[]{"mythicspawner.reload"})
            .setExecutor((c,a) -> Bukkit.getScheduler().runTaskAsynchronously(MythicSpawner.this,() -> {
                var time = System.currentTimeMillis();
                load();
                getCommandAPI().message(c,"reload completed. (" + (System.currentTimeMillis() - time) + " ms)");
            }))
            .build()
            ;

    public CommandAPI getCommandAPI() {
        return commandAPI;
    }

    public void loadFolder(String dir, BiConsumer<File, ConfigurationSection> consumer) {
        var dataFolder = getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdir();
        var folder = new File(dataFolder,dir);
        if (!folder.exists()) folder.mkdir();
        var list = folder.listFiles();
        if (list != null) for (File file : list) {
            var name = StringUtil.getFileName(file);
            if (name.extension().equals("yml")) {
                try {
                    var yaml = new YamlConfiguration();
                    yaml.load(file);
                    consumer.accept(file,yaml);
                } catch (Exception e) {
                    warn("unable to load this file: " + file.getName());
                    var msg = e.getMessage();
                    warn("reason: " + (msg != null ? msg : "unknown"));
                }
            }
        }
    }


    @Override
    public void onDisable() {
        MANAGERS.forEach(m -> m.end(this));
        send("Plugin disabled.");
    }
}
