package readbiomed.annotators.discourse.sdt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SDTClient {

	private String prefix;

	public SDTClient(String prefix) {
		this.prefix = prefix;
	}

	public String [] predict(String string) throws IOException {

		URL url = new URL(prefix + "/predict?string=" + URLEncoder.encode(string, StandardCharsets.UTF_8.toString()));

		URLConnection con = url.openConnection();

		try (BufferedReader b = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			return b.readLine().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(" ", "").replaceAll("'", "").split(",");
		}
	}
}