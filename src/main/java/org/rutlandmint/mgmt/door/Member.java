package org.rutlandmint.mgmt.door;

import java.io.Serializable;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

public class Member implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum Override {
		NONE, OPEN_24, NEVER
	}

	public final long id;
	public final String name;
	public final String email;
	public final String keyCardNumber;
	public final String level;
	public final String status;
	public final boolean afterHoursAccess;
	public final boolean signedWaiver;
	public final boolean signedAgreement;
	public final Override override;

	public String toString() {
		return "Member [email=" + email + "]";
	}

	private static Optional<JSONObject> findField(final String name, final JSONObject member) {
		final JSONArray fieldValues = member.getJSONArray("FieldValues");
		for (int j = 0; j < fieldValues.length(); j++) {
			final JSONObject field = fieldValues.getJSONObject(j);
			if (field.getString("FieldName").equals(name)) {
				return Optional.of(field);
			}
		}
		return Optional.empty();
	}

	public Member(final JSONObject member) {
		name = member.getString("DisplayName");
		email = member.getString("Email");
		id = member.getLong("Id");
		level = member.getJSONObject("MembershipLevel").getString("Name");
		keyCardNumber = findField("Key Card Number", member).map(f -> f.optString("Value")).orElse(null);
		afterHoursAccess = findField("After Hours Access", member).map(f -> f.getJSONArray("Value").length() > 0)
				.orElse(false);
		signedWaiver = findField("Signed Waiver On File", member).map(f -> f.getBoolean("Value")).orElse(false);
		signedAgreement = findField("Signed Membership Agreement On File", member).map(f -> f.getBoolean("Value"))
				.orElse(false);
		status = findField("Membership status", member).map(f -> f.getJSONObject("Value").getString("Value"))
				.orElse("");
		override = findField("Door Override", member)//
				.filter(f -> f.has("Value"))//
				.filter(f -> !f.isNull("Value"))//
				.map(f -> f.getJSONObject("Value").getString("Label"))//
				.map(l -> {
					switch (l) {
					case "Open 24 Hours":
						return Override.OPEN_24;
					case "Never Open":
						return Override.NEVER;
					default:
						return Override.NONE;
					}
				}).orElse(Override.NONE);
	}
}