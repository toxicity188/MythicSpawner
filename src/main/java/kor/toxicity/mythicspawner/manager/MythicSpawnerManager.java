package kor.toxicity.mythicspawner.manager;

import kor.toxicity.mythicspawner.MythicSpawner;

public interface MythicSpawnerManager {
    void start(MythicSpawner spawner);
    void reload(MythicSpawner spawner);
    void end(MythicSpawner spawner);
}
