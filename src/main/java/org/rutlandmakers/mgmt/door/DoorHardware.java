package org.rutlandmakers.mgmt.door;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoorHardware extends Thread {
	private static final Logger log = LoggerFactory.getLogger(DoorHardware.class);
	private final String port;
	private final Set<Consumer<String>> cardListeners = new HashSet<>();

	public DoorHardware(final String port) {
		this.port = port;
		setName("Door Hardware Thread");
		setDaemon(true);
	}

	public void addCardListener(final Consumer<String> listener) {
		cardListeners.add(listener);
	}

	public void unlockBriefly() {
		log.debug("Unlocking Door Briefly.");
	}

	public JSONObject getStatus() {
		return new JSONObject();
	}
}
