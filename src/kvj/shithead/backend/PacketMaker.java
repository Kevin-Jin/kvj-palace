package kvj.shithead.backend;

import java.util.List;

public class PacketMaker {
	public static final byte //op codes
		ADD_PLAYER = 0x01,
		DECK = 0x02,
		SELECT_CARD = 0x03,
		END_TURN = 0x04
	;

	public static byte[] serializedDeck(List<Card.Rank> cards) {
		byte[] message = new byte[cards.size() + 1];
		assert message.length == 53;
		int i = 0;
		message[i++] = PacketMaker.DECK;
		for (Card.Rank card : cards)
			message[i++] = (byte) card.ordinal();
		return message;
	}

	public static byte[] selectCard(Card.Rank selected) {
		return new byte[] { SELECT_CARD, (byte) selected.ordinal() };
	}

	public static byte[] endTurn() {
		return new byte[] { END_TURN };
	}
}
