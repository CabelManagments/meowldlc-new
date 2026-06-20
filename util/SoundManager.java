package util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Простой плеер для звуков мода: hit.ogg / kill.ogg / enable.ogg / disable.ogg.
 * Сами .ogg регистрируются через resources/assets/meowldlc/sounds.json
 * (в build.gradle есть .mp3 -> .ogg конвертация на этапе CI, файлы
 * исходно лежат как .mp3, в репозитории/джаре оказываются .ogg).
 */
public final class SoundManager {

    public static final Identifier HIT = id("hit");
    public static final Identifier KILL = id("kill");
    public static final Identifier ENABLE = id("enable");
    public static final Identifier DISABLE = id("disable");

    private SoundManager() {
    }

    public static void init() {
        // Здесь специально ничего не регистрируем программно — события звука
        // (SoundEvent) подтягиваются автоматически из sounds.json при старте,
        // достаточно, чтобы файл лежал в resources/assets/meowldlc/sounds.json
        // и ссылался на правильные id.
    }

    public static void playHit() {
        play(HIT, 1.0f);
    }

    public static void playKill() {
        play(KILL, 1.0f);
    }

    public static void playEnable() {
        play(ENABLE, 0.6f);
    }

    public static void playDisable() {
        play(DISABLE, 0.6f);
    }

    private static void play(Identifier id, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        SoundEvent event = Registries.SOUND_EVENT.get(id);
        if (event == null) {
            // sounds.json ещё не зарегистрировал id — например, файл не на месте.
            return;
        }

        mc.player.playSound(event, volume, 1.0f);
    }

    private static Identifier id(String path) {
        return Identifier.of("meowldlc", path);
    }
}
