package com.booksocialnetwork.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendEmail(
            String to,
            String userName,
            EmailTemplateName emailTemplate,
            String confirmationUrl,
            String activationCode,
            String subject
    ) throws MessagingException {
        String templateName;
        if(emailTemplate==null){
            templateName="confirm-email";
        }else{
            templateName= emailTemplate.name();
        }
        MimeMessage mimeMessage=javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED,
                StandardCharsets.UTF_8.name()
        );
        Map<String,Object> properties=new HashMap<>();
        properties.put("username",userName);
        properties.put("confirmationUrl",confirmationUrl);
        properties.put("activationCode",activationCode);

        Context context=new Context();
        context.setVariables(properties);

        helper.setFrom("contact@arpitrathore.com");
        helper.setTo(to);
        helper.setSubject(subject);

        String template=templateEngine.process(templateName,context);

        helper.setText(template,true);
        javaMailSender.send(mimeMessage);
    }

}
