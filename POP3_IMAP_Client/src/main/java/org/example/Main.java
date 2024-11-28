package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            // Create a new MailClient instance (connect to SMTP/POP3 server)
            MailClient mailClient = new MailClient("smtp.example.com", 25);  // Use SMTP server and port

            // Login to the mail server
            boolean isLoggedIn = mailClient.login("user@example.com", "AdminPassword123");
            if (isLoggedIn) {
                System.out.println("Logged in successfully.");

                // List available emails (use POP3)
                String emailList = mailClient.listEmails();
                System.out.println("Email List:\n" + emailList);

                // Fetch a specific email by its ID (e.g., email ID 1)
                String emailContent = mailClient.fetchEmail(1);
                System.out.println("Fetched Email Content:\n" + emailContent);

                // Send an email (use SMTP)
                boolean isSent = mailClient.sendEmail("user@example.com", "recipient@example.com", "Subject", "Hello, this is a test email.");
                if (isSent) {
                    System.out.println("Email sent successfully.");
                } else {
                    System.out.println("Failed to send email.");
                }

            } else {
                System.out.println("Login failed.");
            }

            // Close the connection
            mailClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
