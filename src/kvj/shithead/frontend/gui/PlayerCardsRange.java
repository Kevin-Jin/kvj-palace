package kvj.shithead.frontend.gui;

public class PlayerCardsRange {
	private int startIndex;
	private int faceDownLength;
	private int faceUpLength;
	private int handLength;

	public int getFaceDownStart() {
		return startIndex;
	}

	public int getFaceDownLength() {
		return faceDownLength;
	}

	public int getFaceUpStart() {
		return startIndex + faceDownLength;
	}

	public int getFaceUpLength() {
		return faceUpLength;
	}

	public int getHandStart() {
		return startIndex + faceDownLength + faceUpLength;
	}

	public int getHandLength() {
		return handLength;
	}

	public int getEndIndexPlusOne() {
		return startIndex + faceDownLength + faceUpLength + handLength;
	}

	public void shifted(int delta) {
		startIndex += delta;
	}

	public void faceDownLengthened(int delta) {
		faceDownLength += delta;
	}

	public void faceUpLengthened(int delta) {
		faceUpLength += delta;
	}

	public void handLengthened(int delta) {
		handLength += delta;
	}
}
