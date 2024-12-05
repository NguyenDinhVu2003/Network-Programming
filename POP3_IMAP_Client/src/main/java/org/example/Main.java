package org.example;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MailClient pop3 = null;
        MailClient smtp = null;
        boolean isLoggedIn = false;



        while (true) {
            System.out.print("\nEnter command: ");
            String command = scanner.nextLine().trim().toUpperCase();

            // Đăng nhập vào máy chủ (POP3 và SMTP)
            if (command.startsWith("USER")) {
                try {
                    String[] parts = command.split(" ");
                    String username = parts[1];

                    System.out.print("Enter password: ");
                    String password = scanner.nextLine().trim();

                    pop3 = new MailClient("localhost", 110);  // POP3 server on localhost, port 110
                    smtp = new MailClient("localhost", 25);  // SMTP server on localhost, port 25

                    isLoggedIn = pop3.login(username, password);

                    if (isLoggedIn) {
                        System.out.println("Logged in successfully.");
                    } else {
                        System.out.println("Login failed.");
                    }
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            // Liệt kê email trong hộp thư đến
            else if (command.equals("LIST")) {
                if (isLoggedIn) {
                    try {
                        String emailList = pop3.listEmails();
                        if (emailList.isEmpty()) {
                            System.out.println("No emails found.");
                        } else {
                            System.out.println("Email List:\n" + emailList);
                        }
                    } catch (IOException e) {
                        System.out.println("Error fetching email list: " + e.getMessage());
                    }
                } else {
                    System.out.println("You need to log in first.");
                }
            }

            // Lấy chi tiết email theo ID
            else if (command.startsWith("RETR")) {
                if (isLoggedIn) {
                    try {
                        String[] parts = command.split(" ");
                        int emailId = Integer.parseInt(parts[1]);

                        String emailContent = pop3.fetchEmail(emailId);
                        System.out.println("Email Content:\n" + emailContent);
                    } catch (IOException e) {
                        System.out.println("Error fetching email: " + e.getMessage());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid email ID.");
                    }
                } else {
                    System.out.println("You need to log in first.");
                }
            }

            // Gửi email
            else if (command.startsWith("SEND")) {
                if (isLoggedIn) {
                    try {
                        String[] parts = command.split(" ", 4);
                        if (parts.length < 4) {
                            System.out.println("Usage: SEND <to> <subject> <body>");
                            continue;
                        }

                        String to = parts[1];
                        String subject = parts[2];
                        String body = parts[3];

                        boolean isSent = smtp.sendEmail("nguyenvu12@network-programing.com", to, subject, body);

                        if (isSent) {
                            System.out.println("Email sent successfully.");
                        } else {
                            System.out.println("Failed to send email.");
                        }
                    } catch (IOException e) {
                        System.out.println("Error sending email: " + e.getMessage());
                    }
                } else {
                    System.out.println("You need to log in first.");
                }
            }
            else if (command.startsWith("SEND")) {
                if (isLoggedIn) {
                    try {
                        String[] parts = command.split(" ", 4);
                        if (parts.length < 4) {
                            System.out.println("Usage: SEND <to> <subject> <body>");
                            continue;
                        }

                        String to = parts[1];
                        String subject = parts[2];
                        String body = parts[3];

                        boolean isSent = smtp.sendEmail("nguyenvu12@network-programing.com", to, subject, body);

                        if (isSent) {
                            System.out.println("Email sent successfully.");
                        } else {
                            System.out.println("Failed to send email.");
                        }
                    } catch (IOException e) {
                        System.out.println("Error sending email: " + e.getMessage());
                    }
                } else {
                    System.out.println("You need to log in first.");
                }
            }

            // Gửi email có tệp đính kèm
            else if (command.startsWith("SENDATTACHMENT")) {
                if (isLoggedIn) {
                    try {
                        String[] parts = command.split(" ", 5);
                        if (parts.length < 5) {
                            System.out.println("Usage: SENDATTACHMENT <to> <subject> <body> <attachmentPath>");
                            continue;
                        }

                        String to = parts[1];
                        String subject = parts[2];
                        String body = parts[3];
                        String attachmentPath = parts[4];  // Đường dẫn tới tệp đính kèm

                        // Kiểm tra nếu tệp đính kèm tồn tại
                        File attachmentFile = new File(attachmentPath);
                        if (!attachmentFile.exists()) {
                            System.out.println("Attachment file does not exist. Please check the file path.");
                            continue;
                        }

                        boolean isSent = smtp.sendEmailWithAttachment("nguyenvu12@network-programing.com", to, subject, body, attachmentPath);

                        if (isSent) {
                            System.out.println("Email with attachment sent successfully.");
                        } else {
                            System.out.println("Failed to send email with attachment.");
                        }
                    } catch (IOException e) {
                        System.out.println("Error sending email: " + e.getMessage());
                    }
                } else {
                    System.out.println("You need to log in first.");
                }
            }

            // Thoát ứng dụng
            else if (command.equals("QUIT")) {
                System.out.println("Goodbye!");
                break;
            }

            // Lệnh không hợp lệ
            else {
                System.out.println("Invalid command. Available commands: USER, LIST, RETR <email_id>, SEND <to> <subject> <body>, QUIT");
            }
        }

        // Đóng kết nối khi thoát
        try {
            if (pop3 != null) pop3.close();
            if (smtp != null) smtp.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }

        scanner.close();
    }
}
