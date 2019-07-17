package org.rutlandmint.mgmt.door;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.log.EventLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

@SpringBootApplication
public class DoorApplication {
	private static final Logger log = LoggerFactory.getLogger(DoorApplication.class);

	public static void main(final String[] args) {
		SpringApplication.run(DoorApplication.class, args);
	}

	private @Autowired MemberDatabase db;

	@Autowired
	private AccessController ac;

	private @Autowired DoorHardware dh;

	private @Autowired EventLog al;

	private @Autowired ObjectMapper mapper;

	Consumer<String> listener;

	@PostConstruct
	public void setup() throws Exception {
		mapper.registerModule(new JsonOrgModule());

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
}
