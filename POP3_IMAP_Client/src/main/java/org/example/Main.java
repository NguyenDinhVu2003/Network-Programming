package org.example;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            // Create a new MailClient instance (connect to SMTP/POP3 server)
            MailClient pop3MailClient = new MailClient("localhost", 110, "+OK");  // Use POP3 server and port
            MailClient smtpMailClient = new MailClient("localhost", 25, "220");  // Use SMTP server and port

            printSuccess("Successfully connected to server.");

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
            while (command != 5) {
                System.out.println("--- COMMANDS ---");
                System.out.println("1. View Inbox");
                System.out.println("2. Retrieve a specific mail by ID");
                System.out.println("3. Send a mail");
                System.out.println("4. Dowload attachement from a mail");
                System.out.println("5. Logout");

                command = scanner.nextInt();
                scanner.nextLine(); // consume new line

                switch (command) {
                    case 1:
                        printLine(25);
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
                        scanner.nextLine(); // consume newline
                        printLine(25);
                        Mail email = pop3MailClient.fetchEmail(emailId);
                        if (email == null) {
                            printError("Email with id does not exist");
                        } else {
                            System.out.println(email.toString());
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
                        ArrayList<String> attachmentPaths = new ArrayList<>();
                        while (att.equals("y")) {
                            System.out.print("Enter the path of the file: ");
                            String path = scanner.nextLine();
                            attachmentPaths.add(path);
                            System.out.print("Do you want to add another attachement (y/n): ");
                            att = scanner.nextLine();
                        }
                        boolean success = smtpMailClient.sendEmailWithAttachment(userMail, destination, subject, body, attachmentPaths);
                        if (success) {
                            printSuccess("Email sent successfully");
                        } else {
                            printError("Something went wrong");
                        }
                        break;
                    case 4:
                        System.out.print("Enter email id to download attachments: ");
                        int attachmentEmailId = scanner.nextInt();
                        scanner.nextLine(); // consume newline

                        Mail emailWithAttachments = pop3MailClient.fetchEmail(attachmentEmailId);
                        if (emailWithAttachments == null) {
                            printError("Email with id does not exist");
                        } else {
                            Map<String, String> attachments = emailWithAttachments.getAttachments();
                            if (attachments.isEmpty()) {
                                printError("No attachments found in this email.");
                            } else {
                                // Use recipient's email (your email) as folder name
                                String emailFolder = emailWithAttachments.getTo(); // Use recipient's email
                                if (emailFolder == null || emailFolder.isEmpty()) {
                                    printError("Recipient email is missing. Cannot create folder.");
                                    break;
                                }

                                // Replace invalid characters in folder name
                                emailFolder = emailFolder.replaceAll("[^a-zA-Z0-9@.-]", "_");
                                File folder = new File("downloads/" + emailFolder);
                                folder.mkdirs(); // Create the folder if it doesn't exist

                                for (Map.Entry<String, String> entry : attachments.entrySet()) {
                                    String fileName = entry.getKey(); // Use the original filename
                                    if (fileName == null || fileName.isEmpty() || fileName.equalsIgnoreCase("Attachment")) {
                                        continue; // Skip invalid attachment names like "Attachment"
                                    }
                                    String fileContent = entry.getValue();

                                    File outputFile = new File(folder, fileName); // Save to folder
                                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                        fos.write(fileContent.getBytes());
                                        printSuccess("Attachment downloaded: " + outputFile.getPath());
                                    } catch (IOException e) {
                                        printError("Failed to save attachment: " + fileName);
                                    }
                                }
                            }
                        }
                        break;
                    case 5:
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

    private static void printLine(int count) {
        for (int i = 0; i < count; i++) {
            System.out.print("-");
        }
        System.out.println();
    }
}
