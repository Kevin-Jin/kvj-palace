package kvj.shithead.frontend.gui;

public class PlayerCardsRange {
	private final CardRange faceDownRange;
	private final CardRange faceUpRange;
	private final CardRange handRange;

	public PlayerCardsRange(CardRange previous) {
		faceDownRange = new CardRange(previous);
		faceUpRange = new CardRange(faceDownRange);
		handRange = new CardRange(faceUpRange);
	}

	public CardRange getFaceDownRange() {
		return faceDownRange;
	}

	public CardRange getFaceUpRange() {
		return faceUpRange;
	}

	public CardRange getHandRange() {
		return handRange;
	}
}
