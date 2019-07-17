package org.rutlandmint.mgmt.door;

import java.io.IOException;

import org.json.JSONObject;
import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.log.EventLog;
import org.rutlandmint.mgmt.door.log.GoogleSheetEventLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ApiController {

	private @Autowired MemberDatabase db;

	private @Autowired AccessController ac;

	private @Autowired DoorHardware dh;

	private @Autowired GoogleSheetEventLog gl;

	private @Autowired DoorApplication dc;

	private @Autowired EventLog el;

	@Secured("ROLE_STAFF")
	@RequestMapping("status.json")
	@ResponseBody
	JSONObject getStatus() {
		return new JSONObject().put("database", db.getStatus()).put("door", dh.getStatus())
				.put("accessControl", ac.getStatus()).put("google", gl.getStatus());
	}

	@Secured("ROLE_MEMBER")
	@RequestMapping(method = RequestMethod.POST, path = "memberOpen.do")
	@ResponseStatus(HttpStatus.OK)
	void memberOpen(Authentication auth) throws IOException {
		db.getMemberByEmail(auth.getName()).map(ac::isAccessGranted).filter(AccessResult::isGranted).ifPresent(res -> {
			try {
				el.admin("Member Opened Door");
				dh.unlockBriefly();
			} catch (IOException e) {
				throw new Error(e);
			}
		});
	}

	@Secured("ROLE_STAFF")
	@RequestMapping(method = RequestMethod.POST, path = "openBriefly.do")
	@ResponseStatus(HttpStatus.OK)
	void openBriefly() throws IOException {
		el.admin("Opened Door Remotely");
		dh.unlockBriefly();
	}

	@Secured("ROLE_STAFF")
	@RequestMapping(method = RequestMethod.POST, path = "disable.do")
	@ResponseStatus(HttpStatus.OK)
	void disableMemberAccess() throws IOException {
		if (ac.isMemberAccessEnabled()) {
			el.admin("Disabled Member Access");
		}
		ac.setMemberAccessEnabled(false);
	}

	@Secured("ROLE_STAFF")
	@RequestMapping(method = RequestMethod.POST, path = "enable.do")
	@ResponseStatus(HttpStatus.OK)
	void enableMemberAccess() throws IOException {
		if (!ac.isMemberAccessEnabled()) {
			el.admin("Enabled Member Access");
		}
		ac.setMemberAccessEnabled(true);
	}

	@Secured("ROLE_STAFF")
	@RequestMapping(method = RequestMethod.POST, path = "test.do")
	@ResponseStatus(HttpStatus.OK)
	void testCard(@RequestParam final String testCard) throws IOException {
		el.admin("Performing test of " + testCard.trim() + "...");
		dc.listener.accept(testCard.trim());
	}
}
