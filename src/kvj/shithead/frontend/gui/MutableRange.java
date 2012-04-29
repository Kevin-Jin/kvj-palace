package kvj.shithead.frontend.gui;

public class MutableRange {
	private int start;
	private int length;

	public MutableRange(int init, int length) {
		start = init;
		this.length = length;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int i) {
		start = i;
	}

	public void incrementStart() {
		start++;
	}

	public void decrementStart() {
		start--;
	}

	public void addToStart(int i) {
		start += i;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int i) {
		length = i;
	}

	public void decrementLength() {
		length--;
	}
}
