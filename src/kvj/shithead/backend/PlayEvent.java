package kvj.shithead.backend;

public abstract class PlayEvent {
	public abstract String toString();

	public static class HandToFaceUp extends PlayEvent {
		private Card card;

		public HandToFaceUp(Card card) {
			this.card = card;
		}

		@Override
		public String toString() {
			return "put " + card.getRank().toString() + " in face up";
		}
	}

	public static class CardPlayed extends PlayEvent {
		private Card card;

		public CardPlayed(Card card) {
			this.card = card;
		}

		@Override
		public String toString() {
			return "played " + card.getRank().toString();
		}
	}

	public static class PileCleared extends PlayEvent {
		@Override
		public String toString() {
			return "cleared pile";
		}
	}

	public static class PilePickedUp extends PlayEvent {
		@Override
		public String toString() {
			return "picked up pile";
		}
	}
}
