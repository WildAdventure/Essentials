package com.earth2me.essentials;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import com.earth2me.essentials.api.IItemDb;
import com.earth2me.essentials.api.IJails;
import com.earth2me.essentials.api.IWarps;
import com.earth2me.essentials.perm.PermissionsHandler;
import com.earth2me.essentials.register.payment.Methods;


public interface IEssentials extends Plugin
{
	void addReloadListener(IConf listener);

	void reload();

	boolean onCommandEssentials(CommandSender sender, Command command, String commandLabel, String[] args, ClassLoader classLoader, String commandPath, String permissionPrefix, IEssentialsModule module);
	
	@Deprecated
	User getUser(final Object base);
	
	User getUser(String name);
	
	User getUser(UUID uuid);
	
	User getUser(Player base);

	I18n getI18n();

	User getOfflineUser(UUID uuid);
	
	User getOfflineUser(final String name);

	World getWorld(String name);

	int broadcastMessage(String message);

	int broadcastMessage(IUser sender, String message);

	int broadcastMessage(String permission, String message);

	ISettings getSettings();

	BukkitScheduler getScheduler();

	IJails getJails();

	IWarps getWarps();

	Worth getWorth();

	Backup getBackup();

	Methods getPaymentMethod();

	BukkitTask runTaskAsynchronously(Runnable run);

	BukkitTask runTaskLaterAsynchronously(Runnable run, long delay);

	int scheduleSyncDelayedTask(Runnable run);

	int scheduleSyncDelayedTask(Runnable run, long delay);

	int scheduleSyncRepeatingTask(final Runnable run, long delay, long period);

	TNTExplodeListener getTNTListener();

	PermissionsHandler getPermissionsHandler();

	AlternativeCommandsHandler getAlternativeCommandsHandler();

	void showError(final CommandSource sender, final Throwable exception, final String commandLabel);

	IItemDb getItemDb();

	UserMap getUserMap();

	EssentialsTimer getTimer();

	List<String> getVanishedPlayers();
}
