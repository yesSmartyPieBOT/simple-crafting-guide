package com.yessmartypie.simplecraftingguide;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleCraftingGuideClient implements ClientModInitializer {
    public static final String MOD_ID = "simple_crafting_guide";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Simple Crafting Guide initialized.");
        CraftingGuideKeybind.register();
    }
}
