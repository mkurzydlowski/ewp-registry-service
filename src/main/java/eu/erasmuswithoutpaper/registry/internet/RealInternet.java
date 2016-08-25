package eu.erasmuswithoutpaper.registry.internet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.notifier.NotifierFlag;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The "real" implementation of the {@link Internet} interface. It will be used in production and
 * development environment.
 *
 * <p>
 * Note, that in "development" profile is set, then no emails will be actually sent (to prevent
 * accidentally spamming the users).
 * </p>
 */
@Service
@Profile({ "production", "development" })
public class RealInternet implements Internet {

  private static final Logger logger = LoggerFactory.getLogger(RealInternet.class);

  private final JavaMailSender mailSender;
  private final NotifierFlag emailSendingStatus;
  private final String replyToAddress;
  private final String replyToName;
  private final TaskExecutor taskExecutor;
  private final Environment env;

  private volatile Date lastEmailSendingError;

  /**
   * @param mailSender {@link JavaMailSender} to use for sending email messages.
   * @param adminEmails A list of email addresses to be notified on component errors.
   * @param replyToName A name to be used in the From field of the messages sent.
   * @param replyToAddress An email to be used in the From field of the messages sent.
   * @param taskExecutor {@link TaskExecutor} to be used for queuing email messages to be sent
   *        asynchronously.
   * @param env needed to check which Spring profiles were activated (and prevent spamming users in
   *        production environments).
   */
  @Autowired
  public RealInternet(JavaMailSender mailSender,
      @Value("${app.admin-emails}") List<String> adminEmails,
      @Value("${app.instance-name}") String replyToName,
      @Value("${app.reply-to-address}") String replyToAddress, TaskExecutor taskExecutor,
      Environment env) {

    this.mailSender = mailSender;
    this.replyToName = replyToName;
    this.replyToAddress = replyToAddress;
    this.taskExecutor = taskExecutor;
    this.env = env;

    this.emailSendingStatus = new NotifierFlag(adminEmails) {
      @Override
      public String getName() {
        return "Status of SMTP service.";
      }
    };
    this.emailSendingStatus.setStatus(Severity.OK);
  }

  @Override
  public byte[] getUrl(String urlString) throws IOException {
    URL url = new URL(urlString);
    InputStream is = null;
    try {
      URLConnection conn = url.openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(10000);
      conn.setAllowUserInteraction(false);
      is = conn.getInputStream();
      return IOUtils.toByteArray(is);
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  @Override
  public void queueEmail(List<String> recipients, String subject, String contents) {
    /*
     * Note, that in production environment this taskExecutor will contain multiple threads, while
     * in test environment it will execute tasks synchronously.
     */
    this.taskExecutor.execute(new Runnable() {
      @Override
      public void run() {
        RealInternet.this.sendEmail(recipients, subject, contents);
      }
    });
  }

  private void sendEmail(List<String> recipients, String subject, String contents) {
    try {

      MimeMessage message = this.mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message);

      try {
        helper.setFrom(this.replyToAddress, this.replyToName);
        helper.setTo(recipients.toArray(new String[recipients.size()]));
        helper.setSubject(subject);
        helper.setText(contents);
      } catch (MessagingException | UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

      if (this.env.acceptsProfiles("production")) {
        this.mailSender.send(message);
      } else {
        logger.warn("Non-production profile detected. As a safety measure, this prevents "
            + "the message from being actually sent (but we'll pretend it has been).");
        return;
      }

      /*
       * When had the last failure occurred? We don't want the severity to be prematurely set to OK,
       * because admins won't notice the issue. Each error will cause the flag to have the ERROR
       * severity for at least one hour.
       */

      if (this.lastEmailSendingError != null) {
        Calendar anHourAgo = Calendar.getInstance();
        anHourAgo.add(Calendar.HOUR, -1);

        if (this.lastEmailSendingError.after(anHourAgo.getTime())) {
          return;
        }
      }

      this.emailSendingStatus.setStatus(Severity.OK);

    } catch (RuntimeException e) {
      logger.error("RuntimeException while sending e-mail message", e);
      this.emailSendingStatus.setStatus(Severity.ERROR);
      this.lastEmailSendingError = new Date();
    }
  }

  /**
   * @param notifier needed to register local error flags.
   */
  @Autowired
  private void setNotifierServer(NotifierService notifier) {
    notifier.addWatchedFlag(this.emailSendingStatus);
  }
}
