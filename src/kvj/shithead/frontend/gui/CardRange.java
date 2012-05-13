package kvj.shithead.frontend.gui;

public class CardRange {
	private CardRange previous;
	private int count;

	public CardRange(CardRange previous) {
		this.previous = previous;
	}

	public int getStart() {
		return previous != null ? (previous.getStart() + previous.getLength()) : 0;
	}

	public int getLength() {
		return count;
	}

	public void lengthen(int delta) {
		count += delta;
	}
}
