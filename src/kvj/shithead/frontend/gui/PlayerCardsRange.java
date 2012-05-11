package kvj.shithead.frontend.gui;

public class PlayerCardsRange {
	private volatile int startIndex;
	private volatile int faceDownLength;
	private volatile int faceUpLength;
	private volatile int handLength;

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

	//the following methods aren't atomic, but we never
	//should have high enough concurrency that it would
	//be necessary - we only made the variables volatile
	//since they can be accessed from the EDT or the
	//game loop thread, but never simultaneously
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
