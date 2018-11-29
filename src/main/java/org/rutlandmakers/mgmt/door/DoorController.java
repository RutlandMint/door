package org.rutlandmakers.mgmt.door;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.rutlandmakers.mgmt.door.AccessController.AccessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoorController {
	private static final Logger log = LoggerFactory.getLogger(DoorController.class);

	public static void main(final String[] args) throws Exception {
		final String configFile = args.length == 1 ? args[0] : "/etc/door.json";
		log.info("Loading config file {}", configFile);
		final DoorController dc = new DoorController(new File(configFile));
		dc.start();
	}

	private final DoorHardware dh;
	private final MemberDatabase db;
	private final AccessController ac;
	private final WebServer ws;
	private final EventLog al;
	final Consumer<String> listener;

	public DoorController(final File configFile) throws Exception {
		final JSONObject config = new JSONObject(new JSONTokener(new FileInputStream(configFile)));

		final FileEventLog fl = new FileEventLog(config.getString("accessLogs"));
		final GoogleSheetEventLog gl = new GoogleSheetEventLog(config.getJSONObject("google"));
		al = new EventLog.Splitter(fl, gl);

		dh = new DoorHardware(config.getString("port"));
		db = new MemberDatabase(//
				new WildApricot(config.getJSONObject("wildApricot").getString("apiKey")), //
				new File(config.getString("memberCache")));
		ac = new AccessController();
		ws = new WebServer(dh, db, fl, gl, al, ac, this);

		this.listener = cardNum -> {
			try {
				final Optional<Member> om = db.getMemberByAccessCard(cardNum);
				if (!om.isPresent()) {
					al.unknownCard(cardNum);
				} else {
					final Member m = om.get();
					final AccessResult res = ac.isAccessGranted(m);
					if (res.isGranted()) {
						al.accessGranted(m, res);
						dh.unlockBriefly();
					} else {
						al.accessDenied(m, res);
					}
				}
			} catch (final Exception e) {
				log.error("Error occured checking card", e);
			}
		};

		dh.addCardListener(listener);

		dh.addDoorStateChangeListener(ds -> {
			try {
				al.frontDoor(ds);
			} catch (final Exception e) {
				log.error("Error occured logging door state", e);
			}
		});

	}

	private void start() throws Exception {
		db.start();
		dh.start();
		try {
			ws.start();
		} catch (Exception e) {
			log.error("Unable to start server", e);
			ws.stop();
		}
		ws.join();
	}

}
