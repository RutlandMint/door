package org.rutlandmakers.mgmt.door;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccessLog {

	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	private final String dir;
	private long lastModified = System.currentTimeMillis();

	private File getCurrentFile() {
		return new File(dir + "/door-" + DATE_FORMAT.format(new Date()) + ".log");
	}

	public AccessLog(final String dir) throws IOException {
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

	public synchronized void log(final String who, final String action) throws IOException {
		try (FileWriter w = new FileWriter(getCurrentFile(), true)) {
			w.write(TIME_FORMAT.format(new Date()) + "\t" + who + "\t" + action + "\n");
			lastModified = System.currentTimeMillis();
		}
	}

	public JSONObject getLog() throws IOException {
		return getLog(getCurrentFile());
	}

	public long getLastModified() {
		return 1000*(lastModified/1000);
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
