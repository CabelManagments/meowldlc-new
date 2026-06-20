import net.fabricmc.api.ClientModInitializer;
import modules.IModule;
import modules.JumpCircleModule;
import modules.HitParticlesModule;
import modules.TargetESPModule;
import util.SoundManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Точка входа клиентского мода. Лежит в корне репо без package —
 * см. sourceSets в build.gradle: srcDirs = ['.'] + явные include.
 *
 * Регистрируются только модули без игрового преимущества (косметика).
 * TargetESP / KillAura / автобай с аукциона сюда сознательно не добавлены —
 * обсуждали в чате почему.
 */
public class CheatMod implements ClientModInitializer {

    public static final String MOD_ID = "meowldlc";

    private static CheatMod instance;
    private final List<IModule> modules = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        instance = this;

        SoundManager.init();

        registerModule(new JumpCircleModule());
        registerModule(new HitParticlesModule());
        registerModule(new TargetESPModule());
    }

    private void registerModule(IModule module) {
        modules.add(module);
    }

    public List<IModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public IModule getModule(String name) {
        for (IModule m : modules) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public static CheatMod getInstance() {
        return instance;
    }
}
