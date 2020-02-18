package com.earth2me.essentials.commands;

import static com.earth2me.essentials.I18n.tl;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.StringUtil;

import net.ess3.api.events.TeleportHomeEvent;

import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;


public class Commandhome extends EssentialsCommand
{
	public Commandhome()
	{
		super("home");
	}

	// This method contains an undocumented translation parameters #EasterEgg
	@Override
	public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception
	{
		final Trade charge = new Trade(this.getName(), ess);
		User player = user;
		String homeName = "";
		String[] nameParts;
		if (args.length > 0)
		{
			nameParts = args[0].split(":");
			if (nameParts[0].length() == args[0].length() || !user.isAuthorized("essentials.home.others"))
			{
				homeName = nameParts[0];
			}
			else
			{
				player = getPlayer(server, nameParts, 0, true, true);
				if (nameParts.length > 1)
				{
					homeName = nameParts[1];
				}
			}
		}
		try
		{
			if ("bed".equalsIgnoreCase(homeName) && user.isAuthorized("essentials.home.bed"))
			{
				final Location bed = player.getBase().getBedSpawnLocation();
				if (bed != null)
				{
					if (!checkHomeLocation(user, bed)) {
						return;
					};
					user.getTeleport().teleport(bed, charge, TeleportCause.COMMAND);
					throw new NoChargeException();
				}
				else
				{
					throw new Exception(tl("bedMissing"));
				}
			}
			goHome(user, player, homeName.toLowerCase(Locale.ENGLISH), charge);
		}
		catch (NotEnoughArgumentsException e)
		{
			Location bed = player.getBase().getBedSpawnLocation();
			final List<String> homes = player.getHomes();
			if (homes.isEmpty() && player.equals(user))
			{
				user.getTeleport().respawn(charge, TeleportCause.COMMAND);
			}
			else if (homes.isEmpty())
			{
				throw new Exception(tl("noHomeSetPlayer"));
			}
			else if (homes.size() == 1 && player.equals(user))
			{
				goHome(user, player, homes.get(0), charge);
			}
			else
			{
				final int count = homes.size();
				if (user.isAuthorized("essentials.home.bed"))
				{
					if (bed != null)
					{
						homes.add(tl("bed"));
					}
					else
					{
						homes.add(tl("bedNull"));
					}
				}
				user.sendMessage(tl("homes", StringUtil.joinList(homes), count, getHomeLimit(player)));
			}
		}
		throw new NoChargeException();
	}

	private String getHomeLimit(final User player)
	{
		if (!player.getBase().isOnline())
		{
			return "?";
		}
		if (player.isAuthorized("essentials.sethome.multiple.unlimited"))
		{
			return "*";
		}
		return Integer.toString(ess.getSettings().getHomeLimit(player));
	}

	private void goHome(final User user, final User player, final String home, final Trade charge) throws Exception {
		if (home.length() < 1)
		{
			throw new NotEnoughArgumentsException();
		}
		final Location loc = player.getHome(home);
		if (loc == null) {
			throw new NotEnoughArgumentsException();
		}
		
		if (!checkHomeLocation(user, loc)) {
			return;
		}
		
		user.getTeleport().teleport(loc, charge, TeleportCause.COMMAND);
	}
	
	private boolean checkHomeLocation(final User user, final Location home) throws Exception {
		if (/*user.getWorld() != home.getWorld() &&*/ ess.getSettings().isWorldHomePermissions() && !user.isAuthorized("essentials.home_worlds." + home.getWorld().getName())) {
			throw new Exception(ChatColor.DARK_RED + "Non hai il permesso per raggiungere la home in quel mondo.");
		}
		
		if (user.getHomes().size() > ess.getSettings().getHomeLimit(user)) {
			throw new Exception(ChatColor.DARK_RED + "Hai troppe home, devi prima rimuovere quelle in eccesso. Usa il comando " + ChatColor.RED + "/home" + ChatColor.DARK_RED + " per la lista, " + ChatColor.RED + "/delhome <nome>" + ChatColor.DARK_RED + " per eliminarne una.");
		}
		
		TeleportHomeEvent event = new TeleportHomeEvent(user.getPlayer(), home, false);
		Bukkit.getPluginManager().callEvent(event);
		
		if (event.isCancelled()) {
			return false;
		}
		
		return true;
	}
}
