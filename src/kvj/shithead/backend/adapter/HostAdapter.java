package kvj.shithead.backend.adapter;

import java.util.List;

import kvj.shithead.backend.Client;

public class HostAdapter extends BroadcastAdapter {
	private final Client self;
	private final List<Client> others;

	public HostAdapter(Client client, List<Client> connected) {
		self = client;
		others = connected;
	}

	@Override
	protected void broadcast(byte[] message) {
		for (Client client : others)
			if (client != self)
				client.send(message);
	}
}
