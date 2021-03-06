package com.earth2me.essentials;

import static com.earth2me.essentials.I18n.tl;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.TextInput;
import com.earth2me.essentials.textreader.TextPager;
import com.earth2me.essentials.utils.LocationUtil;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ess3.api.IEssentials;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class EssentialsPlayerListener implements Listener {
	private static final Logger LOGGER = Logger.getLogger("Essentials");
	private final transient IEssentials ess;

	public EssentialsPlayerListener(final IEssentials parent) {
		this.ess = parent;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		final User user = ess.getUser(event.getPlayer());
		updateCompass(user);
		user.setDisplayNick();

		if (ess.getSettings().isTeleportInvulnerability()) {
			user.enableInvulnerabilityAfterTeleport();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		final User user = ess.getUser(event.getPlayer());
		if (user.isMuted()) {
			event.setCancelled(true);
			user.sendMessage(tl("voiceSilenced"));
			LOGGER.info(tl("mutedUserSpeaks", user.getName()));
		}
		try {
			final Iterator<Player> it = event.getRecipients().iterator();
			while (it.hasNext()) {
				final User u = ess.getUser(it.next());
				if (u.isIgnoredPlayer(user)) {
					it.remove();
				}
			}
		} catch (UnsupportedOperationException ex) {
			if (ess.getSettings().isDebug()) {
				ess.getLogger().log(Level.INFO, "Ignore could not block chat due to custom chat plugin event.", ex);
			} else {
				ess.getLogger().info("Ignore could not block chat due to custom chat plugin event.");
			}
		}

		user.updateActivity(true);
		user.setDisplayNick();
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(final PlayerMoveEvent event) {
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
			return;
		}

		if (!ess.getSettings().cancelAfkOnMove() && !ess.getSettings().getFreezeAfkPlayers()) {
			event.getHandlers().unregister(this);

			if (ess.getSettings().isDebug()) {
				LOGGER.log(Level.INFO, "Unregistering move listener");
			}

			return;
		}

		final User user = ess.getUser(event.getPlayer());
		if (user.isAfk() && ess.getSettings().getFreezeAfkPlayers()) {
			final Location from = event.getFrom();
			final Location origTo = event.getTo();
			final Location to = origTo.clone();
			if (ess.getSettings().cancelAfkOnMove() && origTo.getY() >= from.getBlockY() + 1) {
				user.updateActivity(true);
				return;
			}
			to.setX(from.getX());
			to.setY(from.getY());
			to.setZ(from.getZ());
			try {
				event.setTo(LocationUtil.getSafeDestination(to));
			} catch (Exception ex) {
				event.setTo(to);
			}
			return;
		}
		final Location afk = user.getAfkPosition();
		if (afk == null || !event.getTo().getWorld().equals(afk.getWorld()) || afk.distanceSquared(event.getTo()) > 9) {
			user.updateActivity(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		final User user = ess.getUser(event.getPlayer());

		if (ess.getSettings().allowSilentJoinQuit() && user.isAuthorized("essentials.silentquit")) {
			event.setQuitMessage(null);
		} else if (ess.getSettings().isCustomQuitMessage() && event.getQuitMessage() != null) {
			final Player player = event.getPlayer();
			event.setQuitMessage(ess.getSettings().getCustomQuitMessage().replace("{PLAYER}", player.getDisplayName()).replace("{USERNAME}", player.getName()));
		}

		if (ess.getSettings().removeGodOnDisconnect() && user.isGodModeEnabled()) {
			user.setGodModeEnabled(false);
		}
		if (user.isVanished()) {
			user.setVanished(false);
		}
		user.setLogoutLocation();
		if (user.isRecipeSee()) {
			user.getBase().getOpenInventory().getTopInventory().clear();
		}
		user.updateActivity(false);
		user.dispose();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final String joinMessage = event.getJoinMessage();
		ess.runTaskAsynchronously(new Runnable() {
			@Override
			public void run() {
				delayedJoin(event.getPlayer(), joinMessage);
			}
		});
		if (ess.getSettings().allowSilentJoinQuit() || ess.getSettings().isCustomJoinMessage()) {
			event.setJoinMessage(null);
		}
	}

	public void delayedJoin(final Player player, final String message) {
		if (!player.isOnline()) {
			return;
		}

		ess.getBackup().onPlayerJoin();
		final User user = ess.getUser(player);

		final long currentTime = System.currentTimeMillis();
		user.checkMuteTimeout(currentTime);
		user.updateActivity(false);

		ess.scheduleSyncDelayedTask(new Runnable() {
			@Override
			public void run() {
				if (!user.getBase().isOnline()) {
					return;
				}

				user.setLastLogin(currentTime);
				user.setDisplayNick();
				updateCompass(user);

				if (!ess.getVanishedPlayers().isEmpty() && !user.isAuthorized("essentials.vanish.see")) {
					for (String p : ess.getVanishedPlayers()) {
						Player toVanish = ess.getServer().getPlayerExact(p);
						if (toVanish != null && toVanish.isOnline()) {
							user.getBase().hidePlayer(toVanish);
						}
					}
				}

				if (user.isAuthorized("essentials.sleepingignored")) {
					user.getBase().setSleepingIgnored(true);
				}

				if ((ess.getSettings().allowSilentJoinQuit() && user.isAuthorized("essentials.silentjoin")) || message == null) {
					// Do nothing - silently join
				} else if (ess.getSettings().isCustomJoinMessage()) {
					ess.getServer().broadcastMessage(ess.getSettings().getCustomJoinMessage().replace("{PLAYER}", player.getDisplayName()).replace("{USERNAME}", player.getName()));
				} else if (ess.getSettings().allowSilentJoinQuit()) {
					ess.getServer().broadcastMessage(message);
				}

				if (!ess.getSettings().isCommandDisabled("motd") && user.isAuthorized("essentials.motd")) {
					try {
						final IText input = new TextInput(user.getSource(), "motd", true, ess);
						final IText output = new KeywordReplacer(input, user.getSource(), ess);
						final TextPager pager = new TextPager(output, true);
						pager.showPage("1", null, "motd", user.getSource());
					} catch (IOException ex) {
						if (ess.getSettings().isDebug()) {
							LOGGER.log(Level.WARNING, ex.getMessage(), ex);
						} else {
							LOGGER.log(Level.WARNING, ex.getMessage());
						}
					}
				}

				if (!ess.getSettings().isCommandDisabled("mail") && user.isAuthorized("essentials.mail")) {
					final List<String> mail = user.getMails();
					if (mail.isEmpty()) {
						user.sendMessage(tl("noNewMail"));
					} else {
						user.sendMessage(tl("youHaveNewMail", mail.size()));
					}
				}

				if (user.isAuthorized("essentials.fly.safelogin")) {
					user.getBase().setFallDistance(0);
					if (LocationUtil.shouldFly(user.getLocation())) {
						user.getBase().setAllowFlight(true);
						user.getBase().setFlying(true);
						user.getBase().sendMessage(tl("flyMode", tl("enabled"), user.getDisplayName()));
					}
				}
				user.getBase().setFlySpeed(0.1f);
				user.getBase().setWalkSpeed(0.2f);

			}
		});
	}

	// Makes the compass item ingame always point to the first essentials home.  #EasterEgg
	private void updateCompass(final User user) {
		Location loc = user.getHome(user.getLocation());
		if (loc == null) {
			loc = user.getBase().getBedSpawnLocation();
		}
		if (loc != null) {
			final Location updateLoc = loc;
			user.getBase().setCompassTarget(updateLoc);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerLogin(final PlayerLoginEvent event) {
		if (event.getResult() == Result.KICK_FULL) {
			if (event.getPlayer().hasPermission("essentials.joinfullserver")) {
				event.allow();
				return;
			}
			event.disallow(Result.KICK_FULL, tl("serverFull"));
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		final boolean backListener = ess.getSettings().registerBackInListener();
		final boolean teleportInvulnerability = ess.getSettings().isTeleportInvulnerability();
		if (backListener || teleportInvulnerability) {
			final User user = ess.getUser(event.getPlayer());
			//There is TeleportCause.COMMMAND but plugins have to actively pass the cause in on their teleports.
			if (backListener && (event.getCause() == TeleportCause.PLUGIN || event.getCause() == TeleportCause.COMMAND)) {
				user.setLastLocation();
			}
			if (teleportInvulnerability && (event.getCause() == TeleportCause.PLUGIN || event.getCause() == TeleportCause.COMMAND)) {
				user.enableInvulnerabilityAfterTeleport();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
		final Player player = event.getPlayer();
		final String cmd = event.getMessage().toLowerCase(Locale.ENGLISH).split(" ")[0].replace("/", "").toLowerCase(Locale.ENGLISH);
		if (!cmd.equalsIgnoreCase("afk")) {
			final User user = ess.getUser(player);
			user.updateActivity(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangedWorldFlyReset(final PlayerChangedWorldEvent event) {
		final User user = ess.getUser(event.getPlayer());
		if ((user.getBase().getGameMode() != GameMode.CREATIVE && user.getBase().getGameMode() != GameMode.SPECTATOR) && !user.isAuthorized("essentials.fly")) {
			user.getBase().setFallDistance(0f);
			user.getBase().setAllowFlight(false);
		}
		if (!user.isAuthorized("essentials.speed")) {
			user.getBase().setFlySpeed(0.1f);
			user.getBase().setWalkSpeed(0.2f);
		} else {
			if (user.getBase().getFlySpeed() > ess.getSettings().getMaxFlySpeed() && !user.isAuthorized("essentials.speed.bypass")) {
				user.getBase().setFlySpeed((float) ess.getSettings().getMaxFlySpeed());
			} else {
				user.getBase().setFlySpeed(user.getBase().getFlySpeed() * 0.99999f);
			}

			if (user.getBase().getWalkSpeed() > ess.getSettings().getMaxWalkSpeed() && !user.isAuthorized("essentials.speed.bypass")) {
				user.getBase().setWalkSpeed((float) ess.getSettings().getMaxWalkSpeed());
			} else {
				user.getBase().setWalkSpeed(user.getBase().getWalkSpeed() * 0.99999f);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
		final User user = ess.getUser(event.getPlayer());
		final String newWorld = event.getPlayer().getLocation().getWorld().getName();
		user.setDisplayNick();
		updateCompass(user);
		if (ess.getSettings().getNoGodWorlds().contains(newWorld) && user.isGodModeEnabledRaw()) {
			user.sendMessage(tl("noGodWorldWarning"));
		}

		if (!user.getWorld().getName().equals(newWorld)) {
			user.sendMessage(tl("currentWorld", newWorld));
		}
		if (user.isVanished()) {
			user.setVanished(user.isAuthorized("essentials.vanish"));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(final PlayerInteractEvent event) {
		switch (event.getAction()) {
			case RIGHT_CLICK_BLOCK:
				if (!event.isCancelled() && event.getClickedBlock().getType() == Material.BED_BLOCK && ess.getSettings().getUpdateBedAtDaytime()) {
					User player = ess.getUser(event.getPlayer());
					if (player.isAuthorized("essentials.sethome.bed")) {
						player.getBase().setBedSpawnLocation(event.getClickedBlock().getLocation());
						player.sendMessage(tl("bedSet", player.getLocation().getWorld().getName(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()));
					}
				}
				break;
			case LEFT_CLICK_AIR:
				if (event.getPlayer().isFlying()) {
					final User user = ess.getUser(event.getPlayer());
					if (user.isFlyClickJump()) {
						useFlyClickJump(user);
						return;
					}
				}
			case LEFT_CLICK_BLOCK:
				if (event.getItem() != null && event.getItem().getType() != Material.AIR) {
					final User user = ess.getUser(event.getPlayer());
					user.updateActivity(true);
					if (user.hasPowerTools() && user.arePowerToolsEnabled() && usePowertools(user, event.getItem().getTypeId())) {
						event.setCancelled(true);
					}
				}
				break;
			default:
				break;
		}
	}

	// This method allows the /jump lock feature to work, allows teleporting while flying #EasterEgg
	private void useFlyClickJump(final User user) {
		try {
			final Location otarget = LocationUtil.getTarget(user.getBase());

			ess.scheduleSyncDelayedTask(new Runnable() {
				@Override
				public void run() {
					Location loc = user.getLocation();
					loc.setX(otarget.getX());
					loc.setZ(otarget.getZ());
					while (LocationUtil.isBlockDamaging(loc.getWorld(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ())) {
						loc.setY(loc.getY() + 1d);
					}
					user.getBase().teleport(loc, TeleportCause.PLUGIN);
				}
			});
		} catch (Exception ex) {
			if (ess.getSettings().isDebug()) {
				LOGGER.log(Level.WARNING, ex.getMessage(), ex);
			}
		}
	}

	private boolean usePowertools(final User user, final int id) {
		final List<String> commandList = user.getPowertool(id);
		if (commandList == null || commandList.isEmpty()) {
			return false;
		}
		boolean used = false;
		// We need to loop through each command and execute
		for (final String command : commandList) {
			if (command.contains("{player}")) {
				continue;
			} else if (command.startsWith("c:")) {
				used = true;
				user.getBase().chat(command.substring(2));
			} else {
				used = true;
				ess.scheduleSyncDelayedTask(new Runnable() {
					@Override
					public void run() {
						user.getServer().dispatchCommand(user.getBase(), command);
						LOGGER.log(Level.INFO, String.format("[PT] %s issued server command: /%s", user.getName(), command));
					}
				});
			}
		}
		return used;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
		if (ess.getSettings().getDisableItemPickupWhileAfk()) {
			if (ess.getUser(event.getPlayer()).isAfk()) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryClickEvent(final InventoryClickEvent event) {
		Player refreshPlayer = null;
		final Inventory top = event.getView().getTopInventory();
		final InventoryType type = top.getType();

		if (type == InventoryType.PLAYER) {
			final User user = ess.getUser((Player) event.getWhoClicked());
			final InventoryHolder invHolder = top.getHolder();
			if (invHolder != null && invHolder instanceof HumanEntity) {
				final User invOwner = ess.getUser((Player) invHolder);
				if (user.isInvSee() && (!user.isAuthorized("essentials.invsee.modify") || invOwner.isAuthorized("essentials.invsee.preventmodify") || !invOwner.getBase().isOnline())) {
					event.setCancelled(true);
					refreshPlayer = user.getBase();
				}
			}
		} else if (type == InventoryType.ENDER_CHEST) {
			final User user = ess.getUser((Player) event.getWhoClicked());
			if (user.isEnderSee() && (!user.isAuthorized("essentials.enderchest.modify"))) {
				event.setCancelled(true);
				refreshPlayer = user.getBase();
			}
		} else if (type == InventoryType.WORKBENCH) {
			User user = ess.getUser((Player) event.getWhoClicked());
			if (user.isRecipeSee()) {
				event.setCancelled(true);
				refreshPlayer = user.getBase();
			}
		} else if (type == InventoryType.CHEST && top.getSize() == 9) {
			final User user = ess.getUser((Player) event.getWhoClicked());
			final InventoryHolder invHolder = top.getHolder();
			if (invHolder != null && invHolder instanceof HumanEntity && user.isInvSee()) {
				event.setCancelled(true);
				refreshPlayer = user.getBase();
			}
		}

		if (refreshPlayer != null) {
			final Player player = refreshPlayer;
			ess.scheduleSyncDelayedTask(new Runnable() {
				@Override
				public void run() {
					player.updateInventory();
				}
			}, 1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryCloseEvent(final InventoryCloseEvent event) {
		Player refreshPlayer = null;
		final Inventory top = event.getView().getTopInventory();
		final InventoryType type = top.getType();
		if (type == InventoryType.PLAYER) {
			final User user = ess.getUser((Player) event.getPlayer());
			user.setInvSee(false);
			refreshPlayer = user.getBase();
		} else if (type == InventoryType.ENDER_CHEST) {
			final User user = ess.getUser((Player) event.getPlayer());
			user.setEnderSee(false);
			refreshPlayer = user.getBase();
		} else if (type == InventoryType.WORKBENCH) {
			final User user = ess.getUser((Player) event.getPlayer());
			if (user.isRecipeSee()) {
				user.setRecipeSee(false);
				event.getView().getTopInventory().clear();
				refreshPlayer = user.getBase();
			}
		} else if (type == InventoryType.CHEST && top.getSize() == 9) {
			final InventoryHolder invHolder = top.getHolder();
			if (invHolder != null && invHolder instanceof HumanEntity) {
				final User user = ess.getUser((Player) event.getPlayer());
				user.setInvSee(false);
				refreshPlayer = user.getBase();
			}
		}

		if (refreshPlayer != null) {
			final Player player = refreshPlayer;
			ess.scheduleSyncDelayedTask(new Runnable() {
				@Override
				public void run() {
					player.updateInventory();
				}
			}, 1);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerFishEvent(final PlayerFishEvent event) {
		final User user = ess.getUser(event.getPlayer());
		user.updateActivity(true);
	}
}
