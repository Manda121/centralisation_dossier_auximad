import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class MailConfigProbe {
    @Configuration(proxyBeanMethods = false)
    @Import(MailSenderAutoConfiguration.class)
    static class MailOnly {}

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MailOnly.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setLogStartupInfo(false);
        try (var context = app.run("--debug=false", "--trace=false", "--spring.main.banner-mode=off", "--logging.level.root=OFF",
                "--logging.level.org.springframework=OFF", "--logging.level.com.auximad=OFF")) {
            MailProperties properties = context.getBean(MailProperties.class);
            String password = properties.getPassword();
            boolean valid = "smtp.gmail.com".equals(properties.getHost()) && properties.getPort() == 587
                    && "notiavinamandaniaina@gmail.com".equals(properties.getUsername())
                    && properties.getUsername().equals(context.getEnvironment().getProperty("app.mail.from"))
                    && password != null && password.length() == 16 && !password.contains(" ");
            if (!valid) { System.out.println("LOCAL_SMTP_CONFIGURATION_INVALID"); System.exit(1); }
            System.out.println("LOCAL_SMTP_CONFIGURATION_OK");
            if (java.util.Arrays.asList(args).contains("--authenticate")) {
                try {
                    // Vérifie connexion et authentification, puis ferme le transport.
                    // Aucun message n'est créé ni envoyé.
                    context.getBean(JavaMailSenderImpl.class).testConnection();
                    System.out.println("SMTP_AUTHENTICATION_OK_NO_EMAIL_SENT");
                } catch (Exception error) {
                    Throwable cause = error;
                    while (cause.getCause() != null) cause = cause.getCause();
                    String message = cause.getMessage() == null ? "" : cause.getMessage();
                    if (message.contains("535") || message.contains("534")) {
                        System.out.println("SMTP_AUTHENTICATION_REFUSED");
                    } else {
                        System.out.println("SMTP_CONNECTION_FAILED_" + cause.getClass().getSimpleName());
                    }
                    System.exit(2);
                }
            }
        } catch (Throwable error) {
            System.out.println("SMTP_CONFIGURATION_CHECK_FAILED_" + error.getClass().getSimpleName());
            System.exit(1);
        }
    }
}
