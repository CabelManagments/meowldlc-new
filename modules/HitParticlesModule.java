package modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Чисто косметический модуль: рисует немного частиц в точке удара,
 * когда ЛЮБАЯ сущность получает урон (не только от игрока). Никакой
 * игровой информации, которой не было бы видно и так, не добавляет —
 * урон и так вызывает тряску/анимацию у ванильной сущности.
 */
public class HitParticlesModule implements IModule {

    private final Random random = new Random();
    private boolean enabled = false;
    private boolean listenerRegistered = false;

    @Override
    public String getName() {
        return "HitParticles";
    }

    @Override
    public String getCategory() {
        return "Visual";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled && !listenerRegistered) {
            registerListener();
            listenerRegistered = true;
        }
    }

    private void registerListener() {
        // ServerLivingEntityEvents.AFTER_DAMAGE — серверное событие, но т.к.
        // в синглплеере/интегрированном сервере у нас есть доступ к клиенту,
        // используем его для определения позиции. На чужом дедике это
        // событие просто не сработает на клиенте — это нормально, частицы
        // тогда рисуются только по факту визуального "хёрта" сущности (см.
        // HurtEffectMixin, если понадобится клиентский вариант — отдельно).
        ServerLivingEntityEvents.AFTER_DAMAGE.register(this::onAfterDamage);
    }

    private void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken,
                                float damageTaken, boolean blocked) {
        if (!enabled) return;
        spawnHitParticles(entity.getPos().add(0, entity.getHeight() / 2.0, 0));
    }

    private void spawnHitParticles(Vec3d pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        for (int i = 0; i < 6; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.4;
            double oy = (random.nextDouble() - 0.5) * 0.4;
            double oz = (random.nextDouble() - 0.5) * 0.4;

            mc.world.addParticle(
                    ParticleTypes.CRIT,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    0.0, 0.0, 0.0
            );
        }
    }
}
