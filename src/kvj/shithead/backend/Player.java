package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.List;

import kvj.shithead.backend.adapter.PlayerAdapter;

public abstract class Player {
	private final List<Card.Rank> faceDown;
	private final List<Card.Rank> faceUp;
	private final List<Card.Rank> hand;
	private final int playerId;
	protected final PlayerAdapter adapter;

	public Player(int playerId, PlayerAdapter adapter) {
		faceDown = new ArrayList<Card.Rank>();
		faceUp = new ArrayList<Card.Rank>();
		hand = new ArrayList<Card.Rank>();
		this.playerId = playerId;
		this.adapter = adapter;
	}

	public int getPlayerId() {
		return playerId;
	}

	public List<Card.Rank> getFaceUp() {
		return faceUp;
	}

	public List<Card.Rank> getFaceDown() {
		return faceDown;
	}

	public List<Card.Rank> getHand() {
		return hand;
	}

	public abstract Card.Rank chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile);

	public abstract void chooseFaceUp(Game g);

	public abstract TurnContext playTurn(Game g);
}
