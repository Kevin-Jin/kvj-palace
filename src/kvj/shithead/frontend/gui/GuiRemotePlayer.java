package kvj.shithead.frontend.gui;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public class GuiRemotePlayer extends GuiPlayer {
	public GuiRemotePlayer(int playerId, PlayerAdapter adapter) {
		super(playerId, adapter);
	}

	@Override
	public Card chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile) {
		return null;
	}
}
