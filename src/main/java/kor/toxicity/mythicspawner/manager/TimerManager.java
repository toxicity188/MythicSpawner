package kor.toxicity.mythicspawner.manager;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import kor.toxicity.mythicspawner.MythicSpawner;
import kor.toxicity.mythicspawner.config.Spawner;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class TimerManager implements MythicSpawnerManager {

    private BukkitTask scheduler;

    @Override
    public void start(MythicSpawner spawner) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void death(MythicMobDeathEvent event) {
                var s = event.getMob().getSpawner();
                if (s != null) {
                    var spawn = ConfigManager.getSpawner(s.getName());
                    if (spawn != null) {
                        spawn.deathAnnounce();
                        spawn.initialize();
                    }
                }
            }
        },spawner);
        spawner.getCommandAPI()
                .create("update")
                .setAliases(new String[]{"u"})
                .setDescription("update all spawners.")
                .setUsage("update [time]")
                .setPermission(new String[]{"mythicspawner.update"})
                .setExecutor((c,a) -> {
                    var time = 1;
                    if (a.length > 0) {
                        try {
                            time = Integer.parseInt(a[0]);
                        } catch (NumberFormatException e) {
                            spawner.getCommandAPI().message(c,"wrong integer format: " + a[0]);
                        }
                    }
                    var iterator = ConfigManager.getSpawners().iterator();
                    for (int i = 0; i < time; i++) {
                        while (iterator.hasNext()) {
                            var next = iterator.next();
                            if (next.isSpawned()) iterator.remove();
                            else next.update();
                        }
                    }
                    spawner.getCommandAPI().message(c,"update finished.");
                })
                .build()
                ;
    }

    public static void update() {
        var announceList = ConfigManager.getSpawners().stream().map(Spawner::update).filter(Objects::nonNull).toList();
        if (!announceList.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(p -> announceList.forEach(a -> a.announce(p)));
        }
    }

    @Override
    public void reload(MythicSpawner spawner) {
        if (scheduler != null) scheduler.cancel();
        scheduler = Bukkit.getScheduler().runTaskTimerAsynchronously(spawner,TimerManager::update,20L,20L);
    }

    @Override
    public void end(MythicSpawner spawner) {

    }
}
