package net.berndreiss.zentodo.server.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * TODO DESCRIBE
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * TODO DESCRIBE
     * @param to
     * @param token
     */
    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify Your Email";
        String verificationUrl = "http://localhost:8080/auth/verify?email= " + to + "&token=" + token;

        String content = "<p>Click the link below to verify your email:</p>" +
                "<p><a href=\"" + verificationUrl + "\">Verify Email</a></p>";

        sendEmail(to, subject, content);
    }

    //TODO DESCRIBE
    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
