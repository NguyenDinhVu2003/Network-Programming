package org.example;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            // Create a new MailClient instance (connect to SMTP/POP3 server)
            MailClient pop3MailClient = new MailClient("localhost", 110);  // Use SMTP server and port
            MailClient smtpMailClient = new MailClient("localhost", 25);  // Use SMTP server and port

            System.out.println("Successfully connected to server.");

            int command = -1;
            while (true) {

                boolean loggedIn = false;
                while (!loggedIn)  {
                    System.out.print("Enter your username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter your password: ");
                    String password = scanner.nextLine();

                    loggedIn = pop3MailClient.login(username, password);
                }

                printSuccess("Successfully logged in!");

                while (command != 4) {
                    System.out.println("Commands:");
                    System.out.println("1. View Inbox");
                    System.out.println("2. Retrieve a specific mail by ID");
                    System.out.println("3. Send a mail");
                    System.out.println("4. Logout");

                    command = scanner.nextInt();

                    switch (command) {
                        case 1:
                            smtpMailClient.listEmails();
                            break;
                        case 2:
                            System.out.println("Email id: ");
                            int emailId = scanner.nextInt();
                            smtpMailClient.fetchEmail(emailId);
                            break;
                        case 3:
                            System.out.println("TODO");
                            break;
                        case 4:
                            System.out.println("Logged out successfully!");
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printSuccess(String msg) {
        final String GREEN = "\033[0;32m";
        final String RESET = "\033[0m";
        System.out.println(GREEN + msg  + RESET);
    }
}
