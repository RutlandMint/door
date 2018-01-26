package org.rutlandmakers.mgmt.door;

import java.util.Calendar;

import org.json.JSONObject;

public class AccessController {

	private boolean memberAccessEnabled = true;

	public static abstract class AccessResult {
		private AccessResult(final String message) {
			this.message = message;
		}

		abstract boolean isGranted();

		public final String message;
	}

	public static final class AccessGranted extends AccessResult {
		private AccessGranted(final String message) {
			super(message);
		}

		@Override
		public boolean isGranted() {
			return true;
		};
	}

	public static final class AccessDenied extends AccessResult {
		private AccessDenied(final String message) {
			super(message);
		}

		@Override
		public boolean isGranted() {
			return false;
		};
	}

	public final AccessDenied NO_WAIVER = new AccessDenied("No Waiver on File");
	public final AccessDenied NO_AGREEMENT = new AccessDenied("No Member Agreement on File");
	public final AccessDenied NO_NIGHT = new AccessDenied("No Nighttime Access");
	public final AccessDenied INACTIVE = new AccessDenied("Member Inactive");
	public final AccessGranted GRANTED = new AccessGranted("");

	public AccessResult isAccessGranted(final Member m) {
		if (!m.signedWaiver) {
			return NO_WAIVER;
		}
		if (!m.signedAgreement) {
			return NO_AGREEMENT;
		}
		if (!"Active".equals(m.status)) {
			return INACTIVE;
		}
		if (isNightMode() && !m.afterHoursAccess) {
			return NO_NIGHT;
		}
		return GRANTED;
	}

	public boolean isNightMode() {
		final int hod = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		final boolean isNight = hod < 8 || hod >= 21;
		return isNight;
	}

	public boolean isMemberAccessEnabled() {
		return memberAccessEnabled;
	}

	public void setMemberAccessEnabled(final boolean memberAccessEnabled) {
		this.memberAccessEnabled = memberAccessEnabled;
	}

	public JSONObject getStatus() {
		return new JSONObject()//
				.put("memberAccessEnabled", memberAccessEnabled)//
				.put("nightMode", isNightMode());
	}
}
