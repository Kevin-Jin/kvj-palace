package kvj.shithead.frontend.cli;

import java.util.List;
import java.util.Scanner;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

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
	public Card chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile, boolean canSkip) {
		Card selection = null;
		if (sameRank) {
			assert canSkip;
			System.out.print("You have another " + state.selection.getRank() + ". Would you like to put it down? (y/n): ");
			String input = scan.nextLine();
			while (!input.equalsIgnoreCase("Y") && !input.equalsIgnoreCase("YES") && !input.equalsIgnoreCase("N") && !input.equalsIgnoreCase("NO")) {
				System.out.print("Please type Y (yes) or N (no): ");
				input = scan.nextLine();
			}
			if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("YES"))
				selection = getCardOfAnySuit(state.currentPlayable, state.selection.getRank());
		} else if (!state.blind) {
			if (canSkip)
				selectText += " (0 to end turn)";
			selectText += ": ";
			System.out.print(selectText);
			String input = scan.nextLine();
			Card.Rank selectionRank = Card.Rank.getRankByText(input);
			boolean notACard = false, notInHand = false, notLegal = false;
			while ((!canSkip || !input.equals("0")) && ((notACard = (selectionRank == null)) || (notInHand = (selection = getCardOfAnySuit(state.currentPlayable, selectionRank)) == null) || (notLegal = checkDiscardPile && !state.g.isMoveLegal(selection)))) {
				if (notACard)
					System.out.print("Your selection is not a valid card. Try again: ");
				else if (notInHand)
					System.out.print("You do not have a " + selectionRank + " in your hand. Try again (0 to end turn): ");
				else if (notLegal)
					System.out.print("You may not put a " + selectionRank + " on top of a " + state.g.getTopCardRank() + ". Try again (0 to end turn): ");
				input = scan.nextLine();
				selectionRank = Card.Rank.getRankByText(input);
				selection = null;
			}
		} else {
			assert canSkip;
			System.out.print("Choose a number from 1 to " + state.currentPlayable.size() + " (inclusive) (0 to end turn): ");
			String input = scan.nextLine();
			int index;
			while (!isNumber(input) || (index = Integer.parseInt(input)) < 0 || index > state.currentPlayable.size()) {
				System.out.print(input + " is not a number from 1 to " + state.currentPlayable.size() + " inclusive. Try again (0 to end turn): ");
				input = scan.nextLine();
				selection = null;
			}
			if (index != 0)
				selection = state.currentPlayable.get(index - 1);
		}

		adapter.cardChosen(selection);
		return selection;
	}

	@Override
	public TurnContext chooseFaceUp(Game g) {
		if (clearScreen) {
			System.out.println("Player " + (getPlayerId() + 1) + " may press enter when ready...");
			scan.nextLine();
		} else {
			System.out.println();
		}

		System.out.println("You may choose any three of these cards to place as your face up cards: " + CliGameUtil.listRanks(getHand()) + ".");
		TurnContext state = super.chooseFaceUp(g);

		if (clearScreen) {
			System.out.println("Press enter to continue...");
			scan.nextLine();
			clearScreen();
		} else {
			System.out.println();
		}

		return state;
	}

	@Override
	protected void switchToHand(TurnContext state) {
		super.switchToHand(state);
		System.out.println("Your hand: " + CliGameUtil.listRanks(state.currentPlayable));
	}

	@Override
	protected void switchToFaceUp(TurnContext state) {
		if (state.currentPlayable == getHand())
			System.out.print("You have exhausted your hand. ");
		super.switchToFaceUp(state);
		System.out.println("Your face up cards: " + CliGameUtil.listRanks(state.currentPlayable));
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
		if (state.selection == null && state.g.canDraw() && state.currentPlayable.isEmpty()) {
			System.out.println("Since cards can be drawn, you must draw and you cannot put down any more cards.");
		} else if (state.won) {
			System.out.println();
		}
	}

	@Override
	protected void wildCardPlayed(TurnContext state) {
		System.out.print("You have put down a wildcard. ");
		if (state.selection == null && state.g.canDraw() && state.currentPlayable.isEmpty()) {
			System.out.println("Since cards can be drawn, you must draw and you cannot put down any more cards.");
		} else if (state.won) {
			System.out.println();
		}
	}

	@Override
	protected void turnEndedEarly() {
		System.out.println("You ended your turn early.");
	}

	@Override
	protected void cardsPickedUp(TurnContext state) {
		if (!state.pickedUp.isEmpty())
			System.out.println("You picked up " + CliGameUtil.listRanks(state.pickedUp) + ".");
		else
			System.out.println("You did not pick up any cards.");
	}

	@Override
	protected void putCard(TurnContext state) {
		super.putCard(state);
		System.out.println("You " + state.events.get(state.events.size() - 1) + ".");
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

	private static Card getCardOfAnySuit(List<Card> hand, Card.Rank rank) {
		if (rank == null)
			return null;
		for (Card card : hand)
			if (card.getRank() == rank)
				return card;
		return null;
	}
}
