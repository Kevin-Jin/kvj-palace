package kvj.shithead.frontend.gui;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.PacketMaker;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public class GuiRemotePlayer extends GuiPlayer {
	protected final Client client;

	public GuiRemotePlayer(int playerId, PlayerAdapter adapter, Client client, GuiGame model) {
		super(playerId, adapter, model);
		this.client = client;
	}

	@Override
	public Card chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile) {
		Card selection = null;
		if (client.fillBuffer(1)) {
			if (client.buffer[0] == PacketMaker.SELECT_CARD) {
				if (client.fillBuffer(2)) {
					selection = Card.deserialize(client.buffer[1]);
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
	protected void moveFromHandToFaceUp(TurnContext state) {
		super.moveFromHandToFaceUp(state);
		model.getView().remotePlayerPutCard(this, state.selection);
	}

	@Override
	protected void putCard(TurnContext state) {
		super.putCard(state);
		model.getView().remotePlayerPutCard(this, state.selection);
	}
}
