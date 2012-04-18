package kvj.shithead.backend;

public abstract class PlayEvent {
	public abstract String toString();

	public static class HandToFaceUp extends PlayEvent {
		private Card.Rank card;

		public HandToFaceUp(Card.Rank card) {
			this.card = card;
		}

		@Override
		public String toString() {
			return "put " + card.toString() + " in face up";
		}
	}

	public static class CardPlayed extends PlayEvent {
		private Card.Rank card;

		public CardPlayed(Card.Rank card) {
			this.card = card;
		}

		@Override
		public String toString() {
			return "played " + card.toString();
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
