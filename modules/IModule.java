package modules;

/**
 * Базовый интерфейс для всех модулей мода.
 * Категории — просто строки для группировки в ClickGUI (Visual, Misc, ...),
 * без какой-то특별ной логики привязки.
 */
public interface IModule {

    String getName();

    String getCategory();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /** Вызывается один раз при включении модуля (например, проиграть enable.ogg). */
    default void onEnable() {
    }

    /** Вызывается один раз при выключении модуля (например, проиграть disable.ogg). */
    default void onDisable() {
    }

    /** Вызывается каждый клиентский тик, если модуль включён. */
    default void onTick() {
    }

    default void toggle() {
        setEnabled(!isEnabled());
        if (isEnabled()) {
            onEnable();
        } else {
            onDisable();
        }
    }
}
