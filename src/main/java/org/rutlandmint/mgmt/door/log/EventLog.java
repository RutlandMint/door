package org.rutlandmint.mgmt.door.log;

import java.io.IOException;

import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.DoorHardware.DoorState;
import org.rutlandmint.mgmt.door.Member;

public interface EventLog {
	public void unknownCard(String cardNum) throws IOException;

	public void accessGranted(Member member, AccessResult res) throws IOException;

	public void accessDenied(Member member, AccessResult res) throws IOException;

	public void frontDoor(DoorState ds) throws IOException;

	public void admin(final String user, final String action) throws IOException;
}
