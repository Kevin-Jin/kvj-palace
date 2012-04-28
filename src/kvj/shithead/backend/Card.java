package kvj.shithead.backend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Card {
	public enum Rank {
		TWO("2"),
		THREE("3"),
		FOUR("4"),
		FIVE("5"),
		SIX("6"),
		SEVEN("7"),
		EIGHT("8"),
		NINE("9"),
		TEN("10"),
		JACK("J"),
		QUEEN("Q"),
		KING("K"),
		ACE("A");

		private static final Map<String, Rank> cache;

		static {
			Map<String, Rank> namesToRanks = new HashMap<String, Rank>();
			for (Rank r : Rank.values()) {
				namesToRanks.put(r.toString(), r);
				namesToRanks.put(r.altStr, r);
			}
			cache = Collections.unmodifiableMap(namesToRanks);
		}

		private String altStr;

		private Rank(String alternate) {
			altStr = alternate;
		}

		public String getShorthandNotation() {
			return altStr;
		}

		public static Rank getRankByText(String name) {
			return cache.get(name.toUpperCase());
		}
	}

	public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }

	private static final Map<Suit, Map<Rank, Card>> cache;

	static {
		Map<Suit, Map<Rank, Card>> suits = new HashMap<Suit, Map<Rank, Card>>();
		for (Suit s : Suit.values()) {
			Map<Rank, Card> cards = new HashMap<Rank, Card>();
			for (Rank r : Rank.values())
				cards.put(r, new Card(s, r));
			suits.put(s, Collections.unmodifiableMap(cards));
		}
		cache = Collections.unmodifiableMap(suits);
	}

	public static Card valueOf(Suit suit, Rank rank) {
		if (suit == null || rank == null)
			return null;
		return cache.get(suit).get(rank);
	}

	private final Suit suit;
	private final Rank rank;

	private Card(Suit suit, Rank rank) {
		this.suit = suit;
		this.rank = rank;
	}

	public Suit getSuit() {
		return suit;
	}

	public Rank getRank() {
		return rank;
	}

	public byte serialize() {
		//4 bits for rank (and we'll only need 2 bits for suit)
		return (byte) (suit.ordinal() << 4 | rank.ordinal());
	}

	public static Card deserialize(byte code) {
		return valueOf(Suit.values()[code >>> 4], Rank.values()[code & 0xF]);
	}
}
