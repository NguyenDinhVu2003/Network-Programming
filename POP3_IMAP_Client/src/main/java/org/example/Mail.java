package org.example;

import java.util.HashMap;
import java.util.Map;

import java.util.*;
import java.util.regex.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Mail {
    private String returnPath;
    private String from;
    private String to;
    private String subject;
    private String date;
    private String textBody;
    private Map<String, String> attachments;

    // Constructor to parse the raw email content
    public Mail(String rawEmail) {
        this.attachments = new HashMap<>();
        parseEmail(rawEmail);
    }

    // Parse the email content into headers, body, and attachments
    private void parseEmail(String rawEmail) {
        String headersSection = rawEmail.split("\n\n")[0]; // The header part comes before an empty line
        String bodySection = rawEmail.substring(headersSection.length()).trim();

        // Regular expressions to extract headers
        this.returnPath = extractHeader(headersSection, "Return-Path");
        this.from = extractHeader(headersSection, "From");
        this.to = extractHeader(headersSection, "To");
        this.subject = extractHeader(headersSection, "Subject");
        this.date = extractHeader(headersSection, "Date");

        // Now, extract multipart sections (body and attachments)
        String boundary = extractBoundary(headersSection);
        if (boundary != null) {
            parseMultipartBody(bodySection, boundary);
        }
    }

    // Extract header value by header name
    private String extractHeader(String headers, String headerName) {
        Pattern pattern = Pattern.compile(headerName + ": (.*)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(headers);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    // Extract boundary from the Content-Type header
    private String extractBoundary(String headers) {
        Pattern pattern = Pattern.compile("boundary=\"([^\"]+)\"", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(headers);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // Parse multipart body and attachments
    private void parseMultipartBody(String bodySection, String boundary) {
        String[] parts = bodySection.split(boundary);
        System.out.println(parts.length);
        for (String part : parts) {
            if (part.contains("Content-Type: text/plain")) {
                // Extract the text body of the email
                this.textBody = extractTextBody(part);
            } else if (part.contains("Content-Disposition: attachment")) {
                // Extract attachments
                String attachment = extractAttachment(part);
                if (!attachment.isEmpty()) {
                    this.attachments.put("Attachment", attachment);
                }
            }
        }
    }

    // Extract text body from a multipart part
    private String extractTextBody(String part) {
        Pattern pattern = Pattern.compile("Content-Type: text/plain.*?\\r?\\n\\r?\\n(.*?)\\r?\\n--", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(part);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    // Extract base64 encoded attachment
    private String extractAttachment(String part) {
        Pattern pattern = Pattern.compile("Content-Transfer-Encoding: base64.*?\\r?\\n\\r?\\n(.*?)\\r?\\n--", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(part);
        if (matcher.find()) {
            String encodedAttachment = matcher.group(1).trim();
            return decodeBase64(encodedAttachment);
        }
        return "";
    }

    // Decode base64 content to a human-readable string (assuming it’s text-based for simplicity)
    private String decodeBase64(String encodedString) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    // Getters for different parts of the email
    public String getReturnPath() {
        return returnPath;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public String getTextBody() {
        return textBody;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    // Override toString for human-readable email representation
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(from).append("\n");
        sb.append("To: ").append(to).append("\n");
        sb.append("Subject: ").append(subject).append("\n");
        
        // Format the date into a human-readable format
        ZonedDateTime dateTime = ZonedDateTime.parse(date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss");
        String formattedDate = dateTime.format(formatter);

        sb.append("Date: ").append(formattedDate).append("\n");
        sb.append("\n" + textBody +"\n");

        if (!attachments.isEmpty()) {
            sb.append("\nAttachments:\n");
            for (String filename : attachments.keySet()) {
                sb.append(" - ").append(filename).append("\n").append(attachments.get(filename)).append("\n");
            }
        }
        return sb.toString();
    }

    // Main method for testing
    public static void main(String[] args) {
        String rawEmail = "Return-Path: daniel@example.com\n" +
                          "Received: from localhost (DESKTOP-BAQ9VDA [127.0.0.1])\n" +
                          "        by DESKTOP-BAQ9VDA with ESMTP\n" +
                          "        ; Wed, 4 Dec 2024 21:19:38 -0800\n" +
                          "Message-ID: <B9D5A1F8-DF8A-4E43-AC0D-791CCD6F1CD1@DESKTOP-BAQ9VDA>\n" +
                          "Content-Type: multipart/mixed; boundary=\"----=_Part_1733375978497\"\n" +
                          "From: daniel@example.com\n" +
                          "To: daniel@example.com\n" +
                          "Subject: text\n" +
                          "Date: 2024-12-05T05:19:38.521522100Z\n" +
                          "\n" +
                          "------=_Part_1733375978497\n" +
                          "Content-Type: text/plain; charset=\"UTF-8\"\n\n" +
                          "text\n" +
                          "------=_Part_1733375978497\n" +
                          "Content-Type: application/octet-stream; name=\"example.txt.txt\"\n" +
                          "Content-Transfer-Encoding: base64\n" +
                          "Content-Disposition: attachment; filename=\"example.txt.txt\"\n\n" +
                          "SGVsbG8gV29ybGQh\n" +
                          "------=_Part_1733375978497--";

        Mail email = new Mail(rawEmail);
        System.out.println(email);
    }
}

