package kvj.shithead.frontend.cli;

import java.util.Collections;
import java.util.Scanner;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

//game rules:
//all cards are drawn at end of turn, and not after a card is played
// so if draw deck is not empty and player used his entire hand,
// he has to end his turn early and draw more cards even if one of
// the drawn cards can be played.
//TEN allows any other card to be played, TWO clears the pile
// both can be placed after any card and both let the player put
// down another card before the game moves on to the next player
//4 consecutive cards of the same rank also clears the pile
//the player who picks up the discard pile gets to put down one of
// his cards to start the new pile before the game moves on to the
// next player
public class CliLocalPlayer extends Player {
	private static void clearScreen() {
		char[] newlines = new char[24];
		for (int i = 0; i < newlines.length; i++)
			newlines[i] = '\n';
		System.out.println(String.valueOf(newlines));
	}

	private static boolean isNumber(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private final Scanner scan;
	private final boolean clearScreen;

	public CliLocalPlayer(int playerId, PlayerAdapter adapter, Scanner scan, boolean clearScreen) {
		super(playerId, adapter);
		this.scan = scan;
		this.clearScreen = clearScreen;
	}

	@Override
	public Card.Rank chooseCard(TurnContext state, String selectText, boolean sameRank, boolean alwaysLegal, boolean noSpecialCards) {
		Card.Rank selection;
		if (sameRank) {
			System.out.print("You have another " + state.selection + ". Would you like to put it down? (y/n): ");
			String yesNo = scan.nextLine();
			while (!yesNo.equalsIgnoreCase("Y") && !yesNo.equalsIgnoreCase("YES") && !yesNo.equalsIgnoreCase("N") && !yesNo.equalsIgnoreCase("NO")) {
				System.out.print("Please type Y (yes) or N (no): ");
				yesNo = scan.nextLine();
			}
			if (yesNo.equalsIgnoreCase("N") || yesNo.equalsIgnoreCase("NO")) {
				selection = null;
			} else {
				selection = state.selection;
			}
		} else if (!state.blind) {
			System.out.print(selectText);
			selection = Card.Rank.getRankByText(scan.nextLine());
			boolean notACard = false, notInHand = false, notLegal = false, specialCard = false;
			while ((notACard = (selection == null)) || (notInHand = !state.currentPlayable.contains(selection)) || (notLegal = !alwaysLegal && !state.g.isMoveLegal(selection)) || (specialCard = noSpecialCards && (selection == Card.Rank.TWO || selection == Card.Rank.TEN))) {
				if (notACard)
					System.out.print("Your selection is not a valid card. Try again: ");
				else if (notInHand)
					System.out.print("You do not have a " + selection + " in your hand. Try again: ");
				else if (notLegal)
					System.out.print("You may not put a " + selection + " on top of a " + state.g.getTopCard() + ". Try again: ");
				else if (specialCard)
					System.out.print("You may not start a pile with a wild or clear card. Try again: ");
				selection = Card.Rank.getRankByText(scan.nextLine());
			}
		} else {
			System.out.print("Choose a number from 1 to " + state.currentPlayable.size() + " (inclusive): ");
			int index;
			String input = scan.nextLine();
			while (!isNumber(input) || (index = Integer.parseInt(input)) < 1 || index > state.currentPlayable.size()) {
				System.out.print(input + " is not a number from 1 to " + state.currentPlayable.size() + " inclusive. Try again: ");
				input = scan.nextLine();
			}
			selection = state.currentPlayable.get(index - 1);
		}

		adapter.cardChosen(selection);
		return selection;
	}

	@Override
	public void chooseFaceUp(Game g) {
		if (clearScreen) {
			System.out.println("Player " + (getPlayerId() + 1) + " may press enter when ready...");
			scan.nextLine();
		} else {
			System.out.println();
		}

		System.out.println("You may choose any three of these cards to place as your face up cards: " + getHand() + ".");
		TurnContext state = new TurnContext(g);
		state.currentPlayable = getHand();
		state.blind = false;
		for (int j = 0; j < 3; j++) {
			Card.Rank selection = chooseCard(state, "Choose one card: ", false, true, false);
			getHand().remove(selection);
			getFaceUp().add(selection);
		}

		if (clearScreen) {
			System.out.println("Press enter to continue...");
			scan.nextLine();
			clearScreen();
		} else {
			System.out.println();
		}
	}

	private boolean hasValidMove(TurnContext state) {
		for (Card.Rank c : state.currentPlayable)
			if (state.g.isMoveLegal(c))
				return true;
		return false;
	}

	private void putCard(TurnContext state) {
		state.currentPlayable.remove(state.selection);
		state.g.addToDiscardPile(state.selection);
		state.moves.add(state.selection);
		System.out.println("Played " + state.selection + ".");
	}

	private void playCard(TurnContext state) {
		putCard(state);
		if (state.currentPlayable.isEmpty()) {
			if (state.g.canDraw()) {
				state.selection = null; //end the turn
			} else if (state.currentPlayable == getHand()) {
				System.out.print("You have exhausted your hand. ");
				if (!getFaceUp().isEmpty()) {
					state.currentPlayable = getFaceUp();
					System.out.println("Your face up cards: " + state.currentPlayable);
				} else if (!getFaceDown().isEmpty()) {
					state.currentPlayable = getFaceDown();
					state.blind = true;
					System.out.println("You must now choose from your face down cards.");
				} else {
					state.won = true;
					System.out.println("You have no cards remaining.");
				}
			} else if (state.currentPlayable == getFaceUp()) {
				state.currentPlayable = getFaceDown();
				state.blind = true;
				System.out.println("You have exhausted your face up cards. You must now choose from your face down cards.");
			} else if (state.currentPlayable == getFaceDown()) {
				state.won = true;
				System.out.println("You have exhausted your face down cards. You have no cards remaining.");
			}
		}
	}

	private void pickUpPile(TurnContext state, String message) {
		System.out.println(message);
		state.g.transferDiscardPile(state.pickedUp);
		state.pickedUpDiscardPile = true;
	}

	@Override
	public TurnContext playTurn(Game g) {
		if (clearScreen) {
			System.out.println("Player " + (getPlayerId() + 1) + " may press enter when ready...");
			scan.nextLine();
		} else {
			System.out.println();
		}

		TurnContext state = new TurnContext(g);

		if (!getHand().isEmpty()) {
			state.currentPlayable = getHand();
			System.out.println("Your hand: " + state.currentPlayable);
		} else if (!getFaceUp().isEmpty()) {
			state.currentPlayable = getFaceUp();
			System.out.println("Your face up cards: " + state.currentPlayable);
		} else if (!getFaceDown().isEmpty()) {
			state.currentPlayable = getFaceDown();
			state.blind = true;
			System.out.println("You must now choose from your face down cards.");
		}

		if (hasValidMove(state) || state.blind) {
			state.selection = chooseCard(state, "Choose first card to put down: ", false, false, false);

			if (!state.g.isMoveLegal(state.selection)) {
				assert state.blind;
				putCard(state);
				pickUpPile(state, "Your gamble failed!");
			} else {
				playCard(state);

				boolean cleared = false, wildcard = false, sameRank = false;
				while (!state.won && ((cleared = (state.g.getTopCard() == Card.Rank.TWO || state.g.getSameRankCount() == 4)) || (wildcard = (state.g.getTopCard() == Card.Rank.TEN)) || (sameRank = !state.blind && state.currentPlayable.contains(state.selection))) && state.selection != null) {
					if (cleared) {
						state.g.transferDiscardPile(null);
						System.out.print("The discard pile has been cleared. ");
					}
					if (wildcard)
						System.out.print("You have put down a wildcard. ");
					state.selection = chooseCard(state, "Choose next card to put down: ", sameRank, true, false);
					if (state.selection != null)
						playCard(state);
					cleared = wildcard = sameRank = false;
				}
				if (cleared) {
					assert state.g.canDraw();
					state.g.transferDiscardPile(null);
					System.out.println("The discard pile has been cleared. Since cards can be drawn, you must draw and you cannot put down any more cards.");
				}
				if (wildcard) {
					assert state.g.canDraw();
					System.out.println("You have put down a wildcard. Since cards can be drawn, you must draw and you cannot put down any more cards.");
				}

				while (state.g.canDraw() && getHand().size() + state.pickedUp.size() < 3)
					state.pickedUp.add(state.g.draw());
			}
		} else {
			pickUpPile(state, "You cannot make any moves.");
		}

		if (!state.pickedUp.isEmpty()) {
			getHand().addAll(state.pickedUp);
			Collections.sort(getHand());
			System.out.println("You picked up " + state.pickedUp + ".");
			if (state.pickedUpDiscardPile) {
				state.currentPlayable = getHand();
				state.blind = false;
				System.out.println("Your hand: " + getHand());

				//TODO: should we allow player to stack cards when putting
				//one down with putCardAfterPickup? cards of only the same
				//rank, or can we use 10s and 2s?
				state.selection = chooseCard(state, "Put down a card to start a new discard pile: ", false, false, true);
				putCard(state);
			}
		} else {
			System.out.println("You did not pick up any cards.");
		}

		if (clearScreen) {
			System.out.println("Press enter to continue...");
			scan.nextLine();
			clearScreen();
		} else {
			System.out.println();
		}

		adapter.turnEnded();
		return state;
	}
}
