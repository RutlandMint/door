package org.rutlandmint.mgmt.door.log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONObject;
import org.rutlandmint.mgmt.door.AccessController.AccessResult;
import org.rutlandmint.mgmt.door.DoorHardware.DoorState;
import org.rutlandmint.mgmt.door.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

@Component
public class PhotoUploader extends Thread implements EventLog {
	private static final Logger log = LoggerFactory.getLogger(PhotoUploader.class);

	private static final String APPLICATION_NAME = "MINT Door Controller";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private final Drive service;

	private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_kk:mm:ss");
	private final String parent;

	private final BlockingDeque<String> requests = new LinkedBlockingDeque<>(5);

	private final int photoCount;
	private final int photoDelay;
	private final String photoUrl;

	public PhotoUploader(//
			@Value("${photo.count}") final int photoCount, //
			@Value("${photo.delay}") final int photoDelay, //
			@Value("${photo.url}") final String photoUrl, //
			@Value("${google.photoParentID}") final String parentID, //
			@Value("${google.creds.type}") final String type, //
			@Value("${google.creds.project_id}") final String project_id, //
			@Value("${google.creds.private_key_id}") final String private_key_id, //
			@Value("${google.creds.private_key}") final String private_key, //
			@Value("${google.creds.client_email}") final String client_email, //
			@Value("${google.creds.client_id}") final String client_id, //
			@Value("${google.creds.auth_uri}") final String auth_uri, //
			@Value("${google.creds.token_uri}") final String token_uri, //
			@Value("${google.creds.auth_provider_x509_cert_url}") final String auth_provider_x509_cert_url, //
			@Value("${google.creds.client_x509_cert_url}") final String client_x509_cert_url //
	) throws IOException, GeneralSecurityException {
		this.setName("Google Photo Uploader");
		this.setDaemon(true);
		this.parent = parentID;

		this.photoCount = photoCount;
		this.photoDelay = photoDelay;
		this.photoUrl = photoUrl;

		// Stupid but easy
		final JSONObject creds = new JSONObject();
		creds.put("type", type);
		creds.put("project_id", project_id);
		creds.put("private_key_id", private_key_id);
		creds.put("private_key", private_key);
		creds.put("client_email", client_email);
		creds.put("client_id", client_id);
		creds.put("auth_uri", auth_uri);
		creds.put("token_uri", token_uri);
		creds.put("auth_provider_x509_cert_url", auth_provider_x509_cert_url);
		creds.put("client_x509_cert_url", client_x509_cert_url);

		final InputStream credStream = new ByteArrayInputStream(creds.toString().getBytes());
		final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		final GoogleCredential credential = GoogleCredential.fromStream(credStream)
				.createScoped(Collections.singleton(DriveScopes.DRIVE));

		service = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
				.build();

		this.start();
	}

	@Override
	public void unknownCard(String cardNum) throws IOException {
		requestImages(" Unknown: " + cardNum);
	}

	@Override
	public void accessGranted(Member member, AccessResult res) throws IOException {
		requestImages(" Granted: " + member.name);
	}

	@Override
	public void accessDenied(Member member, AccessResult res) throws IOException {
		requestImages(" Denied: " + member.name + ": " + res.message);
	}

	@Override
	public void frontDoor(DoorState ds) throws IOException {
		requestImages(" Door " + ds.toString());

	}

	@Override
	public void admin(String user, String action) throws IOException {
		// Blank
	}

	private void requestImages(String text) throws IOException {
		requests.add(text);
	}

	public void run() {
		while (true) {
			while (true) {
				try {
					final String text = requests.takeFirst();
					for (int i = 0; i < photoCount; i++) {
						uploadImageFile(text);
						Thread.sleep(photoDelay);
					}
				} catch (final InterruptedException e) {
					log.error("Waiting for photo request", e);
				} catch (final Exception e) {
					log.error("Writing photo", e);
				}
			}
		}
	}

	private String uploadImageFile(String text) throws IOException {
		final String name = "door-" + TIME_FORMAT.format(new Date()) + text + ".jpg";
		final File fileMetadata = new File();

		fileMetadata.setName(name);
		fileMetadata.setParents(Collections.singletonList(parent));

		try (InputStream image = getImage()) {
			final AbstractInputStreamContent uploadStreamContent = new InputStreamContent("image/jpeg", image);
			service.files().create(fileMetadata, uploadStreamContent)
					.setFields("id, webContentLink, webViewLink, parents").execute();
		}
		return name;
	}

	private Optional<URL> getLink(final String name) throws IOException {
		final FileList result = service.files().list()
				.setQ("parents in '" + parent + "' and name='" + name + "' and mimeType='image/jpeg'")
				.setSpaces("drive").setFields("nextPageToken, files(id, name, webViewLink, webContentLink)").execute();
		for (final File f : result.getFiles()) {
			return Optional.of(new URL(f.getWebViewLink()));
		}
		return Optional.empty();
	}

	private InputStream getImage() throws MalformedURLException, IOException {
		final String url = photoUrl;
		return new URL(url).openStream();
	}

}
