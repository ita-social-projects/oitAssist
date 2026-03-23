package com.itasocialacademy.oitassist.sharepoint.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class GraphAuthService {
    public String getAccessToken() {
        try {
            String tenantId = System.getenv("GRAPH_TENANT_ID");
            String clientId = System.getenv("GRAPH_CLIENT_ID");
            String clientSecret = System.getenv("GRAPH_CLIENT_SECRET");

            URL url = new URL("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=https://graph.microsoft.com/.default" +
                    "&grant_type=client_credentials";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String response = reader.lines().reduce("", (a, b) -> a + b);
            return response.split("\"access_token\":\"")[1].split("\"")[0];

        } catch (Exception e) {
            throw new RuntimeException("Error getting token", e);
        }
    }

}
