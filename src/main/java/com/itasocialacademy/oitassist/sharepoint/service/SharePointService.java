package com.itasocialacademy.oitassist.sharepoint.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class SharePointService {
    private final GraphAuthService authService;

    public SharePointService(GraphAuthService authService) {
        this.authService = authService;
    }

    public void uploadFile(MultipartFile file) {
        try {
            String accessToken = authService.getAccessToken();
            String driveId = System.getenv("GRAPH_DRIVE_ID");

            String urlStr = "https://graph.microsoft.com/v1.0/drives/"
                    + driveId +
                    "/root:/TestUploads/"
                    + file.getOriginalFilename() +
                    ":/content";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);

            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(file.getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                throw new RuntimeException("Upload failed: " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file", e);
        }
    }
}
