package kvj.shithead.frontend.gui;

import kvj.shithead.backend.Game;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public abstract class GuiPlayer extends Player {
	protected final GuiGame model;

	public GuiPlayer(int playerId, PlayerAdapter adapter, GuiGame model) {
		super(playerId, adapter);
		this.model = model;
	}

	@Override
	protected void clearDiscardPile(TurnContext state) {
		super.clearDiscardPile(state);
		model.getView().playerClearedDiscardPile(this);
	}

	@Override
	protected void pickUpPile(TurnContext state, String message) {
		super.pickUpPile(state, message);
		if (!state.pickedUp.isEmpty())
			model.getView().playerPickedUpCards(this, message);
	}

	@Override
	public TurnContext playTurn(Game g) {
		model.getView().playerStartedTurn(this);
		TurnContext cx = super.playTurn(g);
		model.getView().playerEndedTurn(this);
		return cx;
	}
}
