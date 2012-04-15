package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.List;

public class TurnContext {
	public final Game g;
	public final List<Card.Rank> moves;
	public final List<Card.Rank> pickedUp;
	public boolean pickedUpDiscardPile = false;
	public List<Card.Rank> currentPlayable;
	public Card.Rank selection;
	public boolean blind;
	public boolean endTurn;
	public boolean won;

	public TurnContext(Game g) {
		this.g = g;
		moves = new ArrayList<Card.Rank>();
		pickedUp = new ArrayList<Card.Rank>();
	}
}
