package com.dogonfire.gods.tasks;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.LanguageManager;

public class TaskGodSpeak implements Runnable {
	private Gods plugin;
	private UUID playerId = null;
	private String godName = null;
	private LanguageManager.LANGUAGESTRING message = null;
	private int amount = 0;
	private String playerNameString = null;
	private String typeString = null;

	public TaskGodSpeak(Gods instance, String gname, UUID playerId, String player, String type, int a) {
		this.plugin = instance;
		this.playerId = playerId;
		this.godName = new String(gname);
		this.message = null;
	}

	public TaskGodSpeak(Gods instance, String gname, UUID playerId, String player, String type, int a, LanguageManager.LANGUAGESTRING m) {
		this.plugin = instance;
		this.playerId = playerId;
		this.godName = new String(gname);
		this.message = m;

		this.playerNameString = new String(player);
		this.amount = a;
		if (type != null) {
			this.typeString = new String(type);
		} else {
			type = "";
		}
	}

	public void run() {
		Player player = this.plugin.getServer().getPlayer(this.playerId);
		if (player == null) {
			return;
		}
		this.plugin.getLanguageManager().setAmount(this.amount);
		try {
			this.plugin.getLanguageManager().setType(this.typeString);
		} catch (Exception ex) {
			this.plugin.logDebug(ex.getStackTrace().toString());
		}
		this.plugin.getLanguageManager().setPlayerName(this.playerNameString);
		if (this.message != null) {
			player.sendMessage(ChatColor.GOLD + "<" + this.godName + ">: " + ChatColor.WHITE +

					ChatColor.BOLD + this.plugin.getLanguageManager().getLanguageString(this.godName, this.message));
		} else {
			String questionMessage = ChatColor.AQUA + "Use " + ChatColor.WHITE + "/gods yes" + ChatColor.AQUA + " or " + ChatColor.WHITE + "/gods no" + ChatColor.AQUA + " to answer your god.";

			player.sendMessage(ChatColor.GOLD + "[" + "Gods" + "]: " + questionMessage);
		}
	}
}