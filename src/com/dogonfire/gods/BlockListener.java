package com.dogonfire.gods;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.event.EventException
public class BlockListener implements Listener
{
	private Gods plugin;
	private Random random = new Random();
	private HashMap<String, Long> lastEatTimes = new HashMap();

	BlockListener(Gods p)
	{
		this.plugin = p;
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event)
	{
		if (!this.plugin.holyLandEnabled)
		{
			return;
		}
		String targetLandGodName = this.plugin.getLandManager().getGodAtHolyLandLocation(event.getLocation());
		if (targetLandGodName != null)
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		String godName = this.plugin.getBelieverManager().getGodForBeliever(event.getPlayer().getUniqueId());
		Material type = player.getItemInHand().getType();
		if (godName != null)
		{
			Long lastEatTime = (Long) this.lastEatTimes.get(player.getName());
			Long currentTime = Long.valueOf(System.currentTimeMillis());
			if ((lastEatTime == null) || (currentTime.longValue() - lastEatTime.longValue() > 10000L))
			{
				if ((this.plugin.commandmentsEnabled) && (player.getHealth() != player.getMaxHealth()))
				{
					if ((player.isOp()) || (this.plugin.getPermissionsManager().hasPermission(player, "gods.commandments")))
					{
						this.plugin.getGodManager().handleEat(player, godName, type.name());
					}
				}
				if (this.plugin.questsEnabled)
				{
					this.plugin.getQuestManager().handleEat(player.getName(), godName, type.name());
				}
				this.lastEatTimes.put(player.getName(), currentTime);
			}
		}
	}

	@EventHandler
	public void OnPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		String godName = null;
		if ((player == null) || (!this.plugin.isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
		{
			Material type = player.getItemInHand().getType();
			if ((type != null) && (type != Material.AIR))
			{
				godName = this.plugin.getBelieverManager().getGodForBeliever(event.getPlayer().getUniqueId());
				if (godName != null)
				{
					if (this.plugin.holyArtifactsEnabled)
					{
						this.plugin.getHolyArtifactManager().handleActivate(player.getName(), player.getItemInHand());
					}
				}
			}
		}
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
		{
			if (!this.plugin.getAltarManager().isAltarSign(event.getClickedBlock()))
			{
				return;
			}
			
			BlockState state = event.getClickedBlock().getState();

			Sign sign = (Sign) state;
			if (this.plugin.cursingEnabled)
			{
				Player cursedPlayer = this.plugin.getAltarManager().getCursedPlayerFromAltar(event.getClickedBlock(), sign.getLines());
				
				if (cursedPlayer != null)
				{
					if (this.plugin.getGodManager().isPriest(player.getUniqueId()))
					{
						Player oldCursedPlayer = this.plugin.getGodManager().getCursedPlayerForGod(godName);
						if ((oldCursedPlayer != null) && oldCursedPlayer==cursedPlayer)
						{
							this.plugin.getGodManager().setCursedPlayerForGod(godName, null);

							this.plugin.getLanguageManager().setPlayerName(cursedPlayer.getDisplayName());

							this.plugin.getGodManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerUnset, 2);
							this.plugin.getGodManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerUnset, cursedPlayer.getUniqueId());
						}
						else
						{
							this.plugin.getGodManager().setCursedPlayerForGod(godName, cursedPlayer.getUniqueId());

							this.plugin.getLanguageManager().setPlayerName(cursedPlayer.getDisplayName());

							this.plugin.getGodManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerSet, 2);
							this.plugin.getGodManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerSet, player.getUniqueId());

							this.plugin.log(player.getName() + " asked " + godName + " for curses on " + cursedPlayer);
						}
					}
					else
					{
						this.plugin.sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CursesNotAllowed, ChatColor.DARK_RED, 0, "", 1);
					}
					
					return;
				}
			}
			if (this.plugin.blessingEnabled)
			{
				Player blessedPlayer = this.plugin.getAltarManager().getBlessedPlayerFromAltarSign(event.getClickedBlock(), sign.getLines());
				if (blessedPlayer != null)
				{
					if (this.plugin.getGodManager().isPriest(player.getUniqueId()))
					{
						String oldBlessedPlayer = this.plugin.getGodManager().getBlessedPlayerForGod(godName);
						if ((oldBlessedPlayer != null) && (oldBlessedPlayer.equals(blessedPlayer)))
						{
							this.plugin.getGodManager().setBlessedPlayerForGod(godName, null);

							this.plugin.getLanguageManager().setPlayerName(blessedPlayer.getDisplayName());

							this.plugin.getGodManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerUnset, 2);

							this.plugin.getGodManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerUnset, player.getUniqueId());
						}
						else
						{
							this.plugin.getGodManager().setBlessedPlayerForGod(godName, blessedPlayer.getUniqueId());

							this.plugin.getLanguageManager().setPlayerName(blessedPlayer.getDisplayName());

							this.plugin.getGodManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerSet, 2);

							this.plugin.getGodManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerSet, player.getUniqueId());

							this.plugin.log(player.getName() + " asked " + godName + " for blessings on " + blessedPlayer);
						}
					}
					else
					{
						this.plugin.sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsNotAllowed, ChatColor.DARK_RED, 0, "", 1);
					}
					return;
				}
			}
			
			if ((!event.getPlayer().isOp()) && (!this.plugin.getPermissionsManager().hasPermission(event.getPlayer(), "gods.altar.pray")))
			{
				this.plugin.sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarPrayingNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				return;
			}
			
			Block block = event.getClickedBlock();
			
			if (!this.plugin.getAltarManager().isPrayingAltar(block))
			{
				return;
			}
			
			godName = sign.getLine(2);
			
			if (godName == null)
			{
				return;
			}
			
			godName = godName.trim();
			
			if (godName.length() <= 1)
			{
				this.plugin.sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 1);
				return;
			}
			
			godName = this.plugin.getGodManager().formatGodName(godName);
			
			if ((this.plugin.isBlacklistedGod(godName)) || (!this.plugin.isWhitelistedGod(godName)))
			{
				this.plugin.sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				return;
			}
			
			if (!this.plugin.getGodManager().hasGodAccess(player.getUniqueId(), godName))
			{
				this.plugin.sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
				return;
			}
			
			if (this.plugin.getGodManager().handleAltarPray(block.getLocation(), event.getPlayer(), godName))
			{
				this.plugin.log(event.getPlayer().getDisplayName() + " prayed to " + godName + " at an altar");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if ((player == null) || (!this.plugin.isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		
		if (this.plugin.questsEnabled)
		{
			if (event.getAction() == Action.PHYSICAL)
			{
				if (this.plugin.getQuestManager().handlePressurePlate(player.getUniqueId(), event.getClickedBlock()))
				{
					event.setCancelled(true);
				}
			}
			
			if (event.getClickedBlock() != null && this.plugin.getQuestManager().handleOpenChest(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()))
			{
				event.setCancelled(true);
			}
		}
		
		if (this.plugin.biblesEnabled)
		{
			if ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
			{
				if ((event.getItem() != null) && (player.getItemInHand() != null))
				{
					ItemStack book = player.getItemInHand();
					if (book.getType() == Material.WRITTEN_BOOK)
					{
						String godName = this.plugin.getBibleManager().getGodForBible(book);
						if (godName != null)
						{
							Long lastEatTime = (Long) this.lastEatTimes.get(player.getName());
							Long currentTime = Long.valueOf(System.currentTimeMillis());
							if ((lastEatTime == null) || (currentTime.longValue() - lastEatTime.longValue() > 10000L))
							{
								this.plugin.getGodManager().handleReadBible(godName, player);
								this.plugin.getQuestManager().handleReadBible(godName, player.getUniqueId());
								this.lastEatTimes.put(player.getName(), currentTime);
							}
						}
					}
				}
			}
			else if ((event.getAction().equals(Action.LEFT_CLICK_AIR)) || (event.getAction().equals(Action.LEFT_CLICK_BLOCK)))
			{
				if ((event.getItem() != null) && (player.getItemInHand() != null))
				{
					ItemStack book = player.getItemInHand();
					if (book.getType() == Material.WRITTEN_BOOK)
					{
						String godName = this.plugin.getBibleManager().getGodForBible(book);
						if (godName != null)
						{
							this.plugin.getGodManager().handleBibleMelee(godName, player);
							this.plugin.getQuestManager().handleBibleMelee(godName, player.getUniqueId());
						}
					}
				}
			}
		}
		
		if (!this.plugin.holyLandEnabled)
		{
			return;
		}
		
		if (!this.plugin.getPermissionsManager().hasPermission(event.getPlayer(), "gods.holyland"))
		{
			this.plugin.logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		if ((!player.isOp()) && (this.plugin.getLandManager().isNeutralLandLocation(player.getLocation())))
		{
			if (!this.plugin.allowInteractionInNeutralLands)
			{
				event.setCancelled(true);
			}
			return;
		}
		if (event.getClickedBlock() == null)
		{
			return;
		}
		String blockGodName = this.plugin.getLandManager().getGodAtHolyLandLocation(event.getClickedBlock().getLocation());
		if (blockGodName == null)
		{
			return;
		}
		if ((this.plugin.holylandBreakableBlockTypes.contains(event.getClickedBlock().getType())) || (this.plugin.getAltarManager().isAltarBlock(event.getClickedBlock())))
		{
			return;
		}
		if (this.plugin.getLandManager().isContestedLand(player.getLocation()))
		{
			player.sendMessage(ChatColor.RED + "This Holy Land is contested! Win the battle before you can access this Holy Land!");
			event.setCancelled(true);
			return;
		}
		String playerGodName = this.plugin.getBelieverManager().getGodForBeliever(player.getUniqueId());
		if (playerGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
			return;
		}
		if (!playerGodName.equals(blockGodName))
		{
			if (this.plugin.getGodManager().hasAllianceRelation(blockGodName, playerGodName))
			{
				return;
			}
			if (player.isOp())
			{
				return;
			}
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		this.plugin.getGodManager().updateOnlineGods();
		if ((this.plugin.useUpdateNotifications) && ((event.getPlayer().isOp()) || (this.plugin.getPermissionsManager().hasPermission(event.getPlayer(), "gods.updates"))))
		{
			this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new UpdateNotifier(this.plugin, event.getPlayer()));
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		this.lastEatTimes.remove(player.getName());
		if (!this.plugin.holyLandEnabled)
		{
			return;
		}
		this.plugin.getLandManager().handleQuit(player.getName());

		this.plugin.getGodManager().updateOnlineGods();
	}

	@EventHandler
	public void OnEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getDamager() == null)
		{
			return;
		}
		if (!this.plugin.isEnabledInWorld(event.getDamager().getWorld()))
		{
			return;
		}
		if (this.plugin.questsEnabled)
		{
			if ((event.getDamager() instanceof Player))
			{
				Player attackerPlayer = (Player) event.getDamager();
				if ((event.getEntity() instanceof Player))
				{
					Player victimPlayer = (Player) event.getEntity();

					String attackerGodName = this.plugin.getBelieverManager().getGodForBeliever(attackerPlayer.getUniqueId());
					String victimGodName = this.plugin.getBelieverManager().getGodForBeliever(victimPlayer.getUniqueId());
					if ((attackerGodName != null) && (victimGodName != null))
					{
						if (this.plugin.getGodManager().hasWarRelation(attackerGodName, victimGodName))
						{
							this.plugin.logDebug("hasWarRelation");

							event.setCancelled(false);
						}
					}
				}
			}
		}
		if (!this.plugin.holyLandEnabled)
		{
			return;
		}
		if (this.plugin.getLandManager().isNeutralLandLocation(event.getEntity().getLocation()))
		{
			event.setCancelled(true);
		}
		String godName = this.plugin.getLandManager().getGodAtHolyLandLocation(event.getEntity().getLocation());
		if (godName != null)
		{
			if ((event.getDamager() instanceof Player))
			{
				Player player = (Player) event.getDamager();

				String attackerGodName = this.plugin.getBelieverManager().getGodForBeliever(player.getUniqueId());
				if (attackerGodName == null)
				{
					if ((this.plugin.holyLandLightning) && ((event.getEntity() instanceof Player)))
					{
						this.plugin.getGodManager().strikePlayerWithLightning(player.getUniqueId(), 3);
					}
					event.setCancelled(true);

					return;
				}
				if (!godName.equals(attackerGodName))
				{
					if ((this.plugin.holyLandLightning) && ((event.getEntity() instanceof Player)))
					{
						this.plugin.getGodManager().strikePlayerWithLightning(player.getUniqueId(), 3);
					}
					if (!this.plugin.getGodManager().hasWarRelation(godName, attackerGodName))
					{
						event.setCancelled(true);
					}
				}
				else if ((event.getEntity() instanceof Player))
				{
					this.plugin.getGodManager().getGodPvP(godName);
				}
			}
			else if (((event.getDamager() instanceof LivingEntity)) && (!this.plugin.getGodManager().getGodMobDamage(godName)))
			{
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void OnPlayerPickupItem(PlayerPickupItemEvent event)
	{
		Player player = event.getPlayer();
		
		if (player == null || !this.plugin.isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		
		if (this.plugin.holyArtifactsEnabled)
		{
			if (this.plugin.getHolyArtifactManager().isHolyArtifact(event.getItem().getItemStack()))
			{
				if (this.plugin.getHolyArtifactManager().hasHolyArtifact(player.getName()))
				{
					event.setCancelled(true);
					return;
				}
			}
		}
		
		if (this.plugin.marriageEnabled)
		{
			this.plugin.getMarriageManager().handlePickupItem(player, event.getItem(), event.getItem().getLocation());
		}
		
		if (this.plugin.questsEnabled)
		{
			this.plugin.getQuestManager().handlePickupItem(player.getName(), event.getItem(), event.getItem().getLocation());
		}
	}

	@EventHandler
	public void OnPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		if ((player == null) || (!this.plugin.isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if ((!player.isOp()) && (!this.plugin.getPermissionsManager().hasPermission(player, "gods.altar.sacrifice")))
		{
			this.plugin.logDebug("OnPlayerDropItem(): Does not have gods.altar.sacrifice");
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE)
		{
			return;
		}
		this.plugin.getAltarManager().addDroppedItem(event.getItemDrop().getEntityId(), player.getName());
		if (this.plugin.holyArtifactsEnabled)
		{
			this.plugin.getHolyArtifactManager().handleDrop(player.getName(), event.getItemDrop(), event.getItemDrop().getLocation());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnBlockPlace(BlockPlaceEvent event)
	{
		if (!this.plugin.holyLandEnabled)
		{
			return;
		}
		Player player = event.getPlayer();
		if ((player == null) || (!this.plugin.isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if (player.isOp())
		{
			return;
		}
		if (event.getBlock() == null)
		{
			return;
		}
		if (!this.plugin.getPermissionsManager().hasPermission(player, "gods.holyland"))
		{
			this.plugin.logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		if (this.plugin.getLandManager().isNeutralLandLocation(event.getBlock().getLocation()))
		{
			player.sendMessage(ChatColor.RED + "You cannot build in neutral land");

			event.setCancelled(true);
			return;
		}
		String godName = this.plugin.getLandManager().getGodAtHolyLandLocation(event.getBlock().getLocation());
		String playerGod = null;
		if (godName == null)
		{
			return;
		}
		if (player != null)
		{
			playerGod = this.plugin.getBelieverManager().getGodForBeliever(player.getUniqueId());
		}
		if ((playerGod == null) || (!playerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);

			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void OnSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		
		if ((player == null) || (!this.plugin.isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		
		if ((this.plugin.cursingEnabled) && (this.plugin.getAltarManager().isCursingAltar(event.getBlock(), event.getLines())))
		{
			if (!this.plugin.getAltarManager().handleNewCursingAltar(event))
			{
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
			}
			return;
		}
		
		if ((this.plugin.blessingEnabled) && (this.plugin.getAltarManager().isBlessingAltar(event.getBlock(), event.getLines())))
		{
			if (!this.plugin.getAltarManager().handleNewBlessingAltar(event))
			{
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
			}
			return;
		}
		
		if (this.plugin.getAltarManager().isPrayingAltar(event.getBlock()))
		{
			if (!this.plugin.getAltarManager().handleNewPrayingAltar(event))
			{
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
			}
			return;
		}
	}

	@EventHandler
	public void OnEntityDeath(EntityDeathEvent event)
	{
		if (!(event.getEntity().getKiller() instanceof Player))
		{
			return;
		}
		
		Player player = event.getEntity().getKiller();
		
		if (!this.plugin.isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		
		String godName = this.plugin.getBelieverManager().getGodForBeliever(player.getUniqueId());
		
		if (godName == null)
		{
			return;
		}
		
//		if (this.plugin.propheciesEnabled)
//		{
//			this.plugin.getProphecyManager().handleMobKill(player.getName(), godName, event.getEntityType().name());
//		}
		
		if (this.plugin.questsEnabled)
		{
			this.plugin.getQuestManager().handleKilledMob(godName, event.getEntityType().name());
		}
		
		if (this.plugin.holyArtifactsEnabled)
		{
			this.plugin.getHolyArtifactManager().handleDeath(event.getEntity().getKiller().getName(), godName, event.getEntity().getKiller().getItemInHand());
		}
		
		if ((!player.isOp()) && (!this.plugin.getPermissionsManager().hasPermission(player, "gods.commandments")))
		{
			return;
		}
		
		this.plugin.getGodManager().handleKilled(player, godName, event.getEntityType().name());
	}

	@EventHandler
	public void OnEntityCombust(EntityCombustEvent event)
	{
		if (!this.plugin.sacrificesEnabled)
		{
			return;
		}
		if (event.getEntity() == null)
		{
			return;
		}
		if (!(event.getEntity() instanceof Item))
		{
			return;
		}
		Item item = (Item) event.getEntity();
		if (!this.plugin.isEnabledInWorld(item.getWorld()))
		{
			return;
		}
		if (event.getEntity().getType() != EntityType.DROPPED_ITEM)
		{
			return;
		}
		String believerName = this.plugin.getAltarManager().getDroppedItemPlayer(event.getEntity().getEntityId());
		if (believerName == null)
		{
			return;
		}
		Player player = this.plugin.getServer().getPlayer(believerName);
		if (player == null)
		{
			return;
		}
		if ((!player.isOp()) && (!this.plugin.getPermissionsManager().hasPermission(player, "gods.altar.sacrifice")))
		{
			this.plugin.logDebug("Does not have gods.altar.sacrifice");
			return;
		}
		
		String godName = this.plugin.getBelieverManager().getGodForBeliever(player.getUniqueId());
		
		if (godName == null)
		{
			return;
		}
		
		if (this.plugin.getQuestManager().handleSacrifice(player, godName, item.getItemStack().getType().name()))
		{
			return;
		}
		
		this.plugin.getGodManager().handleSacrifice(godName, player, item.getItemStack().getType());
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player player = event.getEntity();
		if (!this.plugin.isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		String godName = this.plugin.getBelieverManager().getGodForBeliever(player.getUniqueId());
		GodManager.GodType godType = this.plugin.getGodManager().getDivineForceForGod(godName);

		this.plugin.getGodManager().handleKilledPlayer(player.getUniqueId(), godName, godType);
		this.plugin.getQuestManager().handleKilledPlayer(player.getUniqueId(), godName);

		double powerAfter = 0.0D;
		double powerBefore = 0.0D;
		if ((event.getEntity().getKiller() != null) && ((event.getEntity().getKiller() instanceof Player)))
		{
			Player killer = event.getEntity().getKiller();
			String killerGodName = this.plugin.getBelieverManager().getGodForBeliever(killer.getUniqueId());
			if (killerGodName != null)
			{
				if (godName == null)
				{
					if (this.plugin.getGodManager().getDivineForceForGod(killerGodName) == GodManager.GodType.WAR)
					{
						powerBefore = this.plugin.getBelieverManager().getBelieverPower(player.getUniqueId());
						this.plugin.getBelieverManager().increasePrayer(killer.getUniqueId(), killerGodName, 2);
						this.plugin.getBelieverManager().increasePrayerPower(killer.getUniqueId(), 2);
						powerAfter = this.plugin.getBelieverManager().getBelieverPower(player.getUniqueId());

						this.plugin.sendInfo(killer.getUniqueId(), LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
					}
				}
				else
				{
					List<String> warRelations = this.plugin.getGodManager().getWarRelations(killerGodName);
					if (warRelations != null)
					{
						if (warRelations.contains(godName))
						{
							powerBefore = this.plugin.getBelieverManager().getBelieverPower(player.getUniqueId());
							this.plugin.getBelieverManager().increasePrayer(killer.getUniqueId(), killerGodName, 2);
							this.plugin.getBelieverManager().increasePrayerPower(killer.getUniqueId(), 2);
							powerAfter = this.plugin.getBelieverManager().getBelieverPower(player.getUniqueId());

							this.plugin.sendInfo(killer.getUniqueId(), LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingEnemy, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
						}
					}
				}
			}
		}
	}
}
