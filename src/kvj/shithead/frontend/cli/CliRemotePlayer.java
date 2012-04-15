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

	public Card.Rank chooseCard(TurnContext state, String selectText, boolean sameRank, boolean alwaysLegal, boolean noSpecialCards) {
		Card.Rank selection = null;
		if (client.fillBuffer(1)) {
			if (client.buffer[0] == PacketMaker.SELECT_CARD) {
				if (client.fillBuffer(2)) {
					selection = Card.Rank.values()[client.buffer[1]];
					client.compactBuffer(2);
				}
			} else if (client.buffer[0] == PacketMaker.END_TURN) {
				client.compactBuffer(1);
			}
		}

		adapter.cardChosen(selection);
		return selection;
	}

	@Override
	public void chooseFaceUp(Game g) {
		System.out.println("Waiting on Player " + (getPlayerId() + 1) + "...");

		for (int i = 0; i < 3; i++) {
			Card.Rank card = chooseCard(null, null, false, false, false);
			getHand().remove(card);
			getFaceUp().add(card);
		}

		System.out.println();
	}

	@Override
	public TurnContext playTurn(Game g) {
		System.out.println("Waiting on Player " + (getPlayerId() + 1) + "...");

		TurnContext state = new TurnContext(g);

		if (!getHand().isEmpty()) {
			state.currentPlayable = getHand();
		} else if (!getFaceUp().isEmpty()) {
			state.currentPlayable = getFaceUp();
		} else if (!getFaceDown().isEmpty()) {
			state.currentPlayable = getFaceDown();
			state.blind = true;
		}

		Card.Rank card;
		while ((card = chooseCard(null, null, false, false, false)) != null) {
			boolean inCurrentPlayable = state.currentPlayable.remove(card);
			if (state.blind && !state.g.isMoveLegal(card)) {
				state.g.addToDiscardPile(card);
				state.g.transferDiscardPile(getHand());
				state.currentPlayable = getHand();
				state.blind = false;
			} else if (!inCurrentPlayable || !state.g.isMoveLegal(card)) {
				//either this card wasn't in our hand/face up before (and we picked up the pile first)
				//or we place a card from our hand that is lower in value than the current pile top card
				//checking CliLocalPlayer.hasValidMove(state.currentPlayable) before doing
				//state.currentPlayable.remove(card) will have the same effect as this check
				state.g.transferDiscardPile(getHand());
				if (!inCurrentPlayable)
					state.currentPlayable.remove(card);
				state.g.addToDiscardPile(card);
				state.currentPlayable = getHand();
			} else {
				state.g.addToDiscardPile(card);
				if (card == Card.Rank.TWO || state.g.getSameRankCount() == 4)
					state.g.transferDiscardPile(null);
			}

			if (state.currentPlayable.isEmpty()) {
				if (state.currentPlayable == getHand()) {
					if (!getFaceUp().isEmpty()) {
						state.currentPlayable = getFaceUp();
					} else if (!getFaceDown().isEmpty()) {
						state.currentPlayable = getFaceDown();
						state.blind = true;
					} else {
						state.won = true;
					}
				} else if (state.currentPlayable == getFaceUp()) {
					state.currentPlayable = getFaceDown();
					state.blind = true;
				} else if (state.currentPlayable == getFaceDown()) {
					state.won = true;
				}
			}
		}

		while (state.g.canDraw() && getHand().size() < 3)
			getHand().add(state.g.draw());

		System.out.println();

		adapter.turnEnded();
		return state;
	}
}
