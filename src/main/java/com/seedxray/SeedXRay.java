package com.seedxray;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeedXRay implements ModInitializer {
	public static final String MOD_ID = "seedxray";

	/** what players see, the id has no room for the dash */
	public static final String MOD_NAME = "Seed X-Ray";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
	}
}