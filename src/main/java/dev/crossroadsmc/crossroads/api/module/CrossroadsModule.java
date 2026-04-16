package dev.crossroadsmc.crossroads.api.module;

public interface CrossroadsModule {
    String getId();

    default void onLoad(CrossroadsModuleContext context) {
    }

    default void onEnable(CrossroadsModuleContext context) {
    }

    default void onDisable(CrossroadsModuleContext context) {
    }
}
