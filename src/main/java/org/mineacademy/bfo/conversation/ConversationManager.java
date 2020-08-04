package org.mineacademy.bfo.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.debug.Debugger;

import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class ConversationManager implements Listener {

	private static final List<SimpleConversation> conversations = new ArrayList<>();

	public static void register(@NonNull final SimpleConversation conversation) {
		conversations.add(conversation);
	}

	public static void end(@NonNull final SimpleConversation simpleConversation) {
		conversations.remove(simpleConversation);
	}

	public static List<SimpleConversation> getConversations() {
		return Collections.unmodifiableList(conversations);
	}

	public SimpleConversation getConversationOfPlayerUnsafe(@NonNull final ProxiedPlayer proxiedPlayer) {
		final val optionalConversation = getConversationOfPlayer(proxiedPlayer);
		Valid.checkBoolean(optionalConversation.isPresent(), "No conversation found for '" + proxiedPlayer.getName() + "'");
		return optionalConversation.get();
	}

	public Optional<SimpleConversation> getConversationOfPlayer(@NonNull final ProxiedPlayer player) {
		for (final SimpleConversation conversation : getConversations()) {
			if (conversation.getPlayer().equals(player)) {
				return Optional.of(conversation);
			}
		}

		return Optional.empty();
	}

	// ----------------------------------------------------------------------------------------------------
	// Listener
	// ----------------------------------------------------------------------------------------------------

	@EventHandler
	public void onChat(final ChatEvent event) {
		if (event.isCancelled()) {
			return;
		}

		if (event.isCommand()) {
			return;
		}

		if (!(event.getSender() instanceof ProxiedPlayer)) {
			return;
		}

		final ProxiedPlayer player = (ProxiedPlayer) event.getSender();
		final String message = event.getMessage();

		getConversationOfPlayer(player).ifPresent((conversation -> {
			try {
				conversation.getCurrentPrompt().onInput(message);
				event.setCancelled(true);
				event.setMessage("");
			} catch (final Throwable throwable) {
				Debugger.saveError(throwable, "Exception in conversation");
			}
		}));
	}

	@EventHandler
	public void onLeave(final PlayerDisconnectEvent event) {

	}

}
