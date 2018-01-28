package org.rutlandmakers.mgmt.door;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemberDatabase extends Thread {
	private static final Logger log = LoggerFactory.getLogger(MemberDatabase.class);

	private final WildApricot wa;
	private final File dbFile;

	private Set<Member> members;
	private Date listDate;
	private boolean fromFile = true;

	public MemberDatabase(final WildApricot wa, final File dbFile) {
		this.wa = wa;
		this.dbFile = dbFile;
		setName("Database Load Thread");
		setDaemon(true);
	}

	@Override
	public void run() {
		while (true) {
			try {
				doLoad();
				Thread.sleep(300_000);
			} catch (final Throwable e) {
				log.error("Error loading members", e);
			}
		}
	}

	public synchronized JSONObject getStatus() {
		if (members == null) {
			return new JSONObject().put("loaded", false);
		}
		return new JSONObject()//
				.put("loaded", true)//
				.put("memberCount", members.size())//
				.put("fromFile", fromFile)//
				.put("ageSeconds", (System.currentTimeMillis() - listDate.getTime()) / 1000)
				.put("date", listDate.getTime());
	}

	private void doLoad() {
		if (members == null) {
			try {
				loadFromFile();
			} catch (final IOException e) {
				log.error("Error loading from db file", e);
			}
		}
		try {
			loadFromWildApricot();
			try {
				writeToFile();
			} catch (final IOException e) {
				log.error("Error writing to db file", e);
			}
		} catch (final IOException e) {
			log.error("Error loading from wild apricot", e);
		}
	}

	private void loadFromWildApricot() throws IOException {
		final Set<Member> m = wa.loadMembers();
		if (m.size() == 0) {
			throw new IOException("No Members Loaded!");
		}
		synchronized (this) {
			this.members = m;
			listDate = new Date();
			fromFile = false;
			log.info("Loaded {} members from Wild Apricot", members.size());
		}
	}

	private synchronized void writeToFile() throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dbFile))) {
			oos.writeObject(members);
			log.info("Wrote {} members to {}", members.size(), dbFile.getAbsolutePath());
		}

	}

	private synchronized void loadFromFile() throws IOException {
		if (!dbFile.exists()) {
			throw new FileNotFoundException(dbFile.getAbsolutePath());
		}
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dbFile))) {
			try {
				members = (Set<Member>) ois.readObject();
				listDate = new Date(dbFile.lastModified());
				fromFile = true;
				log.info("Loaded {} members from {}", members.size(), dbFile.getAbsolutePath());
			} catch (final ClassNotFoundException e) {
				throw new IOException("Bad data found in member file.", e);
			}

		}
	}

	public synchronized Optional<Member> getMemberByAccessCard(final String cardNum) {
		if (members == null) {
			log.error("No members loaded in getMemberByAccessCard");
			return Optional.empty();
		}
		return members.stream().filter(m -> cardNum.equals(m.keyCardNumber) || cardNum.equals("100" + m.keyCardNumber))
				.findAny();
	}
}
