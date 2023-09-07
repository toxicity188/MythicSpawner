package kor.toxicity.mythicspawner.config;

import kor.toxicity.toxicitylibs.api.ComponentReader;
import kor.toxicity.toxicitylibs.api.ReaderBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class Announce {
    private final List<ComponentReader<Player>> playerComponentReader;
    private Sound sound;
    public Announce(ConfigurationSection section) {

        playerComponentReader = section.getStringList("message").stream().map(s -> ReaderBuilder.placeholder(s).build()).toList();

        var soundConfig = section.getString("sound");
        if (soundConfig != null) {
            var split = soundConfig.split(",");
            if (split.length == 3) {
                try {
                    sound = new Sound(
                            split[0],
                            Float.parseFloat(split[1]),
                            Float.parseFloat(split[2])
                    );
                } catch (Exception e) {
                    throw new NullPointerException("unable to load sound data");
                }
            }
        }
    }

    public void announce(Player player) {
        for (ComponentReader<Player> componentReader : playerComponentReader) {
            player.sendMessage(componentReader.getResult(player));
        }
        if (sound != null) sound.play(player);
    }


    private record Sound(String name, float volume, float pitch) {
        public void play(Player player) {
            player.playSound(player.getLocation(),name,volume,pitch);
        }
    }
}
