package kvj.shithead.backend.adapter;

import kvj.shithead.backend.Card;

public interface PlayerAdapter {
	public void cardChosen(Card.Rank selected);
	public void turnEnded();
}
