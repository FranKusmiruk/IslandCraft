package com.github.hoqhuuep.islandcraft.localchat;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.hoqhuuep.islandcraft.core.Message;

public class LocalChatManager {
	final LocalChatConfig config;

	public LocalChatManager(final LocalChatConfig config) {
		this.config = config;
	}

	public void sendLocalMessage(final Player from, final String message) {
		final String fromName = from.getName();
		final Location fromLocation = from.getLocation();
		final List<Player> players = from.getWorld().getPlayers();
		for (final Player to : players) {
			final Location toLocation = to.getLocation();
			final double radius = config.LOCAL_CHAT_RADIUS;
			if (fromLocation.distanceSquared(toLocation) <= radius * radius) {
				Message.LOCAL_MESSAGE.send(to, fromName, message);
			}
		}
	}
}
