package org.rutlandmint.mgmt.door.log;

import java.io.IOException;

import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.DoorHardware.DoorState;
import org.rutlandmint.mgmt.door.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class MultiEventLog implements EventLog {

	@Autowired
	private final EventLog[] logs;

	public MultiEventLog(final EventLog... logs) {
		this.logs = logs;
	}

	@Override
	public void unknownCard(final String cardNum) throws IOException {
		for (final EventLog log : logs) {
			log.unknownCard(cardNum);
		}
	}

	@Override
	public void accessGranted(final Member member, final AccessResult res) throws IOException {
		for (final EventLog log : logs) {
			log.accessGranted(member, res);
		}
	}

	@Override
	public void accessDenied(final Member member, final AccessResult res) throws IOException {
		for (final EventLog log : logs) {
			log.accessDenied(member, res);
		}
	}

	@Override
	public void frontDoor(final DoorState ds) throws IOException {
		for (final EventLog log : logs) {
			log.frontDoor(ds);
		}
	}

	@Override
	public void admin(final String user, final String action) throws IOException {
		for (final EventLog log : logs) {
			log.admin(user, action);
		}
	}
}