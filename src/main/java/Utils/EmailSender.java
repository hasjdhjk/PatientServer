package Utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailSender {

    private static Session getSession() {
        String host = System.getenv("SMTP_HOST");
        String port = System.getenv("SMTP_PORT");
        String user = System.getenv("SMTP_USER");
        String pass = System.getenv("SMTP_PASS");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    public static void sendEmail(String to, String subject, String body) throws MessagingException {
        Session session = getSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(System.getenv("SMTP_FROM")));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }
}
