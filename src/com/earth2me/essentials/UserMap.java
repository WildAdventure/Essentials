package com.earth2me.essentials;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;

import org.bukkit.entity.Player;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import net.ess3.api.IEssentials;
import wild.api.uuid.UUIDRegistry;

public class UserMap extends CacheLoader<UUID, User> implements IConf {
	
	private final transient IEssentials ess;
	private final transient LoadingCache<UUID, User> users;
	private final transient ConcurrentSkipListSet<UUID> keys = new ConcurrentSkipListSet<>();

	public UserMap(final IEssentials ess) {
		super();
		this.ess = ess;
		users = CacheBuilder.newBuilder().maximumSize(ess.getSettings().getMaxUserCacheCount()).softValues().build(this);
		loadAllUsersAsync(ess);
	}

	private void loadAllUsersAsync(final IEssentials ess) {
		ess.runTaskAsynchronously(() -> {
			final File userdir = new File(ess.getDataFolder(), "userdata");
			if (!userdir.exists()) {
				return;
			}
			keys.clear();
			users.invalidateAll();
			for (String string : userdir.list()) {
				if (!string.endsWith(".yml")) {
					continue;
				}
				try {
					final UUID uuid = UUID.fromString(string.substring(0, string.length() - 4));
					keys.add(uuid);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			UUIDRegistry.syncLoadUUIDs(keys);
		});
	}

	public boolean userExists(final UUID uuid) {
		return keys.contains(uuid);
	}

	public User getUser(final UUID uuid) {
		try {
			return users.get(uuid);
		} catch (ExecutionException ex) {
			return null;
		} catch (UncheckedExecutionException ex) {
			return null;
		}
	}

	@Override
	public User load(final UUID uuid) throws Exception {
		for (Player player : ess.getServer().getOnlinePlayers()) {
			if (player.getUniqueId().equals(uuid)) {
				keys.add(uuid);
				return new User(player, ess);
			}
		}
		final File userFile = getUserFile(uuid);
		if (userFile.exists()) {
			keys.add(uuid);
			String name = UUIDRegistry.getName(uuid);
			if (name == null) {
				ess.getLogger().severe("Couldn't find name for UUID " + uuid);
				name = uuid.toString();
			}
			return new User(new OfflinePlayer(name, uuid, ess), ess);
		}
		throw new Exception("User not found!");
	}

	@Override
	public void reloadConfig() {
		loadAllUsersAsync(ess);
	}

	public void removeUser(final UUID uuid) {
		keys.remove(uuid);
		users.invalidate(uuid);
	}

	public Set<UUID> getAllUniqueUsers() {
		return Collections.unmodifiableSet(keys);
	}

	public int getUniqueUsers() {
		return keys.size();
	}

	public File getUserFile(final UUID uuid) {
		final File userFolder = new File(ess.getDataFolder(), "userdata");
		return new File(userFolder, uuid.toString() + ".yml");
	}

}
