package org.mineacademy.bfo.conversation;

import org.mineacademy.bfo.Common;

import jline.internal.Nullable;
import lombok.Data;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@Data
public abstract class SimplePrompt {

	private final SimpleConversation parent;

	public final void onInput(final String input) {
		final val prompt = acceptValidatedInput(input);
		if (prompt == null) {
			parent.onEnd();
			ConversationManager.end(parent);
			return;
		}
		parent.currentPrompt = prompt;
		tell(prompt.getPrompt());
	}

	protected final void tell(final String... message) {
		Common.tell(parent.getPlayer(), message);
	}

	protected final ProxiedPlayer getPlayer() {
		return getParent().getPlayer();
	}

	@NonNull
	public abstract String getPrompt();

	@Nullable
	protected abstract SimplePrompt acceptValidatedInput(final String input);
}
