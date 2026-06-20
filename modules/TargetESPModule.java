package modules;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Чисто косметический модуль: подсвечивает сущность, на которую СМОТРИТ
 * игрок (ванильный crosshair-рейкаст через {@code mc.targetedEntity}).
 * Никакой собственной логики "видеть через стены"/трекать невидимые
 * сущности тут нет — рейкаст и так уважает LOS, мы просто рисуем эффект
 * на результате, который ванильный клиент и так выдаёт.
 *
 * Два режима:
 *  - MARKER: простая пульсирующая рамка-куб вокруг сущности.
 *  - CRYSTALS: несколько кубиков, вращающихся орбитой вокруг сущности
 *    (визуально как декоративные кристаллы).
 */
public class TargetESPModule implements IModule {

    public enum Mode { MARKER, CRYSTALS }

    private boolean enabled = false;
    private boolean listenerRegistered = false;

    private Mode mode = Mode.CRYSTALS;
    private int crystalCount = 14;
    private float orbitSpeed = 3.0f;
    private boolean keepLastPosition = true;

    private final List<Crystal> crystals = new ArrayList<>();
    private final Random random = new Random();
    private Entity lastTarget = null;
    private float tick = 0f;
    private Vector3f lastPos = null;

    @Override
    public String getName() {
        return "TargetESP";
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
            WorldRenderEvents.AFTER_ENTITIES.register(this::onRenderWorld);
            listenerRegistered = true;
        }
        if (!enabled) {
            crystals.clear();
            lastTarget = null;
        }
    }

    @Override
    public void onTick() {
        if (!enabled) return;
        tick += orbitSpeed;

        MinecraftClient mc = MinecraftClient.getInstance();
        Entity target = (mc.targetedEntity instanceof LivingEntity living) ? living : null;

        if (target != lastTarget) {
            if (target != null) {
                generateCrystals(target);
                lastPos = new Vector3f((float) target.getX(), (float) target.getY(), (float) target.getZ());
            } else if (!keepLastPosition) {
                crystals.clear();
                lastPos = null;
            }
            lastTarget = target;
        }
    }

    private void onRenderWorld(WorldRenderContext context) {
        if (!enabled || crystals.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Entity target = lastTarget;

        Vector3f origin;
        if (target != null) {
            origin = new Vector3f((float) target.getX(), (float) target.getY(), (float) target.getZ());
            lastPos = origin;
        } else if (keepLastPosition && lastPos != null) {
            origin = lastPos;
        } else {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        var camera = context.camera().getPos();
        float baseHeight = target != null ? target.getHeight() : 1.8f;

        matrices.push();
        matrices.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);

        switch (mode) {
            case MARKER -> renderMarker(matrices, baseHeight);
            case CRYSTALS -> renderCrystals(matrices, baseHeight);
        }

        matrices.pop();
    }

    private void renderMarker(MatrixStack ms, float height) {
        ms.push();
        ms.translate(0, height / 2.0, 0);
        float pulse = 1.0f + (float) Math.sin(tick * 0.05) * 0.05f;
        ms.scale(pulse, pulse, pulse);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        float w = 0.45f, h = 0.9f;
        float[] c = {1f, 1f, 1f, 0.85f};

        drawBoxOutline(buffer, m, w, h, c);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
        ms.pop();
    }

    private void drawBoxOutline(BufferBuilder b, Matrix4f m, float w, float h, float[] c) {
        float[][] corners = {
                {-w, -h, -w}, {w, -h, -w}, {w, -h, w}, {-w, -h, w},
                {-w, h, -w}, {w, h, -w}, {w, h, w}, {-w, h, w}
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] e : edges) {
            float[] a = corners[e[0]];
            float[] bb = corners[e[1]];
            b.vertex(m, a[0], a[1], a[2]).color(c[0], c[1], c[2], c[3]);
            b.vertex(m, bb[0], bb[1], bb[2]).color(c[0], c[1], c[2], c[3]);
        }
    }

    private void renderCrystals(MatrixStack ms, float height) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (Crystal crystal : crystals) {
            float floatAnim = (float) Math.sin(Math.toRadians(tick + crystal.index * 35)) * 0.06f;
            float orbitAngle = crystal.baseAngle + tick * 0.02f;

            double rx = Math.cos(orbitAngle) * crystal.radius;
            double ry = crystal.heightFraction * height + floatAnim;
            double rz = Math.sin(orbitAngle) * crystal.radius;

            renderCrystalCube(ms, rx, ry, rz, crystal);
        }

        RenderSystem.disableBlend();
    }

    private void renderCrystalCube(MatrixStack ms, double x, double y, double z, Crystal crystal) {
        ms.push();
        ms.translate(x, y, z);
        float s = crystal.scale * 0.12f;
        ms.scale(s, s, s);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(crystal.rotY + tick * 1.5f));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(crystal.rotX));

        float[] c = hueColor(crystal.index * 30, 0.8f);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        float w = 1.0f, h = 2.0f;

        drawFace(buffer, m, 0, h, 0, -w, 0, w, w, 0, w, c);
        drawFace(buffer, m, 0, h, 0, w, 0, w, w, 0, -w, c);
        drawFace(buffer, m, 0, h, 0, w, 0, -w, -w, 0, -w, c);
        drawFace(buffer, m, 0, h, 0, -w, 0, -w, -w, 0, w, c);
        drawFace(buffer, m, 0, -h, 0, w, 0, w, -w, 0, w, c);
        drawFace(buffer, m, 0, -h, 0, w, 0, -w, w, 0, w, c);
        drawFace(buffer, m, 0, -h, 0, -w, 0, -w, w, 0, -w, c);
        drawFace(buffer, m, 0, -h, 0, -w, 0, w, -w, 0, -w, c);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void drawFace(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float[] c) {
        b.vertex(m, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        b.vertex(m, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        b.vertex(m, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
    }

    /** Простой HSB-градиент без зависимости от java.awt.Color. */
    private float[] hueColor(float hueDegrees, float alpha) {
        float hue = (hueDegrees % 360f) / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.65f, 1.0f);
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f,
                alpha
        };
    }

    private void generateCrystals(Entity target) {
        crystals.clear();
        float height = target.getHeight();
        float width = target.getWidth();
        float baseRadius = Math.max(0.75f, width * 0.9f + 0.35f);
        float minY = 0.15f, maxY = Math.max(minY + 0.2f, height - 0.15f);

        for (int i = 0; i < crystalCount; i++) {
            float angle = (float) ((double) i / crystalCount * Math.PI * 2.0);
            float yFraction = minY + (maxY - minY) * (i % 3) / 2f;
            crystals.add(new Crystal(
                    yFraction / Math.max(height, 0.01f),
                    20f + random.nextFloat() * 40f,
                    random.nextFloat() * 360f,
                    0.75f + random.nextFloat() * 0.35f,
                    i, angle, baseRadius + (random.nextFloat() - 0.5f) * 0.18f
            ));
        }
    }

    public void setMode(Mode mode) { this.mode = mode; }
    public void setCrystalCount(int count) { this.crystalCount = Math.max(1, count); }
    public void setOrbitSpeed(float speed) { this.orbitSpeed = speed; }
    public void setKeepLastPosition(boolean keep) { this.keepLastPosition = keep; }

    private static class Crystal {
        final float heightFraction, rotX, rotY, scale, baseAngle, radius;
        final int index;

        Crystal(float heightFraction, float rotX, float rotY, float scale, int index, float baseAngle, float radius) {
            this.heightFraction = heightFraction;
            this.rotX = rotX;
            this.rotY = rotY;
            this.scale = scale;
            this.index = index;
            this.baseAngle = baseAngle;
            this.radius = radius;
        }
    }
}
