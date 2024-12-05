package org.example;

import java.io.*;
import java.net.*;

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
        StringBuilder emails = new StringBuilder();
        String response = sendCommand("LIST");

        if (response.startsWith("+OK")) {
            String line;
            while (!(line = reader.readLine()).equals(".")) {
                emails.append(line).append("\n");
            }
        }
        return emails.toString();
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
        sendCommand("EHLO localhost");
        sendCommand("MAIL FROM:<" + from + ">");
        sendCommand("RCPT TO:<" + to + ">");
        sendCommand("DATA");
        writer.write("Subject: " + subject + "\r\n");
        writer.write("\r\n");
        writer.write(body + "\r\n");
        writer.write(".\r\n");
        writer.flush();
        String response = reader.readLine();
        return response.startsWith("250");
    }

    private static void printError(String error) {
        final String RED = "\033[0;31m";
        final String RESET = "\033[0m";  // Reset color to default
        System.out.println(RED + error + RESET);  // Print response in red color

    }
}
