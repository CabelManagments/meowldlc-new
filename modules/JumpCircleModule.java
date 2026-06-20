package modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Чисто косметический модуль: когда игрок в воздухе, рисует на земле
 * окружность в точке предсказанного приземления (баллистика по motionY/g).
 * Никакого автоматического действия не выполняет, преимущества не даёт —
 * это просто визуальная подсказка для самого игрока, как индикатор
 * приземления в паркур-моде.
 *
 * Рисуем линиями (RenderLayer.getLines()), а не залитым полигоном —
 * как и раньше, так проще избежать сортировки прозрачности.
 *
 * ВАЖНО: метод normal() у VertexConsumer в 1.21.4 принимает
 * MatrixStack.Entry, а не "голую" матрицу нормалей — учтено ниже.
 */
public class JumpCircleModule implements IModule {

    private static final double GRAVITY = 0.08; // ускорение свободного падения тика в МК
    private static final int SEGMENTS = 32;

    private boolean enabled = false;
    private float radius = 0.5f;
    private int color = 0x40FFFFFF; // ARGB, полупрозрачный белый

    @Override
    public String getName() {
        return "JumpCircle";
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
        if (enabled) {
            WorldRenderEvents.AFTER_ENTITIES.register(this::onRenderWorld);
        }
        // примечание: для простоты регистрируем обработчик один раз и просто
        // проверяем isEnabled() внутри. Двойная регистрация на повторных
        // toggle() безопасна с точки зрения логики, но в проде лучше хранить
        // ссылку на listener и снимать регистрацию в onDisable().
    }

    private void onRenderWorld(WorldRenderContext context) {
        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.isOnGround()) return;

        Vec3d landing = predictLanding(mc.player);
        if (landing == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider provider = context.consumers();
        if (matrices == null || provider == null) return;

        Vec3d camera = context.camera().getPos();

        matrices.push();
        matrices.translate(landing.x - camera.x, landing.y - camera.y, landing.z - camera.z);

        VertexConsumer buffer = provider.getBuffer(RenderLayer.getLines());
        drawCircleOutline(buffer, matrices, radius, color);

        matrices.pop();
    }

    /**
     * Простая баллистика: вперёд по времени интегрируем motionY с учётом
     * гравитации до пересечения с текущей высотой блока под игроком.
     * Не идеально точно повторяет ванильную физику на гранях блоков,
     * но для визуальной подсказки достаточно.
     */
    private Vec3d predictLanding(Entity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double vy = player.getVelocity().y;
        double vx = player.getVelocity().x;
        double vz = player.getVelocity().z;

        double groundY = findGroundY(player);
        if (Double.isNaN(groundY)) return null;

        for (int tick = 0; tick < 200; tick++) {
            vy -= GRAVITY;
            vy *= 0.98; // примерное затухание, как у ванильного motionY
            x += vx;
            z += vz;
            y += vy;
            if (y <= groundY) {
                return new Vec3d(x, groundY, z);
            }
        }
        return null;
    }

    private double findGroundY(Entity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return Double.NaN;

        int x = MathHelper.floor(player.getX());
        int z = MathHelper.floor(player.getZ());
        int startY = MathHelper.floor(player.getY());

        for (int y = startY; y > mc.world.getBottomY(); y--) {
            if (!mc.world.getBlockState(new net.minecraft.util.math.BlockPos(x, y, z)).isAir()) {
                return y + 1.0;
            }
        }
        return Double.NaN;
    }

    private void drawCircleOutline(VertexConsumer buffer, MatrixStack matrices, float r, int argb) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();

        float a = ((argb >> 24) & 0xFF) / 255f;
        float rr = ((argb >> 16) & 0xFF) / 255f;
        float gg = ((argb >> 8) & 0xFF) / 255f;
        float bb = (argb & 0xFF) / 255f;

        for (int i = 0; i < SEGMENTS; i++) {
            double angle1 = (Math.PI * 2 * i) / SEGMENTS;
            double angle2 = (Math.PI * 2 * (i + 1)) / SEGMENTS;

            float x1 = (float) (Math.cos(angle1) * r);
            float z1 = (float) (Math.sin(angle1) * r);
            float x2 = (float) (Math.cos(angle2) * r);
            float z2 = (float) (Math.sin(angle2) * r);

            buffer.vertex(pos, x1, 0.02f, z1).color(rr, gg, bb, a).normal(entry, 0f, 1f, 0f);
            buffer.vertex(pos, x2, 0.02f, z2).color(rr, gg, bb, a).normal(entry, 0f, 1f, 0f);
        }
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setColor(int argb) {
        this.color = argb;
    }
}
