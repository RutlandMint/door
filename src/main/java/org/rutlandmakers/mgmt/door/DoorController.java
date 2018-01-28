package org.rutlandmakers.mgmt.door;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;

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
	private final AccessLog al;
	private final AccessController ac;
	private final WebServer ws;

	public DoorController(final File configFile) throws Exception {
		final JSONObject config = new JSONObject(new JSONTokener(new FileInputStream(configFile)));

		al = new AccessLog(config.getString("accessLogs"));
		dh = new DoorHardware(config.getString("port"));
		db = new MemberDatabase(//
				new WildApricot(config.getJSONObject("wildApricot").getString("apiKey")), //
				new File(config.getString("memberCache")));
		ac = new AccessController();
		ws = new WebServer(dh, db, al, ac);

		dh.addCardListener(cardNum -> {
			try {
				final Optional<Member> om = db.getMemberByAccessCard(cardNum);
				if (!om.isPresent()) {
					al.log("Unknown Card [" + cardNum + "]", "Denied");
				} else {
					final Member m = om.get();
					final AccessResult res = ac.isAccessGranted(m);
					if (res.isGranted()) {
						al.log(m.name, "Access Granted: " + res.message);
						dh.unlockBriefly();
					} else {
						al.log(m.name, "Access Denied: " + res.message);
					}
				}
			} catch (final Exception e) {
				log.error("Error occured checking card", e);
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
