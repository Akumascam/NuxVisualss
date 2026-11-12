package com.nuxvisuals;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NuxVisuals implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("nuxvisuals");

    @Override
    public void onInitialize() {
        LOGGER.info("NuxVisuals загружен!");
    }
}
