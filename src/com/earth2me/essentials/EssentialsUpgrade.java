package com.earth2me.essentials;

import java.io.*;
import net.ess3.api.IEssentials;

public class EssentialsUpgrade {
	
	private final transient IEssentials ess;
	private final transient EssentialsConf doneFile;

	EssentialsUpgrade(final IEssentials essentials) {
		ess = essentials;
		if (!ess.getDataFolder().exists()) {
			ess.getDataFolder().mkdirs();
		}
		doneFile = new EssentialsConf(new File(ess.getDataFolder(), "upgrades-done.yml"));
		doneFile.load();
	}

	private void upgradeToUUID() {
		if (doneFile.getBoolean("upgradeToUUID", false)) {
			return;
		}
		final File usersFolder = new File(ess.getDataFolder(), "userdata");
		if (!usersFolder.exists()) {
			return;
		}
		final File[] listOfFiles = usersFolder.listFiles();
		for (File listOfFile : listOfFiles) {
			final String filename = listOfFile.getName();
			if (!listOfFile.isFile() || !filename.endsWith(".yml")) {
				continue;
			}

			// TODO
		}
		doneFile.setProperty("upgradeToUUID", true);
		doneFile.save();
	}

	public void beforeSettings() {
		if (!ess.getDataFolder().exists()) {
			ess.getDataFolder().mkdirs();
		}
	}

	public void afterSettings() {
		upgradeToUUID();
	}
}
