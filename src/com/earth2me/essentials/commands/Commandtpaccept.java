package com.earth2me.essentials.commands;

import static com.earth2me.essentials.I18n.tl;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;


public class Commandtpaccept extends EssentialsCommand
{
	public Commandtpaccept()
	{
		super("tpaccept");
	}

	@Override
	public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception
	{
		final User requester;
		try
		{
			requester = ess.getUser(user.getTeleportRequest());
		}
		catch (Exception ex)
		{
			throw new Exception(tl("noPendingRequest"));
		}

		if (!requester.getBase().isOnline())
		{
			throw new Exception(tl("noPendingRequest"));
		}

		if (user.isTpRequestHere() && ((!requester.isAuthorized("essentials.tpahere") && !requester.isAuthorized("essentials.tpaall"))
									   || (/*user.getWorld() != requester.getWorld() &&*/ ess.getSettings().isWorldTeleportPermissions()
										   && !user.isAuthorized("essentials.worlds." + user.getWorld().getName()))))
		{
			throw new Exception(tl("noPendingRequest"));
		}

		if (!user.isTpRequestHere() && (!requester.isAuthorized("essentials.tpa")
										|| (/*user.getWorld() != requester.getWorld() &&*/ ess.getSettings().isWorldTeleportPermissions()
											&& !user.isAuthorized("essentials.worlds." + /*requester*/user.getWorld().getName()))))
		{
			//throw new Exception(tl("noPendingRequest"));
			throw new Exception(ChatColor.DARK_RED + "Non hai il permesso per accettare richieste di teletrasporto in questo mondo.");
		}

		if (args.length > 0 && !requester.getName().contains(args[0]))
		{
			throw new Exception(tl("noPendingRequest"));
		}

		long timeout = ess.getSettings().getTpaAcceptCancellation();
		if (timeout != 0 && (System.currentTimeMillis() - user.getTeleportRequestTime()) / 1000 > timeout)
		{
			user.requestTeleport(null, false);
			throw new Exception(tl("requestTimedOut"));
		}

		final Trade charge = new Trade(this.getName(), ess);
		user.sendMessage(tl("requestAccepted"));
		requester.sendMessage(tl("requestAcceptedFrom", user.getDisplayName()));

		try
		{
			if (user.isTpRequestHere())
			{
				final Location loc = user.getTpRequestLocation();
				requester.getTeleport().teleportPlayer(user, user.getTpRequestLocation(), charge, TeleportCause.COMMAND);
				requester.sendMessage(tl("teleporting", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
			}
			else
			{
				requester.getTeleport().teleport(user.getBase().getLocation(), charge, TeleportCause.COMMAND);
			}
		}
		catch (Exception ex)
		{
			user.sendMessage(tl("pendingTeleportCancelled"));
			ess.showError(requester.getSource(), ex, commandLabel);
		}
		user.requestTeleport(null, false);
		throw new NoChargeException();
	}

}
