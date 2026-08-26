package com.plokie;

import com.plokie.classes.SplatoonClasses;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.commands.PingCommand;

import com.plokie.customitems.CustomItemManager;
import com.plokie.helpers.ScheduleEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import sun.rmi.server.Dispatcher;

public class Splatoon implements ModInitializer {
	public static class Tags {
		public static final TagKey<Block> GROUND_BLOCKS =  create("ground_blocks");
		public static final TagKey<Block> WALL_BLOCKS =  create("wall_blocks");

		private static TagKey<Block> create(String string) {
			return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("splatoon", string));
		}
	}


	public static final String MOD_ID = "splatoon";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer SERVER = null;

	AbilityManager abilityManager;
	SplatoonClasses classManager;
	CustomItemManager customItemManager;
	ScheduleEvent scheduleEvent;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			SERVER = server;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SERVER = null;
		});

		this.scheduleEvent = new ScheduleEvent();
		this.abilityManager = new AbilityManager();
		this.customItemManager = new CustomItemManager();
		this.classManager = new SplatoonClasses();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("ping").executes(PingCommand::execute));
		});

		LOGGER.info("Splatoon plugin :)");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
