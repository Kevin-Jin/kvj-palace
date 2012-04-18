package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kvj.shithead.backend.adapter.PlayerAdapter;

//game rules:
//Player with lowest valued card (excluding special wild and clear cards)
// starts the game and the player after gets the first choice of cards to play.
//All cards are drawn at end of turn, and not after a card is played, so if the
// draw deck is not empty and player used his entire hand, he has to end his
// turn early and draw more cards even if one of the drawn cards can be played.
//TEN allows any other card to be played, TWO clears the pile. Both can be
// placed after any card and both let the player put down another card before
// play moves on to the next player.
//4 consecutive cards of the same rank also clears the pile and has the same
// effect as a TWO.
//The player who picks up the discard pile gets to put down 1 or more cards in
// his hand (including the ones he picked up) before play moves on, as long as
// the card combination is legal.
public abstract class Player {
	private final List<Card.Rank> faceDown;
	private final List<Card.Rank> faceUp;
	private final List<Card.Rank> hand;
	private final int playerId;
	protected final PlayerAdapter adapter;

	public Player(int playerId, PlayerAdapter adapter) {
		faceDown = new ArrayList<Card.Rank>();
		faceUp = new ArrayList<Card.Rank>();
		hand = new ArrayList<Card.Rank>();
		this.playerId = playerId;
		this.adapter = adapter;
	}

	public int getPlayerId() {
		return playerId;
	}

	public List<Card.Rank> getFaceUp() {
		return faceUp;
	}

	public List<Card.Rank> getFaceDown() {
		return faceDown;
	}

	public List<Card.Rank> getHand() {
		return hand;
	}

	public abstract Card.Rank chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile);

	protected void moveFromHandToFaceUp(TurnContext state) {
		getHand().remove(state.selection);
		getFaceUp().add(state.selection);
		state.events.add(new PlayEvent.HandToFaceUp(state.selection));
	}

	public TurnContext chooseFaceUp(Game g) {
		TurnContext state = new TurnContext(g);
		state.currentPlayable = getHand();
		state.blind = false;
		for (int i = 0; i < 3; i++) {
			state.selection = chooseCard(state, "Choose one card: ", false, true);
			moveFromHandToFaceUp(state);
		}
		return state;
	}

	protected void switchToHand(TurnContext state) {
		state.currentPlayable = getHand();
		state.blind = false;
	}

	protected void switchToFaceUp(TurnContext state) {
		state.currentPlayable = getFaceUp();
		state.blind = false;
	}

	protected void switchToFaceDown(TurnContext state) {
		state.currentPlayable = getFaceDown();
		state.blind = true;
	}

	protected void outOfCards(TurnContext state) {
		state.won = true;
	}

	protected void clearDiscardPile(TurnContext state) {
		state.g.transferDiscardPile(null);
		state.events.add(new PlayEvent.PileCleared());
	}

	protected void wildCardPlayed(TurnContext state) {
		
	}

	protected void cardsPickedUp(TurnContext state) {
		
	}

	protected void putCard(TurnContext state) {
		state.currentPlayable.remove(state.selection);
		state.g.addToDiscardPile(state.selection);
		state.events.add(new PlayEvent.CardPlayed(state.selection));
	}

	protected void pickUpPile(TurnContext state, String message) {
		state.g.transferDiscardPile(state.pickedUp);
		state.pickedUpDiscardPile = true;
		state.events.add(new PlayEvent.PilePickedUp());
	}

	private boolean hasValidMove(TurnContext state) {
		for (Card.Rank c : state.currentPlayable)
			if (state.g.isMoveLegal(c))
				return true;
		return false;
	}

	private void playCard(TurnContext state) {
		putCard(state);
		if (state.currentPlayable.isEmpty())
			if (state.g.canDraw())
				state.selection = null; //end the turn
			else if (state.currentPlayable == getHand())
				if (!getFaceUp().isEmpty())
					switchToFaceUp(state);
				else if (!getFaceDown().isEmpty())
					switchToFaceDown(state);
				else
					outOfCards(state);
			else if (state.currentPlayable == getFaceUp())
				switchToFaceDown(state);
			else if (state.currentPlayable == getFaceDown())
				outOfCards(state);
	}

	private void finishTurn(TurnContext state) {
		playCard(state);

		boolean cleared = false, wildcard = false, sameRank = false;
		while (!state.won && ((cleared = (state.g.getTopCard() == Card.Rank.TWO || state.g.getSameRankCount() == 4)) || (wildcard = (state.g.getTopCard() == Card.Rank.TEN)) || (sameRank = !state.blind && state.currentPlayable.contains(state.selection))) && state.selection != null) {
			if (cleared)
				clearDiscardPile(state);
			if (wildcard)
				wildCardPlayed(state);
			state.selection = chooseCard(state, "Choose next card to put down: ", sameRank, false);
			if (state.selection != null)
				playCard(state);
			cleared = wildcard = sameRank = false;
		}
		if (cleared)
			clearDiscardPile(state);
		if (wildcard)
			wildCardPlayed(state);

		while (state.g.canDraw() && getHand().size() + state.pickedUp.size() < 3)
			state.pickedUp.add(state.g.draw());
	}

	public TurnContext playTurn(Game g) {
		TurnContext state = new TurnContext(g);

		if (!getHand().isEmpty())
			switchToHand(state);
		else if (!getFaceUp().isEmpty())
			switchToFaceUp(state);
		else if (!getFaceDown().isEmpty())
			switchToFaceDown(state);

		if (hasValidMove(state) || state.blind) {
			state.selection = chooseCard(state, "Choose first card to put down: ", false, true);

			if (!state.g.isMoveLegal(state.selection)) {
				assert state.blind;
				putCard(state);
				pickUpPile(state, "You lost the gamble!");
			} else {
				finishTurn(state);
			}
		} else {
			pickUpPile(state, "You cannot make any moves.");
		}

		if (!state.pickedUp.isEmpty()) {
			getHand().addAll(state.pickedUp);
			Collections.sort(getHand());
			cardsPickedUp(state);
			if (state.pickedUpDiscardPile) {
				switchToHand(state);

				state.selection = chooseCard(state, "Choose first card of new discard pile: ", false, false);
				finishTurn(state);
			}
		} else {
			cardsPickedUp(state);
		}

		return state;
	}
}
