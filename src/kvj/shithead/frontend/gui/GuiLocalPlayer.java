package kvj.shithead.frontend.gui;

import java.util.concurrent.CountDownLatch;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public class GuiLocalPlayer extends GuiPlayer {
	private CountDownLatch inputWait;
	private Card chosen;

	public GuiLocalPlayer(int playerId, PlayerAdapter adapter, GuiGame model) {
		super(playerId, adapter, model);
		inputWait = new CountDownLatch(1);
	}

	@Override
	public Card chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile) {
		try {
			inputWait.await();
			inputWait = new CountDownLatch(1);
			adapter.cardChosen(chosen);
			return chosen;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void cardChosen(Card value) {
		chosen = value;
		inputWait.countDown();
	}
}
