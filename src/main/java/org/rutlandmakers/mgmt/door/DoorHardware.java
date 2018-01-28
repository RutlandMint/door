package org.rutlandmakers.mgmt.door;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

public class DoorHardware extends Thread {
	private static final Logger log = LoggerFactory.getLogger(DoorHardware.class);
	private final Set<Consumer<String>> cardListeners = new HashSet<>();
	private final SerialPort comPort;

	public DoorHardware(final String portName) {
		setName("Door Hardware Thread");
		setDaemon(true);
		log.info("Opening port {}", portName);
		comPort = SerialPort.getCommPort(portName);
		comPort.setBaudRate(115200);
		comPort.setNumDataBits(8);
		comPort.setParity(SerialPort.NO_PARITY);
		comPort.setNumStopBits(1);
		comPort.openPort();
		log.info("Opened port {}", comPort);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

	}

	public void run() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(comPort.getInputStream()))) {
			String line;
			while (null != (line = in.readLine())) {
				if (line.startsWith("#")) {
					log.info("Door Debug: {}", line);
				} else if (line.startsWith("CARD=")) {
					final String card = line.substring(5);
					log.info("Card Read: {}", card);
					cardListeners.forEach(c -> c.accept(card));
				} else {
					log.warn("Unrecognized output from door {}", line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addCardListener(final Consumer<String> listener) {
		cardListeners.add(listener);
	}

	public synchronized void unlockBriefly() throws IOException {
		log.debug("Unlocking Door Briefly.");
		comPort.getOutputStream().write('U');
	}

	public JSONObject getStatus() {
		return new JSONObject();
	}
}
