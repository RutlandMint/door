package org.rutlandmint.mgmt.door.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.DoorHardware.DoorState;
import org.rutlandmint.mgmt.door.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileEventLog implements EventLog {

	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	private final String dir;
	private long lastModified = System.currentTimeMillis();

	private File getCurrentFile() {
		return new File(dir + "/door-" + DATE_FORMAT.format(new Date()) + ".log");
	}

	public FileEventLog(@Value("${accessLogs}") final String dir) throws IOException {
		this.dir = dir;
		final File d = new File(dir);
		if (!d.exists()) {
			if (!new File(dir).mkdir()) {
				throw new IOException("Can't create access log directory");
			}
		}
		if (!d.isDirectory()) {
			throw new IOException("Access log directory is not a directory");
		}
	}

	@Override
	public void unknownCard(final String cardNum) throws IOException {
		log("Unknown Card [" + cardNum + "]", "Denied");
	}

	@Override
	public void accessGranted(final Member member, final AccessResult res) throws IOException {
		log(member.name, "Access Granted: " + res.message);
	}

	@Override
	public void accessDenied(final Member member, final AccessResult res) throws IOException {
		log(member.name, "Access Denied: " + res.message);
	}

	@Override
	public void frontDoor(final DoorState ds) throws IOException {
		log("[Front Door]", ds.toString());
	}

	@Override
	public void admin(final String user, final String action) throws IOException {
		log(user, action);
	}

	private synchronized void log(final String who, final String action) throws IOException {
		try (FileWriter w = new FileWriter(getCurrentFile(), true)) {
			w.write(TIME_FORMAT.format(new Date()) + "\t" + who + "\t" + action + "\n");
			lastModified = System.currentTimeMillis();
		}
	}

	public JSONObject getLog() throws IOException {
		return getLog(getCurrentFile());
	}

	public long getLastModified() {
		return 1000 * (lastModified / 1000);
	}

	private synchronized JSONObject getLog(final File f) throws IOException {
		final JSONObject ret = new JSONObject();
		final JSONArray a = new JSONArray();
		ret.put("accessLog", a);
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] ll = line.split("\t");
				a.put(new JSONObject()//
						.put("time", ll[0])//
						.put("name", ll[1])//
						.put("action", ll[2]));
			}
		}
		return ret;
	}
}
