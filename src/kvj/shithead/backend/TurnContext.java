package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.List;

public class TurnContext {
	public final Game g;
	public final boolean choosingFaceUp;
	public final List<PlayEvent> events;
	public final List<Card> pickedUp;
	public volatile List<Card> currentPlayable;
	public Card selection;
	public volatile boolean blind;
	public boolean won;

	public TurnContext(Game g, boolean choosingFaceUp) {
		this.g = g;
		this.choosingFaceUp = choosingFaceUp;
		events = new ArrayList<PlayEvent>();
		pickedUp = new ArrayList<Card>();
	}
}
