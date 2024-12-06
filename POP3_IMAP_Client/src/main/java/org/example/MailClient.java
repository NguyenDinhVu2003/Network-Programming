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
     */
    public MailClient(String server, int port, String response) throws IOException {
        socket = new Socket(server, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String serverGreeting = reader.readLine();
        if (serverGreeting.startsWith(response)) {
            System.out.println(serverGreeting);
        } else {
            printError(serverGreeting);
        }
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
     * @return true if the server confirms successful login (response starts with "+OK"),
     *         false otherwise
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
 
    public boolean sendEmailWithAttachment(String from, String to, String subject, String body, String attachmentPath) throws IOException {
        // Tạo boundary cho MIME
        String boundary = "----=_Part_" + System.currentTimeMillis();

        sendCommand("EHLO localhost");
    
        // Now, read all the lines in the EHLO response
        String line;
        while ((line = reader.readLine()) != null && !line.startsWith("250 ")) {
            //System.out.println(line); // Optionally print the response
        }

        // Gửi lệnh MAIL FROM
        String response = sendCommand("MAIL FROM:<" + from + ">");
        //System.out.println(response);

        response = sendCommand("RCPT TO:<" + to + ">");
        //System.out.println(response);

        response = sendCommand("DATA");
        //System.out.println(response); // Expecting "354 OK, send"

        // Write email headers
        writer.write("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n");
        writer.write("From: " + from + "\r\n");
        writer.write("To: " + to + "\r\n");
        writer.write("Subject: " + subject + "\r\n");
        writer.write("Date: " + java.time.Instant.now().toString() + "\r\n");
        writer.write("\r\n"); // Empty line separating headers from body

         // Write the body of the email
        writer.write("--" + boundary + "\r\n");
        writer.write("Content-Type: text/plain; charset=\"UTF-8\"\r\n");
        writer.write("\r\n");
        writer.write(body + "\r\n");
        writer.write("\r\n");

        // If there's an attachment, handle it
        if (attachmentPath != null && !attachmentPath.isEmpty()) {
            File attachment = new File(attachmentPath);
            if (attachment.exists()) {
                byte[] fileBytes = Files.readAllBytes(attachment.toPath());
                String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

                // Add attachment part
                writer.write("--" + boundary + "\r\n");
                writer.write("Content-Type: application/octet-stream; name=\"" + attachment.getName() + "\"\r\n");
                writer.write("Content-Transfer-Encoding: base64\r\n");
                writer.write("Content-Disposition: attachment; filename=\"" + attachment.getName() + "\"\r\n");
                writer.write("\r\n");
                
                // Write Base64 encoded file content
                int chunkSize = 76; // SMTP requires chunks of 76 characters
                
                for (int i = 0; i < encodedFile.length(); i += chunkSize) {
                    writer.write(encodedFile, i, Math.min(i + chunkSize, encodedFile.length()));
                    writer.write("\r\n");
                }
                writer.write("\r\n");
            } else {
                throw new IOException("Attachment file not found: " + attachmentPath);
            }
        }
        
        // End the email content
        writer.write("--" + boundary + "--\r\n");
        writer.write(".\r\n");
        
        // Send the email content
        writer.flush();
        
        // Read the server response to ensure it was accepted
        response = reader.readLine();
        
        return response.startsWith("250");  // Return true if email was accepted successfully
    }

    public String logout() throws IOException {
        String response = sendCommand("QUIT");
        return response;
    }

    private static void printError(String error) {
        final String RED = "\033[0;31m";
        final String RESET = "\033[0m";
        System.out.println(RED + error + RESET);
    }
}
