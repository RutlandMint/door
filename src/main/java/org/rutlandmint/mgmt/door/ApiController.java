package org.rutlandmint.mgmt.door;

import java.io.IOException;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.log.EventLog;
import org.rutlandmint.mgmt.door.log.FileEventLog;
import org.rutlandmint.mgmt.door.log.GoogleSheetEventLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

@Controller
public class ApiController {

	private @Autowired MemberDatabase db;

	private @Autowired AccessController ac;

	private @Autowired DoorHardware dh;

	private @Autowired GoogleSheetEventLog gl;

	private @Autowired FileEventLog fl;

	private @Autowired DoorApplication dc;

	private @Autowired EventLog el;

	class MemberEntry {
		@JsonUnwrapped
		Member member;

		public MemberEntry(final Member member) {
			super();
			this.member = member;
		}

		public boolean getAccessGranted() {
			final AccessResult res = ac.isAccessGranted(member);
			return res == AccessController.GRANTED || res == AccessController.STAFF;
		}

		public String getDenyMessage() {
			final AccessResult res = ac.isAccessGranted(member);
			return (res == AccessController.GRANTED || res == AccessController.STAFF) ? null : res.message;
		}
	}

	@RequestMapping("members.json")
	@ResponseBody
	Stream<MemberEntry> getMembers() {
		return db.getMembers().stream().map(MemberEntry::new);
	}

	@RequestMapping("status.json")
	@ResponseBody
	JSONObject getStatus() {
		return new JSONObject().put("database", db.getStatus()).put("door", dh.getStatus())
				.put("accessControl", ac.getStatus()).put("google", gl.getStatus());
	}

	@RequestMapping("accessLog.json")
	@ResponseBody
	JSONObject getAccessLog(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		if (request.getDateHeader("If-Modified-Since") >= fl.getLastModified()) {
			response.setStatus(304);
			return null;
		} else {
			return fl.getLog();
		}
	}

	@RequestMapping("openBriefly.do")
	@ResponseStatus(HttpStatus.OK)
	void openBriefly(@RequestHeader("X-Email") final String user) throws IOException {
		el.admin(user, "Opened Door Remotely");
		dh.unlockBriefly();
	}

	@RequestMapping("disable.do")
	@ResponseStatus(HttpStatus.OK)
	void disableMemberAccess(@RequestHeader("X-Email") final String user) throws IOException {
		if (ac.isMemberAccessEnabled()) {
			el.admin(user, "Disabled Member Access");
		}
		ac.setMemberAccessEnabled(false);
	}

	@RequestMapping("enable.do")
	@ResponseStatus(HttpStatus.OK)
	void enableMemberAccess(@RequestHeader("X-Email") final String user) throws IOException {
		if (!ac.isMemberAccessEnabled()) {
			el.admin(user, "Enabled Member Access");
		}
		ac.setMemberAccessEnabled(true);
	}

	@RequestMapping("test.do")
	@ResponseStatus(HttpStatus.OK)
	void testCard(@RequestHeader("X-Email") final String user, @RequestParam final String testCard) throws IOException {
		el.admin(user, "Performing test of " + testCard.trim() + "...");
		dc.listener.accept(testCard.trim());
	}
}
