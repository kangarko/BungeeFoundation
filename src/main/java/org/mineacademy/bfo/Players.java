package org.mineacademy.bfo;

import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@UtilityClass
public class Players {

	private final ProxyServer instance = ProxyServer.getInstance();

	public Optional<ProxiedPlayer> find(@NonNull final String name) {
		return Optional.ofNullable(instance.getPlayer(name));
	}

	public Optional<ProxiedPlayer> find(@NonNull final UUID uuid) {
		return Optional.ofNullable(instance.getPlayer(uuid));
	}
}
