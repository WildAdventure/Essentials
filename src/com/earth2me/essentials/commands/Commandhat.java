package com.earth2me.essentials.commands;

import static com.earth2me.essentials.I18n.tl;
import com.earth2me.essentials.User;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

public class Commandhat extends EssentialsCommand {
	public Commandhat() {
		super("hat");
	}

	@Override
	protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
		final ItemStack head = user.getBase().getInventory().getHelmet();
		if (head != null && head.getType() != Material.AIR) {
			user.sendMessage(ChatColor.DARK_RED + "Non devi indossare nulla nello slot dell'elmo.");
			return;
		}
		
		if (user.getBase().getInventory().getItemInMainHand().getType() == Material.AIR) {
			user.sendMessage(tl("hatFail"));
			return;
		}
		
		final ItemStack hand = user.getBase().getInventory().getItemInMainHand();
		if (hand.getType().getMaxDurability() == 0) {
			user.getBase().getInventory().setHelmet(hand);
			user.getBase().getInventory().setItemInMainHand(null);
			user.sendMessage(tl("hatPlaced"));
		} else {
			user.sendMessage(tl("hatArmor"));
		}
	}
}
