package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
	private final List<Card.Rank> cards;

	public Deck() {
		cards = new ArrayList<Card.Rank>();
	}

	public void populate() {
		for (Card.Rank c : Card.Rank.values())
			for (int i = 0; i < 4; i++)
				cards.add(c);
		Collections.shuffle(cards);
	}

	public void addCards(List<Card.Rank> cards) {
		this.cards.addAll(cards);
	}

	public Card.Rank pop() {
		return cards.remove(0);
	}

	public boolean isEmpty() {
		return cards.isEmpty();
	}

	public int size() {
		return cards.size();
	}

	public List<Card.Rank> getList() {
		return cards;
	}
}
