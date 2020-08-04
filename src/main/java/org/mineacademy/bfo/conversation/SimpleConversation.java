package org.mineacademy.bfo.conversation;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;

import lombok.Data;
import lombok.NonNull;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@Data
public abstract class SimpleConversation {

	private final ProxiedPlayer player;

	protected SimplePrompt currentPrompt;

	// ----------------------------------------------------------------------------------------------------
	// Methods to override / implement
	// ----------------------------------------------------------------------------------------------------

	protected SimpleConversation(@NonNull final ProxiedPlayer player) {
		this.player = player;
	}

	public void start() {
		ConversationManager.register(this);
		Valid.checkNotNull(getFirstPrompt(), "Prompt mustn't be null!");
		Valid.checkNotNull(player, "Player mustn't be null");

		currentPrompt = getFirstPrompt();
		Common.tell(getPlayer(), getFirstPrompt().getPrompt());
	}

	protected abstract SimplePrompt getFirstPrompt();

	public void onEnd() {
	}
}
