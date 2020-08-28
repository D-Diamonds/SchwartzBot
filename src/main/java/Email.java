import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

/**
 * @author jlandsman3
 */

public class Email {

    private static final String address = "motivatingmessage@gmail.com";

    private final Set<InternetAddress> recipients;
    private final Gmail service;

    private final String ID;

    private final String emailMessage;

    public Email(Gmail service, Set<InternetAddress> recipients, String emailMessage) throws IOException {
        this.service = service;
        this.recipients = recipients;
        this.emailMessage = emailMessage;

        ID = service.users().getProfile("me").getUserId();
    }

    public MimeMessage createEmail() throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(address));
        for (InternetAddress emailAddress : recipients) {
            email.addRecipient(javax.mail.Message.RecipientType.BCC, emailAddress);
        }
        email.setSubject("Motivation for the week");
        email.setText(emailMessage);
        return email;
    }

    public Message createMessageWithEmail(MimeMessage emailContent) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public void sendMessage() throws MessagingException, IOException {
        Message message = service.users().messages().send(ID, createMessageWithEmail(createEmail())).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
    }
}
