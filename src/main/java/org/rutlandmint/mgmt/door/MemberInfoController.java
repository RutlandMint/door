package org.rutlandmint.mgmt.door;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

@Controller
public class MemberInfoController {

	private @Autowired MemberDatabase db;

	private @Autowired AccessController ac;

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

	@Secured("ROLE_STAFF")
	@RequestMapping("members.json")
	@ResponseBody
	Stream<MemberEntry> getMembers() {
		return db.getMembers().stream().map(MemberEntry::new);
	}

	@Secured("ROLE_MEMBER")
	@RequestMapping("self.json")
	@ResponseBody
	Optional<MemberEntry> getSelf(Authentication auth) {
		return db.getMemberByEmail(auth.getName()).map(MemberEntry::new);
	}
	
	@Secured("ROLE_STAFF")
	@RequestMapping(method = RequestMethod.POST, path = "reloadMembers.do")
	@ResponseStatus(HttpStatus.OK)
	void reloadMembers() throws IOException {
		db.doLoad();
	}
}
