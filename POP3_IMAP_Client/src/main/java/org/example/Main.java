package org.example;

import java.io.Console;
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


            String userMail = null;
            boolean loggedIn = false;
            while (!loggedIn)  {
                System.out.print("Enter your username: ");
                userMail = scanner.nextLine();

                // Get the system's console to read password without displaying it
                Console console = System.console();
                if (console == null) {
                    System.out.println("No console available");
                    return;
                }
                char[] passwordArray = console.readPassword("Enter your password: ");
                String password = new String(passwordArray);

                loggedIn = pop3MailClient.login(userMail, password);
            }

            printSuccess("Successfully logged in!");

            int command = -1;
            while (command != 4) {
                System.out.println("Commands:");
                System.out.println("1. View Inbox");
                System.out.println("2. Retrieve a specific mail by ID");
                System.out.println("3. Send a mail");
                System.out.println("4. Logout");

                command = scanner.nextInt();
                scanner.nextLine(); // consume new line

                switch (command) {
                    case 1:
                        String emails = pop3MailClient.listEmails();
                        if (emails.isEmpty()) {
                            System.out.println("No emails to display");
                        } else {
                            System.out.println(emails);
                        }     
                        break;
                    case 2:
                        System.out.print("Email id: ");
                        int emailId = scanner.nextInt();
                        String email = pop3MailClient.fetchEmail(emailId);
                        if (email.isEmpty()) {
                            printError("Email with id does not exist");
                        } else {
                            System.out.println(email);
                        }
                        break;
                    case 3:
                        System.out.print("Enter destination: ");
                        String destination = scanner.nextLine();
                        System.out.print("Enter subject: ");
                        String subject = scanner.nextLine();
                        System.out.println("Enter body: ");
                        String body = scanner.nextLine();
                        System.out.print("Do you want to add an attachement (y/n): ");
                        String att = scanner.nextLine();
                        boolean success = false;
                        if (att.equals("y")) {
                            System.out.print("Enter the path of the file: ");
                            String path = scanner.nextLine();
                            success = smtpMailClient.sendEmailWithAttachment(userMail, destination, subject, body, path);
                        } else {
                            success = smtpMailClient.sendEmail(userMail, destination, subject, body);
                        }
                        if (success) {
                            printSuccess("Email sent successfully");
                        } else {
                            printError("Something went wrong");
                        }
                        break;
                    case 4:
                        loggedIn = false;
                        pop3MailClient.logout();
                        break;
                    default:
                        printError("Command not available");
                        break;
                }
            }
            
            scanner.close();
            smtpMailClient.close();
            pop3MailClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printSuccess(String msg) {
        final String GREEN = "\033[0;32m";
        final String RESET = "\033[0m";
        System.out.println(GREEN + msg  + RESET);
    }

    private static void printError(String error) {
        final String RED = "\033[0;31m";
        final String RESET = "\033[0m";
        System.out.println(RED + error + RESET);
    }

}
