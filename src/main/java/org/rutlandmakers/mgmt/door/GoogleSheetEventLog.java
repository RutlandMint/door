package org.rutlandmakers.mgmt.door;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.rutlandmakers.mgmt.door.AccessController.AccessResult;
import org.rutlandmakers.mgmt.door.DoorHardware.DoorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleSheetEventLog extends Thread implements EventLog {
	private static final Logger log = LoggerFactory.getLogger(GoogleSheetEventLog.class);
	private static final String APPLICATION_NAME = "MINT Door Controller";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("d MMM yyyy HH:mm:ss z", Locale.US);
	static {
		DATE_FMT.setTimeZone(TimeZone.getTimeZone("EST"));
	}

	private static final class Update {
		final String range;
		final List<Object> row;

		Update(final String range, final Object[] values) {
			this.range = range;
			row = new ArrayList<>(values.length + 1);
			row.add(DATE_FMT.format(new Date()));
			row.addAll(Arrays.asList(values));
		}
	}

	private final Sheets service;
	private final String sheetID;
	private final BlockingDeque<Update> updates = new LinkedBlockingDeque<>(100);

	public GoogleSheetEventLog(final JSONObject config) throws IOException, GeneralSecurityException {
		this.setName("Google Sheets Update Thread");
		this.setDaemon(true);
		this.sheetID = config.getString("sheetID");
		final InputStream credStream = new ByteArrayInputStream(config.getJSONObject("creds").toString().getBytes());
		final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		final GoogleCredential credential = GoogleCredential.fromStream(credStream)
				.createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
		service = new Sheets.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
				.build();
		start();
	}
	
	public JSONObject getStatus(){
		return new JSONObject().put("queueSize", updates.size());
	}

	@Override
	public void unknownCard(final String cardNum) throws IOException {
		offer("Unknown!A2", cardNum);
	}

	@Override
	public void accessGranted(final Member member, final AccessResult res) throws IOException {
		offer("Granted!A2", member.name, res.message);
	}

	@Override
	public void accessDenied(final Member member, final AccessResult res) throws IOException {
		offer("Denied!A2", member.name, res.message);
	}

	@Override
	public void frontDoor(final DoorState ds) throws IOException {
		offer("Door!A2", ds.toString());
	}

	@Override
	public void admin(final String user, final String action) throws IOException {
		offer("Admin!A2", user!=null?user:"[unknown]", action);
	}

	@Override
	public void run() {
		while (true) {
			try {
				final Update u = updates.takeFirst();
				write(u.range, u.row);
			} catch (final InterruptedException e) {
				log.error("Waiting for update", e);
			} catch (final IOException e) {
				log.error("Writing update", e);
			}

		}
	}

	private void offer(final String range, final Object... values) throws IOException {
		updates.add(new Update(range, values));
	}

	private void write(final String range, final List<Object> row) throws IOException {
		final List<List<Object>> rows = Arrays.asList(row);
		final ValueRange body = new ValueRange().setValues(rows);
		final AppendValuesResponse result = service.spreadsheets().values().append(sheetID, range, body)
				.setValueInputOption("RAW").execute();
	}

	public static void main(final String... args) throws IOException, GeneralSecurityException {
		final String configFile = args.length == 1 ? args[0] : "/etc/door.json";
		final JSONObject config = new JSONObject(new JSONTokener(new FileInputStream(configFile)));
		final GoogleSheetEventLog el = new GoogleSheetEventLog(config.getJSONObject("google"));
		el.offer("Sheet1!A2", "one", "thread", 4);
	}
}
