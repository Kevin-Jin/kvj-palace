package kvj.shithead.frontend.gui;

import kvj.shithead.backend.Game;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public abstract class GuiPlayer extends Player {
	protected final GuiGame model;
	private boolean waiting;

	public GuiPlayer(int playerId, PlayerAdapter adapter, GuiGame model) {
		super(playerId, adapter);
		this.model = model;
	}

	public void startedGame() {
		currentCx = new TurnContext(model, false);
		currentCx.currentPlayable = getHand();
		currentCx.blind = false;
	}

	public void putFirstCard() {
		currentCx = null;
	}

	@Override
	public TurnContext chooseFaceUp(Game g) {
		waiting = true;
		model.getView().playerStartedTurn(this);
		TurnContext cx = super.chooseFaceUp(g);
		waiting = false;
		model.getView().playerEndedTurn(this);
		return cx;
	}

	@Override
	protected void clearDiscardPile(TurnContext state) {
		super.clearDiscardPile(state);
		model.getView().playerClearedDiscardPile(this);
	}

	@Override
	protected void cardsPickedUp(TurnContext state) {
		super.cardsPickedUp(state);
		model.getView().playerPickedUpCards(this);
	}

	@Override
	protected void pickUpPile(TurnContext state, String message) {
		super.pickUpPile(state, message);
		model.getView().drawHint(message);
	}

	@Override
	public TurnContext playTurn(Game g) {
		waiting = true;
		model.getView().playerStartedTurn(this);
		TurnContext cx = super.playTurn(g);
		waiting = false;
		model.getView().playerEndedTurn(this);
		return cx;
	}

	public boolean isThinking() {
		return waiting;
	}
}
