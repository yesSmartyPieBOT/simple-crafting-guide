package com.yessmartypie.simplecraftingguide;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class CraftingGuideKeybind {
    private static KeyBinding openGuideKey;

    private CraftingGuideKeybind() {
    }

    public static void register() {
        openGuideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_crafting_guide.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.MISC,
                0
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuideKey.wasPressed()) {
                MinecraftClient current = MinecraftClient.getInstance();
                if (current.player != null && current.world != null) {
                    current.setScreen(new CraftingGuideScreen());
                }
            }
        });
    }
}
