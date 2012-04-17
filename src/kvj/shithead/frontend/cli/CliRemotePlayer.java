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
	public Card.Rank chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile) {
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

		super.chooseFaceUp(g);

		System.out.println();
	}

	@Override
	public TurnContext playTurn(Game g) {
		System.out.println("Waiting on Player " + (getPlayerId() + 1) + "...");

		TurnContext state = super.playTurn(g);

		System.out.println();

		return state;
	}
}
