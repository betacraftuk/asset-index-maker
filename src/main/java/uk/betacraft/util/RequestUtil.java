package uk.betacraft.util;

import java.net.HttpURLConnection;
import java.net.URL;

public class RequestUtil {

    public static WebData pingGET(Request req) {
        try {
            URL url = new URL(req.REQUEST_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.setReadTimeout(15000);
            con.setConnectTimeout(15000);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            for (String key : req.PROPERTIES.keySet()) {
                con.addRequestProperty(key, req.PROPERTIES.get(key));
            }

            int http = con.getResponseCode();

            // Don't read data to save time, only read response code
            if (http >= 400 && http < 600) {
                con.getErrorStream().close();
            } else {
                con.getInputStream().close();
            }

            return new WebData(null, http);
        } catch (javax.net.ssl.SSLHandshakeException e) {
            e.printStackTrace();
            return new WebData(null, -2);
        } catch (Throwable t) {
            t.printStackTrace();
            return new WebData(null, -1);
        }
    }
}