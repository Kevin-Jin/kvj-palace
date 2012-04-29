package kvj.shithead.frontend.gui;

import kvj.shithead.backend.Player;
import kvj.shithead.backend.adapter.PlayerAdapter;

public abstract class GuiPlayer extends Player {
	private boolean waitingForMove;

	public GuiPlayer(int playerId, PlayerAdapter adapter) {
		super(playerId, adapter);
	}

	public boolean isThinking() {
		return waitingForMove;
	}
}
