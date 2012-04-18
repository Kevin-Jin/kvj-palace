package kvj.shithead.frontend.cli;

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
	public Card.Rank chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile) {
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
			boolean notACard = false, notInHand = false, notLegal = false;
			while ((notACard = (selection == null)) || (notInHand = !state.currentPlayable.contains(selection)) || (notLegal = checkDiscardPile && !state.g.isMoveLegal(selection))) {
				if (notACard)
					System.out.print("Your selection is not a valid card. Try again: ");
				else if (notInHand)
					System.out.print("You do not have a " + selection + " in your hand. Try again: ");
				else if (notLegal)
					System.out.print("You may not put a " + selection + " on top of a " + state.g.getTopCard() + ". Try again: ");
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
		super.chooseFaceUp(g);

		if (clearScreen) {
			System.out.println("Press enter to continue...");
			scan.nextLine();
			clearScreen();
		} else {
			System.out.println();
		}
	}

	@Override
	protected void switchToHand(TurnContext state) {
		super.switchToHand(state);
		System.out.println("Your hand: " + state.currentPlayable);
	}

	@Override
	protected void switchToFaceUp(TurnContext state) {
		if (state.currentPlayable == getHand())
			System.out.print("You have exhausted your hand. ");
		super.switchToFaceUp(state);
		System.out.println("Your face up cards: " + state.currentPlayable);
	}

	@Override
	protected void switchToFaceDown(TurnContext state) {
		if (state.currentPlayable == getHand())
			System.out.print("You have exhausted your hand. ");
		else if (state.currentPlayable == getFaceUp())
			System.out.print("You have exhausted your face up cards. ");
		super.switchToFaceDown(state);
		System.out.println("You must now choose from your face down cards.");
	}

	@Override
	protected void outOfCards(TurnContext state) {
		if (state.currentPlayable == getHand())
			System.out.print("You have exhausted your hand. ");
		else if (state.currentPlayable == getFaceDown())
			System.out.print("You have exhausted your face down cards. ");
		super.outOfCards(state);
		System.out.println("You have no cards remaining.");
	}

	@Override
	protected void clearDiscardPile(TurnContext state) {
		super.clearDiscardPile(state);
		System.out.print("The discard pile has been cleared. ");
		if (state.selection == null) {
			assert state.g.canDraw() && state.currentPlayable.isEmpty();
			System.out.println("Since cards can be drawn, you must draw and you cannot put down any more cards.");
		}
	}

	@Override
	protected void wildCardPlayed(TurnContext state) {
		System.out.print("You have put down a wildcard. ");
		if (state.selection == null) {
			assert state.g.canDraw() && state.currentPlayable.isEmpty();
			System.out.println("Since cards can be drawn, you must draw and you cannot put down any more cards.");
		}
	}

	@Override
	protected void cardsPickedUp(TurnContext state) {
		if (!state.pickedUp.isEmpty())
			System.out.println("You picked up " + state.pickedUp + ".");
		else
			System.out.println("You did not pick up any cards.");
	}

	@Override
	protected void putCard(TurnContext state) {
		super.putCard(state);
		System.out.println("Played " + state.selection + ".");
	}

	@Override
	protected void pickUpPile(TurnContext state, String message) {
		System.out.println(message);
		super.pickUpPile(state, message);
	}

	@Override
	public TurnContext playTurn(Game g) {
		if (clearScreen) {
			System.out.println("Player " + (getPlayerId() + 1) + " may press enter when ready...");
			scan.nextLine();
		} else {
			System.out.println();
		}

		TurnContext state = super.playTurn(g);

		if (clearScreen) {
			System.out.println("Press enter to continue...");
			scan.nextLine();
			clearScreen();
		} else {
			System.out.println();
		}

		return state;
	}
}
