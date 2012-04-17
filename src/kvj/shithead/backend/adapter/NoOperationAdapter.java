package kvj.shithead.backend.adapter;

import kvj.shithead.backend.Card;

public class NoOperationAdapter implements PlayerAdapter {
	private static final NoOperationAdapter singleton = new NoOperationAdapter();

	private NoOperationAdapter() {
		
	}

	@Override
	public void cardChosen(Card.Rank selected) {
		
	}

	public static NoOperationAdapter getInstance() {
		return singleton;
	}
}
