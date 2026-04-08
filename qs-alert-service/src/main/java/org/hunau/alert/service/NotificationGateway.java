package org.hunau.alert.service;

import org.hunau.alert.model.AlertRecord;
import org.hunau.common.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGateway.class);

    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;

    @Value("${app.notification.retry-once:true}")
    private boolean retryOnce;

    @Value("${app.notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.notification.sms.gateway-url:}")
    private String smsGatewayUrl;

    @Value("${app.notification.sms.to:}")
    private String smsTo;

    @Value("${app.notification.sms.api-key:}")
    private String smsApiKey;

    @Value("${app.notification.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.notification.mail.to:}")
    private String mailTo;

    @Value("${app.notification.mail.subject-prefix:[QS-ALERT]}")
    private String mailSubjectPrefix;

    public NotificationGateway(RestTemplate restTemplate, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.restTemplate = restTemplate;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public NotificationResult send(AlertRecord record) {
        RiskLevel level = record == null ? null : record.getRiskLevel();
        boolean needSms = level == RiskLevel.HIGH;
        boolean needMail = level == RiskLevel.MEDIUM;

        ChannelAttempt first = sendByRequiredChannels(record, needSms, needMail);
        if (first.success || !retryOnce || (!first.smsFailed && !first.mailFailed)) {
            return first.toResult(0);
        }

        ChannelAttempt second = retryFailedChannels(record, first, needSms, needMail);
        return second.toResult(1);
    }

    private ChannelAttempt sendByRequiredChannels(AlertRecord record, boolean needSms, boolean needMail) {
        boolean smsOk = !needSms || sendSms(record);
        boolean mailOk = !needMail || sendMail(record);
        return new ChannelAttempt(smsOk, mailOk, needSms && !smsOk, needMail && !mailOk);
    }

    private ChannelAttempt retryFailedChannels(AlertRecord record, ChannelAttempt first, boolean needSms, boolean needMail) {
        boolean smsOk = !needSms || !first.smsFailed || sendSms(record);
        boolean mailOk = !needMail || !first.mailFailed || sendMail(record);
        return new ChannelAttempt(smsOk, mailOk, needSms && !smsOk, needMail && !mailOk);
    }

    private boolean sendSms(AlertRecord record) {
        if (!smsEnabled) {
            log.warn("sms channel disabled, skip send");
            return false;
        }
        if (isBlank(smsGatewayUrl) || isBlank(smsTo)) {
            log.warn("sms config missing, gateway-url or to is blank");
            return false;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("to", smsTo);
            body.put("qsId", record == null ? null : record.getQsId());
            body.put("eventId", record == null ? null : record.getEventId());
            body.put("riskLevel", record == null || record.getRiskLevel() == null ? null : record.getRiskLevel().name());
            body.put("riskScore", record == null ? null : record.getRiskScore());
            body.put("message", buildSmsText(record));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!isBlank(smsApiKey)) {
                headers.set("X-API-Key", smsApiKey);
            }
            restTemplate.postForEntity(smsGatewayUrl, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception ex) {
            log.warn("send sms failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean sendMail(AlertRecord record) {
        if (!mailEnabled) {
            log.warn("mail channel disabled, skip send");
            return false;
        }
        if (mailSender == null) {
            log.warn("mail sender bean unavailable, check spring.mail.* config");
            return false;
        }
        if (isBlank(mailTo)) {
            log.warn("mail config missing, mail.to is blank");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(mailTo);
            message.setSubject(mailSubjectPrefix + " 风险预警 " + (record == null || record.getRiskLevel() == null ? "UNKNOWN" : record.getRiskLevel().name()));
            message.setText(buildMailText(record));
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("send mail failed: {}", ex.getMessage());
            return false;
        }
    }

    private String buildSmsText(AlertRecord record) {
        if (record == null) {
            return "QSGuard预警通知";
        }
        return "QSGuard预警: qsId=" + record.getQsId() + ", level=" + record.getRiskLevel() + ", score=" + String.format("%.2f", record.getRiskScore());
    }

    private String buildMailText(AlertRecord record) {
        if (record == null) {
            return "预警详情为空";
        }
        return "预警详情\n"
                + "eventId=" + record.getEventId() + "\n"
                + "qsId=" + record.getQsId() + "\n"
                + "companyId=" + record.getCompanyId() + "\n"
                + "riskLevel=" + record.getRiskLevel() + "\n"
                + "riskScore=" + String.format("%.4f", record.getRiskScore()) + "\n"
                + "detail=" + record.getDetail();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private record ChannelAttempt(boolean smsOk, boolean mailOk, boolean smsFailed, boolean mailFailed) {
        private NotificationResult toResult(int retryCount) {
            boolean success = smsOk && mailOk;
            String pushStatus = success ? (retryCount > 0 ? "retry" : "success") : "fail";
            String message = success
                    ? (retryCount > 0 ? "发送预警成功(重试1次后成功)" : "发送预警成功")
                    : "发送预警失败"
                    + "(sms=" + (smsOk ? "ok" : "fail")
                    + ", mail=" + (mailOk ? "ok" : "fail") + ")";
            String channelSummary = "SYSTEM,SMS,EMAIL";
            return new NotificationResult(success, retryCount, pushStatus, message, channelSummary);
        }
    }

    public record NotificationResult(boolean success,
                                     int retryCount,
                                     String pushStatus,
                                     String message,
                                     String channelSummary) {
    }
}

