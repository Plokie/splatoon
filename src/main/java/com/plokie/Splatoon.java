package com.plokie;

import com.plokie.commands.PingCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import sun.rmi.server.Dispatcher;

public class Splatoon implements ModInitializer {
	public static final String MOD_ID = "splatoon";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer SERVER = null;

	//TeamManager m_teamManager;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			SERVER = server;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SERVER = null;
		});

		//m_teamManager = new TeamManager();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("ping").executes(PingCommand::execute));
		});

		LOGGER.info("Splatoon plugin :)");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
