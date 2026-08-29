package com.plokie;

import com.plokie.classes.SplatoonClasses;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.commands.PingCommand;

import com.plokie.customitems.CustomItemManager;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.ScheduleEvent;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.SkirmishManager;
import com.plokie.management.maps.GamemodeMaps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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

	AbilityManager abilityManager = null;
	SplatoonClasses classManager = null;
	CustomItemManager customItemManager = null;
	SkirmishManager skirmishManager = null;
	ScheduleEvent scheduleEvent = null;
	public static GameFlowManager gameFlowManager = null;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			SERVER = server;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SERVER = null;
		});

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			IPlayerMixin oldPlayerMixin = (IPlayerMixin)oldPlayer;
			IPlayerMixin newPlayerMixin = (IPlayerMixin)newPlayer;

			newPlayerMixin.setClass(oldPlayerMixin.getSplatoonClass());
		});

		this.scheduleEvent = new ScheduleEvent();
		this.abilityManager = new AbilityManager();
		this.customItemManager = new CustomItemManager();
		this.classManager = new SplatoonClasses();
		gameFlowManager = new GameFlowManager();
		this.skirmishManager = new SkirmishManager();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("ping").executes(PingCommand::execute));
		});

		CommandBuilder.command("warp").subcommand("hub").executes(ctx->{
			//ctx.getStack().getSource().getEntity().teleportTo();
			return "Teleporting...";
		});

		CommandBuilder.command("warp").subcommand("to").argumentEnum("map_id", GamemodeMaps.class).executes(ctx->{
			return "Teleporting...";
		});

		LOGGER.info("Splatoon plugin :)");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
