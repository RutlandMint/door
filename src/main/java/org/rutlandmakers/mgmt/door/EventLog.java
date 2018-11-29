package org.rutlandmakers.mgmt.door;

import java.io.IOException;

import org.rutlandmakers.mgmt.door.AccessController.AccessResult;
import org.rutlandmakers.mgmt.door.DoorHardware.DoorState;

public interface EventLog {
	public void unknownCard(String cardNum) throws IOException;

	public void accessGranted(Member member, AccessResult res) throws IOException;

	public void accessDenied(Member member, AccessResult res) throws IOException;

	public void frontDoor(DoorState ds) throws IOException;

	public void admin(final String user, final String action) throws IOException;

	public class Splitter implements EventLog {
		private final EventLog[] logs;

		public Splitter(final EventLog... logs) {
			this.logs = logs;
		}

		@Override
		public void unknownCard(String cardNum) throws IOException {
			for (EventLog log : logs)
				log.unknownCard(cardNum);
		}

		@Override
		public void accessGranted(Member member, AccessResult res) throws IOException {
			for (EventLog log : logs)
				log.accessGranted(member, res);
		}

		@Override
		public void accessDenied(Member member, AccessResult res) throws IOException {
			for (EventLog log : logs)
				log.accessDenied(member, res);
		}

		@Override
		public void frontDoor(DoorState ds) throws IOException {
			for (EventLog log : logs)
				log.frontDoor(ds);
		}

		@Override
		public void admin(String user, String action) throws IOException {
			for (EventLog log : logs)
				log.admin(user, action);
		}
	}
}
