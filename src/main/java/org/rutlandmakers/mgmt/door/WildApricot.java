package org.rutlandmakers.mgmt.door;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WildApricot {
	private static final Logger log = LoggerFactory.getLogger(WildApricot.class);

	private final String apiKey;

	public WildApricot(final String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBearerToken() throws IOException {
		final HttpHost target = new HttpHost("oauth.wildapricot.org", 443, "https");
		final CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),
				new UsernamePasswordCredentials("APIKEY", apiKey));
		try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
				.build()) {
			final HttpPost getToken = new HttpPost("https://oauth.wildapricot.org/auth/token");
			final List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			params.add(new BasicNameValuePair("scope", "contacts_view"));
			getToken.setEntity(new UrlEncodedFormEntity(params));
			try (CloseableHttpResponse resp = httpclient.execute(getToken)) {
				final JSONObject tokenInfo = new JSONObject(new JSONTokener(resp.getEntity().getContent()));
				log.trace("Got {}", tokenInfo.toString(4));
				return tokenInfo.getString("access_token");
			}
		}
	}

	public Set<Member> loadMembers() throws IOException {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			final String query = "'Key Card Number','After Hours Access','Signed Waiver On File','Signed Membership Agreement On File','Membership Status'";
			final HttpGet searchMembers = new HttpGet(
					"https://api.wildapricot.org/v2/Accounts/241012/Contacts?$async=false&$select="
							+ URLEncoder.encode(query, "UTF-8"));
			searchMembers.addHeader("Authorization", "Bearer " + getBearerToken());
			try (CloseableHttpResponse resp = httpclient.execute(searchMembers)) {
				final JSONObject memberQueryResult = new JSONObject(new JSONTokener(resp.getEntity().getContent()));
				log.trace("Got {}", memberQueryResult.toString(4));
				final Set<Member> members = new HashSet<>();
				final JSONArray contacts = memberQueryResult.getJSONArray("Contacts");
				for (int i = 0; i < contacts.length(); i++) {
					final JSONObject j = contacts.getJSONObject(i);
					if (j.has("MembershipLevel")) {
						final Member member = new Member(j);
						members.add(member);
					}
				}
				return members;
			}
		}
	}
}
