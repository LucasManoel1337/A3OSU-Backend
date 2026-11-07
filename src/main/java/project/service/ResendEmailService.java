package project.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import project.modal.entity.VerificationPurpose;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class ResendEmailService {

    @Value("${app.email.resend-api-key:}")
    private String resendApiKey;

    @Value("${app.email.from:no-reply@osutournament.com.br}")
    private String fromAddress;

    public void enviarCodigo(String recipient, String code, VerificationPurpose purpose) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("A variável RESEND_API_KEY precisa estar configurada para enviar emails.");
        }

        String title = purpose == VerificationPurpose.EMAIL_VERIFICATION
                ? "Confirmação de email"
                : "Redefinição de senha";
        String instructions = purpose == VerificationPurpose.EMAIL_VERIFICATION
                ? "Use o código abaixo para confirmar seu email e ativar sua conta:"
                : "Use o código abaixo para continuar a redefinição da sua senha:";
        String html = loadHtmlTemplate(code, title, instructions);

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(recipient)
                .subject(title + " - osu!tournament")
                .html(html)
                .build();

        try {
            new Resend(resendApiKey).emails().send(options);
        } catch (ResendException e) {
            throw new IllegalStateException("Não foi possível enviar o email de verificação.", e);
        }
    }

    private String loadHtmlTemplate(String code, String title, String instructions) {
        ClassPathResource resource = new ClassPathResource("templates/email-reset.html");
        try (InputStream input = resource.getInputStream()) {
            String html = new String(FileCopyUtils.copyToByteArray(input), StandardCharsets.UTF_8);
            return html.replace("{{title}}", title)
                    .replace("{{instructions}}", instructions)
                    .replace("{{resetCode}}", code);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar o template de email.", e);
        }
    }
}
