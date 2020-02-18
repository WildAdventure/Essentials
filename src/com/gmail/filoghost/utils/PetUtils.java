package com.gmail.filoghost.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.dsh105.echopet.api.EchoPetAPI;
import com.dsh105.echopet.compat.api.entity.IPet;

public class PetUtils {
	
	private static boolean usePets;

	public static void setup() {
		usePets = Bukkit.getPluginManager().getPlugin("SonarPet") != null;
	}
	
	public static void beforeTeleport(Player player) {
		if (usePets) {
			IPet pet = EchoPetAPI.getAPI().getPet(player);
			if (pet != null) {
				pet.setAsHat(false);
				pet.ownerRidePet(false);
			}
		}
	}

}
