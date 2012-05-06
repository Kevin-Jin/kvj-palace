package kvj.shithead.frontend.cli;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.PacketMaker;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public class CliRemotePlayer extends Player {
	protected final Client client;

	public CliRemotePlayer(int playerId, PlayerAdapter adapter, Client client) {
		super(playerId, adapter);
		this.client = client;
	}

	@Override
	public Card chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile, boolean canSkip) {
		Card selection = null;
		if (client.fillBuffer(1)) {
			if (client.buffer[0] == PacketMaker.SELECT_CARD) {
				if (client.fillBuffer(2)) {
					selection = Card.deserialize(client.buffer[1]);
					client.compactBuffer(2);
				}
			}
		}

		adapter.cardChosen(selection);
		return selection;
	}

	@Override
	protected void moveFromHandToFaceUp(TurnContext state) {
		super.moveFromHandToFaceUp(state);
		System.out.println("Player " + (getPlayerId() + 1) + " " + state.events.get(state.events.size() - 1) + ".");
	}

	@Override
	public TurnContext chooseFaceUp(Game g) {
		System.out.println("Waiting on Player " + (getPlayerId() + 1) + "...");

		TurnContext state = super.chooseFaceUp(g);

		System.out.println();

		return state;
	}

	@Override
	protected void clearDiscardPile(TurnContext state) {
		super.clearDiscardPile(state);
		System.out.println("Player " + (getPlayerId() + 1) + " " + state.events.get(state.events.size() - 1) + ".");
	}

	@Override
	protected void putCard(TurnContext state) {
		super.putCard(state);
		System.out.println("Player " + (getPlayerId() + 1) + " " + state.events.get(state.events.size() - 1) + ".");
	}

	@Override
	protected void pickUpPile(TurnContext state, String message) {
		super.pickUpPile(state, message);
		System.out.println("Player " + (getPlayerId() + 1) + " " + state.events.get(state.events.size() - 1) + ".");
	}

	@Override
	public TurnContext playTurn(Game g) {
		System.out.println("Waiting on Player " + (getPlayerId() + 1) + "...");

		TurnContext state = super.playTurn(g);

		System.out.println();

		return state;
	}
}
