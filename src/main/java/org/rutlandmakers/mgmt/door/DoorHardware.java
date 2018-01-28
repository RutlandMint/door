package org.rutlandmakers.mgmt.door;

import java.io.IOException;
import java.io.InputStream;
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
	private String status;
	private long statusAge = 0;

	public DoorHardware(final String portName) {
		setName("Door Hardware Thread");
		setDaemon(true);
		log.info("Opening port {}", portName);
		comPort = SerialPort.getCommPort(portName);
		comPort.setBaudRate(115200);
		comPort.setNumDataBits(8);
		comPort.setParity(SerialPort.NO_PARITY);
		comPort.setNumStopBits(1);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
		comPort.openPort();
		log.info("Opened port {}", comPort);
	}

	public void run() {
		try {
			final InputStream in = comPort.getInputStream();
			final StringBuffer s = new StringBuffer();
			while (true) {
				final int c = in.read();
				if (c == '\n') {
					process(s.toString().trim());
					s.setLength(0);
				} else {
					s.append((char) c);
				}
			}
		} catch (Exception e) {
			log.error("Error in hardware thread ", e);
		}
	}

	public void process(final String line) {
		if (line.startsWith("#")) {
			log.info("Door Debug: {}", line);
		} else if (line.startsWith("STATUS=")) {
			status = line.substring(7);
			statusAge = System.currentTimeMillis();
		} else if (line.startsWith("CARD=")) {
			final String card = line.substring(5);
			log.info("Card Read: {}", card);
			cardListeners.forEach(c -> c.accept(card));
		} else {
			log.warn("Unrecognized output from door {}", line);
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
		return new JSONObject()//
				.put("statusAgeSeconds", (System.currentTimeMillis() - statusAge) / 1000)//
				.put("status", status);
	}
}
