package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
	private final List<Card> cards;

	public Deck() {
		cards = new ArrayList<Card>();
	}

	public void populate() {
		for (Card.Suit s : Card.Suit.values())
			for (Card.Rank r : Card.Rank.values())
				cards.add(Card.valueOf(s, r));
		Collections.shuffle(cards);
	}

	public void addCards(List<Card> cards) {
		this.cards.addAll(cards);
	}

	public Card pop() {
		return cards.remove(cards.size() - 1);
	}

	public boolean isEmpty() {
		return cards.isEmpty();
	}

	public int size() {
		return cards.size();
	}

	public List<Card> getList() {
		return cards;
	}
}
