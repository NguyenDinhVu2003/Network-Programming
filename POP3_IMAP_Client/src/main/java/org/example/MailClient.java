package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Base64;

public class MailClient {
    // This socket facilitates communication between the client and the server.
    private Socket socket;
    // A buffered reader for reading data from the mail server.
    private BufferedReader reader;
    // A buffered writer for sending data to the mail server.
    private BufferedWriter writer;

    /**
     * Establishes a connection to the specified mail server.
     *
     * This constructor initializes a connection to the mail server using the given
     * hostname and port. Otherwise, it establishes a plain socket connection.
     * Additionally, it sets up input and output streams for communication with the server.
     *
     * @param server the hostname or IP address of the mail server (e.g., "pop3.example.com")
     * @param port the port number to connect to
     * @throws IOException if there is an error creating the socket or setting up streams
     *
     */
    public MailClient(String server, int port) throws IOException {
        socket = new Socket(server, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String serverGreeting = reader.readLine();
        System.out.println(serverGreeting);
    }

    /**
     * Sends a command to the mail server and returns the server's response.
     *
     * This method writes the specified command to the mail server, followed by a
     * carriage return and newline (`\r\n`) as required by the POP3/SMTP protocol.
     * It then flushes the output stream to ensure the command is sent immediately
     * and reads the server's response.
     *
     * @param command the command to send to the mail server
     * @return the first line of the server's response to the command
     * @throws IOException if there is an issue with writing to or reading from the server
     */
    public String sendCommand(String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
        return reader.readLine();
    }

    /**
     * Closes the connection to the mail server and releases resources.
     *
     * @throws IOException if there is an issue closing the streams or the socket
     */
    public void close() throws IOException {
        writer.close();
        reader.close();
        socket.close();
    }

    /**
     * Authenticates the user with the mail server using a username and password.
     *
     * This method sends the "USER" command followed by the username and the "PASS"
     * command followed by the password to the POP3 server. It checks the server's
     * response to determine if the login was successful.
     *
     * @param username the username or email address used for authentication
     * @param password the password associated with the username
     * @return true if the server confirms successful login (response starts with "+OK"),
     *         false otherwise
     * @throws IOException if there is an issue with the server communication
     */
    public boolean login(String username, String password) throws IOException {
        String response = sendCommand("USER " + username);
        // System.out.println(response);
        if (!response.startsWith("+OK")) {
            printError(response);
            return false;
        }

        response = sendCommand("PASS " + password);
        // System.out.println(response);
        boolean loggedIn = response.startsWith("+OK");
        if (!loggedIn) {
            printError(response);
        }
        return loggedIn;
    }

    /**
     * Retrieves a list of emails available on the mail server.
     *
     * This method sends the "LIST" command to the POP3 server, which returns a list of
     * all emails in the inbox. Each email entry includes its unique ID and size in bytes.
     * The method reads the response line by line until it encounters a single period (".")
     * on a line, indicating the end of the list. The result is returned as a string, with
     * each line corresponding to one email.
     *
     * @return a String containing the list of emails in the inbox, where each line includes
     *         an email ID and its size (e.g., "1 1200" for email ID 1 with 1200 bytes).
     *         If the inbox is empty, an empty string is returned.
     * @throws IOException if there is an issue with server communication
     */
    public String listEmails() throws IOException {
        StringBuilder emailTable = new StringBuilder();
        emailTable.append(String.format("%-10s %-10s\n", "Email ID", "Size (bytes)"));  // Table headers
    
        String response = sendCommand("LIST");
    
        if (response.startsWith("+OK")) {
            String line;
            while (!(line = reader.readLine()).equals(".")) {
                String[] parts = line.split("\\s+");  // Split on whitespace (spaces or tabs)
                if (parts.length == 2) {
                    String emailId = parts[0];
                    String size = parts[1];
    
                    // Format each email ID and size into a row in the table
                    emailTable.append(String.format("%-10s %-10s\n", emailId, size));
                }
            }
        }
    
        return emailTable.toString();
    }
    

    /**
     * Fetches the full content of a specific email from the server using its ID.
     *
     * This method sends the "RETR" command to the POP3 server to retrieve the email
     * with the specified ID. It reads the response line by line until it encounters
     * a single period (".") on a line, which indicates the end of the email content.
     *
     * @param emailId emailId the ID of the email to fetch (as listed by the "LIST" command)
     * @return a String containing the full raw content of the email, including headers and body
     * @throws IOException IOException if there is an issue with the server communication
     */
    public String fetchEmail(int emailId) throws IOException {
        StringBuilder emailContent = new StringBuilder();
        String response = sendCommand("RETR " + emailId);

        if (response.startsWith("+OK")) {
            String line;
            while (!(line = reader.readLine()).equals(".")) {
                emailContent.append(line).append("\n");
            }
        }
        return emailContent.toString();
    }

    /**
     * Sends an email to the specified recipient using the SMTP protocol.
     *
     * This method establishes a communication sequence with the SMTP server to send an email.
     * It uses the "MAIL FROM", "RCPT TO", and "DATA" commands to define the sender, recipient,
     * and email content (including the subject and body). The email content is terminated
     * by a single period (".") on a line, as per SMTP protocol requirements.
     *
     * @param from the sender's email address (e.g., "sender@example.com")
     * @param to the recipient's email address (e.g., "recipient@example.com")
     * @param subject the subject of the email
     * @param body the body content of the email
     * @return true if the email was successfully sent (server responds with code 250), false otherwise
     * @throws IOException if there is an issue with the server communication
     */
    public boolean sendEmail(String from, String to, String subject, String body) throws IOException {
        sendCommand("EHLO localhost");  // Greet the server
        sendCommand("MAIL FROM:<" + from + ">");  // Sender email address
        sendCommand("RCPT TO:<" + to + ">");  // Recipient email address
        sendCommand("DATA");  // Start composing the email
    
        // Write email headers
        writer.write("From: " + from + "\r\n");
        writer.write("To: " + to + "\r\n");
        writer.write("Subject: " + subject + "\r\n");
        writer.write("Date: " + java.time.Instant.now().toString() + "\r\n");  // Optional but useful
        writer.write("\r\n");  // Blank line separating headers and body
    
        // Write email body
        writer.write(body + "\r\n");
    
        // End of the email data
        writer.write(".\r\n");  // Indicate the end of the email message
    
        writer.flush();  // Ensure the data is sent
    
        // Read the server's response to ensure the message was accepted
        String response = reader.readLine();
        return response.startsWith("250");  // SMTP response code 250 means success
    }

    public void logout() throws IOException {
        String response = sendCommand("QUIT");
    }
    

 public boolean sendEmailWithAttachment(String from, String to, String subject, String body, String attachmentPath) throws IOException {
        // Tạo boundary cho MIME
        String boundary = "----=_Part_" + System.currentTimeMillis();

        // Gửi lệnh EHLO để bắt đầu phiên SMTP
        sendCommand("EHLO localhost");

        // Gửi lệnh MAIL FROM
        sendCommand("MAIL FROM:<" + from + ">");

        // Gửi lệnh RCPT TO
        sendCommand("RCPT TO:<" + to + ">");

        // Bắt đầu phần dữ liệu email
        sendCommand("DATA");

        // Tạo phần đầu email (bao gồm Subject và MIME header)
        writer.write("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n");
        writer.write("Subject: " + subject + "\r\n");
        writer.write("\r\n");

        // Phần body văn bản của email
        writer.write("--" + boundary + "\r\n");
        writer.write("Content-Type: text/plain; charset=\"UTF-8\"\r\n");
        writer.write("\r\n");
        writer.write(body + "\r\n");
        writer.write("\r\n");

        // Bắt đầu phần đính kèm
        File attachment = new File(attachmentPath);
        if (attachment.exists()) {
            // Đọc tệp đính kèm và mã hóa thành Base64
            byte[] fileBytes = Files.readAllBytes(attachment.toPath());
            String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

            // Phần MIME cho tệp đính kèm
            writer.write("--" + boundary + "\r\n");
            writer.write("Content-Type: application/octet-stream; name=\"" + attachment.getName() + "\"\r\n");
            writer.write("Content-Transfer-Encoding: base64\r\n");
            writer.write("Content-Disposition: attachment; filename=\"" + attachment.getName() + "\"\r\n");
            writer.write("\r\n");

            // Gửi tệp đính kèm đã mã hóa
            writer.write(encodedFile);
            writer.write("\r\n");
        }

        // Kết thúc phần đính kèm và email
        writer.write("--" + boundary + "--\r\n");  // Đánh dấu kết thúc các phần MIME
        writer.write(".\r\n");  // Kết thúc dữ liệu email

        // Đảm bảo dữ liệu đã được gửi đi
        writer.flush();

        // Đọc phản hồi từ máy chủ SMTP
        String response = reader.readLine();

        // Nếu phản hồi là 250, thì gửi email thành công
        return response.startsWith("250");
    }

    private static void printError(String error) {
        final String RED = "\033[0;31m";
        final String RESET = "\033[0m";  // Reset color to default
        System.out.println(RED + error + RESET);  // Print response in red color
    }
}
