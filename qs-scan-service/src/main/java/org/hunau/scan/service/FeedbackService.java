package org.hunau.scan.service;

import org.hunau.common.R;
import org.hunau.common.util.AssertUtil;
import org.hunau.scan.client.AlertFeignClient;
import org.hunau.scan.client.BlockFeignClient;
import org.hunau.scan.client.TraceFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FeedbackService {

    private static final DateTimeFormatter FEEDBACK_ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final JdbcTemplate jdbcTemplate;
    private final TraceFeignClient traceFeignClient;
    private final AlertFeignClient alertFeignClient;
    private final BlockFeignClient blockFeignClient;

    @Value("${app.feedback.image-dir:data/feedback}")
    private String imageDir;

    @Value("${app.feedback.rate.high:0.20}")
    private double highRate;

    @Value("${app.feedback.rate.medium:0.10}")
    private double mediumRate;

    @Value("${app.feedback.rate.low:0.03}")
    private double lowRate;

    public FeedbackService(JdbcTemplate jdbcTemplate,
                           TraceFeignClient traceFeignClient,
                           AlertFeignClient alertFeignClient,
                           BlockFeignClient blockFeignClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.traceFeignClient = traceFeignClient;
        this.alertFeignClient = alertFeignClient;
        this.blockFeignClient = blockFeignClient;
    }

    @PostConstruct
    public void ensureFeedbackSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS feedback_record ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈记录自增ID',"
                + "feedback_id VARCHAR(40) NOT NULL COMMENT '反馈唯一编号',"
                + "qs_id VARCHAR(32) NOT NULL COMMENT '二维码ID',"
                + "batch_id VARCHAR(32) NULL COMMENT '批次ID',"
                + "company_id VARCHAR(32) NULL COMMENT '企业ID',"
                + "device_fingerprint VARCHAR(64) NOT NULL COMMENT '设备指纹',"
                + "submitter_ip VARCHAR(64) NULL COMMENT '提交者IP地址',"
                + "feedback_type VARCHAR(20) NOT NULL COMMENT '反馈类型(CROSS_REGION跨区销售/COUNTERFEIT假冒伪劣/OTHER其他)',"
                + "description VARCHAR(200) NULL COMMENT '反馈详细描述',"
                + "region VARCHAR(100) NOT NULL COMMENT '反馈地区',"
                + "lat DOUBLE NULL COMMENT '纬度坐标',"
                + "lng DOUBLE NULL COMMENT '经度坐标',"
                + "image_file VARCHAR(128) NOT NULL COMMENT '水印后图片文件名',"
                + "qr_status VARCHAR(20) NULL COMMENT '提交时二维码状态',"
                + "complaint_rate DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '投诉率(反馈数/扫码数)',"
                + "risk_level VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT '风险等级(NONE/LOW/MEDIUM/HIGH)',"
                + "status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '处理状态(SUBMITTED已提交/ACCEPTED已受理/REJECTED已驳回/CLOSED已结案)',"
                + "handle_user VARCHAR(64) NULL COMMENT '处理人',"
                + "handle_note VARCHAR(255) NULL COMMENT '处理备注',"
                + "handle_time DATETIME NULL COMMENT '处理时间',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "UNIQUE KEY uk_feedback_id (feedback_id),"
                + "UNIQUE KEY uk_qs_device_feedback (qs_id, device_fingerprint),"
                + "KEY idx_feedback_qs_time (qs_id, created_at),"
                + "KEY idx_feedback_company (company_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费者质量反馈记录表'");

        addColumnIfMissing("submitter_ip", "ALTER TABLE feedback_record ADD COLUMN submitter_ip VARCHAR(64) NULL COMMENT '提交者IP地址' AFTER device_fingerprint");
        addColumnIfMissing("handle_user", "ALTER TABLE feedback_record ADD COLUMN handle_user VARCHAR(64) NULL COMMENT '处理人' AFTER status");
        addColumnIfMissing("handle_note", "ALTER TABLE feedback_record ADD COLUMN handle_note VARCHAR(255) NULL COMMENT '处理备注' AFTER handle_user");
        addColumnIfMissing("handle_time", "ALTER TABLE feedback_record ADD COLUMN handle_time DATETIME NULL COMMENT '处理时间' AFTER handle_note");
    }

    public R<Map<String, Object>> submit(String qsId,
                                         String feedbackType,
                                         String deviceFingerprint,
                                         String submitterIp,
                                         String region,
                                         String description,
                                         Double latitude,
                                         Double longitude,
                                         MultipartFile image) {
        AssertUtil.notEmpty(qsId, "qsId不能为空");
        AssertUtil.notEmpty(feedbackType, "反馈类型不能为空");
        AssertUtil.notEmpty(deviceFingerprint, "设备指纹不能为空");
        AssertUtil.notEmpty(region, "所在地区不能为空");
        if (image == null || image.isEmpty()) {
            return R.fail("反馈图片不能为空");
        }

        String normalizedType = feedbackType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("CROSS_REGION", "COUNTERFEIT", "OTHER").contains(normalizedType)) {
            return R.fail("反馈类型仅支持 CROSS_REGION/COUNTERFEIT/OTHER");
        }

        Integer existed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM feedback_record WHERE qs_id=? AND device_fingerprint=?",
                Integer.class,
                qsId.trim(),
                deviceFingerprint.trim()
        );
        if (existed > 0) {
            return R.fail("同一设备对该二维码仅允许提交一次反馈");
        }

        R<Map<String, Object>> traceResp = traceFeignClient.queryTrace(qsId.trim());
        if (traceResp == null || traceResp.getCode() != 200 || traceResp.getData() == null) {
            return R.fail("反馈失败：二维码溯源信息不可用");
        }

        String companyId = "";
        String batchId = "";
        String qrStatus = "unknown";
        Integer maxAllowedScans = null;
        Object qsCodeRaw = traceResp.getData().get("qsCode");
        if (qsCodeRaw instanceof Map<?, ?> qsCodeMap) {
            companyId = stringValue(qsCodeMap.get("companyId"));
            batchId = stringValue(qsCodeMap.get("batchId"));
            qrStatus = stringValue(qsCodeMap.get("status"));
            Object maxScansObj = qsCodeMap.get("maxAllowedScans");
            if (maxScansObj != null) {
                try {
                    maxAllowedScans = Integer.parseInt(String.valueOf(maxScansObj));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        if (maxAllowedScans == null) {
            Integer dbMaxScans = queryMaxAllowedScansFromDb(qsId.trim());
            if (dbMaxScans != null) {
                maxAllowedScans = dbMaxScans;
            } else {
                maxAllowedScans = 5;
            }
        }

        String feedbackId = generateFeedbackId();
        String imageFileName;
        try {
            imageFileName = saveWithWatermark(feedbackId, image);
        } catch (Exception ex) {
            return R.fail("反馈图片处理失败: " + ex.getMessage());
        }

        int scanCount = countScan(qsId.trim());
        int oldFeedbackCount = countFeedback(qsId.trim());
        int newFeedbackCount = oldFeedbackCount + 1;
        
        double complaintRate = 0.0;
        double thresholdRate = 0.4;
        int scanThreshold = (int) Math.ceil(maxAllowedScans * thresholdRate);
        
        if (scanCount >= scanThreshold) {
            complaintRate = newFeedbackCount / (double) Math.max(1, scanCount);
        }
        
        String riskLevel = decideRiskLevel(complaintRate);

        jdbcTemplate.update(
                "INSERT INTO feedback_record(feedback_id,qs_id,batch_id,company_id,device_fingerprint,submitter_ip,feedback_type,description,region,lat,lng,image_file,qr_status,complaint_rate,risk_level,status,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                feedbackId,
                qsId.trim(),
                batchId,
                companyId,
                deviceFingerprint.trim(),
                fitLength(submitterIp, 64),
                normalizedType,
                fitLength(description, 200),
                fitLength(region, 100),
                latitude,
                longitude,
                imageFileName,
                qrStatus,
                complaintRate,
                riskLevel,
                "SUBMITTED"
        );

        boolean proofSuccess = saveFeedbackProof(feedbackId, qsId.trim(), normalizedType, companyId);
        boolean alertTriggered = triggerFeedbackAlertIfNeeded(feedbackId, qsId.trim(), companyId, normalizedType, complaintRate, riskLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("feedbackId", feedbackId);
        result.put("qsId", qsId.trim());
        result.put("companyId", companyId);
        result.put("complaintRate", complaintRate);
        result.put("riskLevel", riskLevel);
        result.put("proofSuccess", proofSuccess);
        result.put("alertTriggered", alertTriggered);
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        result.put("message", "[" + currentTime + "] 反馈已存证，编号 " + feedbackId + "，可用于进度查询");
        result.put("statusQueryPath", "/scan/feedback/status/" + feedbackId);
        result.put("imagePath", "/scan/feedback/image/" + imageFileName);
        return R.ok(result);
    }

    public R<Map<String, Object>> status(String feedbackId) {
        AssertUtil.notEmpty(feedbackId, "feedbackId不能为空");
        Map<String, Object> row = jdbcTemplate.query(
                "SELECT feedback_id,qs_id,company_id,feedback_type,description,region,submitter_ip,qr_status,complaint_rate,risk_level,status,handle_user,handle_note,handle_time,created_at FROM feedback_record WHERE feedback_id=?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("feedbackId", rs.getString("feedback_id"));
                    item.put("qsId", rs.getString("qs_id"));
                    item.put("companyId", rs.getString("company_id"));
                    item.put("feedbackType", rs.getString("feedback_type"));
                    item.put("description", rs.getString("description"));
                    item.put("region", rs.getString("region"));
                    item.put("submitterIp", rs.getString("submitter_ip"));
                    item.put("complaintRate", rs.getDouble("complaint_rate"));
                    item.put("riskLevel", rs.getString("risk_level"));
                    item.put("feedbackStatus", rs.getString("status"));
                    item.put("handleUser", rs.getString("handle_user"));
                    item.put("handleNote", rs.getString("handle_note"));
                    item.put("handleTime", rs.getTimestamp("handle_time") == null ? null : rs.getTimestamp("handle_time").toLocalDateTime());
                    item.put("createdAt", rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
                    return item;
                },
                feedbackId.trim()
        );
        if (row == null) {
            return R.fail("反馈编号不存在");
        }

        String qsId = String.valueOf(row.get("qsId"));
        R<Map<String, Object>> traceResp = traceFeignClient.queryTrace(qsId);
        if (traceResp != null && traceResp.getCode() == 200 && traceResp.getData() != null) {
            Object qsCodeRaw = traceResp.getData().get("qsCode");
            if (qsCodeRaw instanceof Map<?, ?> qsMap) {
                row.put("qrCurrentStatus", stringValue(qsMap.get("status")));
            }
        }
        return R.ok(row);
    }

    public R<List<Map<String, Object>>> list(String role, String companyId) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        String normalizedCompany = companyId == null ? "" : companyId.trim();
        List<Map<String, Object>> rows;
        if (!normalizedCompany.isBlank()) {
            // 无论角色是什么，只要提供了 companyId，就根据 companyId 过滤
            rows = jdbcTemplate.query(
                    "SELECT feedback_id,qs_id,company_id,feedback_type,region,risk_level,status,submitter_ip,created_at FROM feedback_record WHERE company_id=? ORDER BY created_at DESC LIMIT 300",
                    (rs, rowNum) -> mapFeedback(rs),
                    normalizedCompany
            );
        } else if ("COMPANY".equals(normalizedRole)) {
            // 如果是 COMPANY 角色但没有提供 companyId，返回空列表
            rows = new ArrayList<>();
        } else {
            // 其他情况（ADMIN 或未登录）返回全量消息
            rows = jdbcTemplate.query(
                    "SELECT feedback_id,qs_id,company_id,feedback_type,region,risk_level,status,submitter_ip,created_at FROM feedback_record ORDER BY created_at DESC LIMIT 500",
                    (rs, rowNum) -> mapFeedback(rs)
            );
        }
        return R.ok(rows);
    }

    public R<Map<String, Object>> detail(String feedbackId, String role, String companyId) {
        AssertUtil.notEmpty(feedbackId, "feedbackId不能为空");
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        String normalizedCompany = companyId == null ? "" : companyId.trim();

        Map<String, Object> row;
        if ("COMPANY".equals(normalizedRole) && !normalizedCompany.isBlank()) {
            row = jdbcTemplate.query(
                    "SELECT feedback_id,qs_id,company_id,feedback_type,description,region,submitter_ip,image_file,complaint_rate,risk_level,status,handle_user,handle_note,handle_time,created_at FROM feedback_record WHERE feedback_id=? AND company_id=?",
                    rs -> rs.next() ? mapFeedbackDetail(rs) : null,
                    feedbackId.trim(), normalizedCompany
            );
        } else {
            row = jdbcTemplate.query(
                    "SELECT feedback_id,qs_id,company_id,feedback_type,description,region,submitter_ip,image_file,complaint_rate,risk_level,status,handle_user,handle_note,handle_time,created_at FROM feedback_record WHERE feedback_id=?",
                    rs -> rs.next() ? mapFeedbackDetail(rs) : null,
                    feedbackId.trim()
            );
        }
        if (row == null) {
            return R.fail("反馈编号不存在或无权限查看");
        }
        return R.ok(row);
    }

    public R<Map<String, Object>> updateStatus(String feedbackId, String status, String handleNote, String role, String operator, String companyId) {
        AssertUtil.notEmpty(feedbackId, "feedbackId不能为空");
        AssertUtil.notEmpty(status, "status不能为空");
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!"ADMIN".equals(normalizedRole)) {
            return R.fail("仅管理员可更新反馈状态");
        }

        String nextStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACCEPTED", "REJECTED", "CLOSED").contains(nextStatus)) {
            return R.fail("status仅支持 ACCEPTED/REJECTED/CLOSED");
        }

        Map<String, Object> current = jdbcTemplate.query(
                "SELECT qs_id,company_id,submitter_ip FROM feedback_record WHERE feedback_id=?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("qsId", rs.getString("qs_id"));
                    item.put("companyId", rs.getString("company_id"));
                    item.put("submitterIp", rs.getString("submitter_ip"));
                    return item;
                },
                feedbackId.trim()
        );
        if (current == null) {
            return R.fail("反馈编号不存在");
        }

        String note = fitLength(handleNote, 180);
        if ("CLOSED".equals(nextStatus)) {
            String qsId = String.valueOf(current.get("qsId"));
            String submitterIp = safeText((String) current.get("submitterIp"));
            String qrStatus = "unknown";
            try {
                R<Map<String, Object>> traceResp = traceFeignClient.queryTrace(qsId);
                if (traceResp != null && traceResp.getCode() == 200 && traceResp.getData() != null) {
                    Object qsCodeRaw = traceResp.getData().get("qsCode");
                    if (qsCodeRaw instanceof Map<?, ?> qsMap) {
                        qrStatus = safeText(stringValue(qsMap.get("status")));
                    }
                }
            } catch (Exception ignored) {
            }
            String closeSummary = "结案说明: submitterIp=" + submitterIp + ", qrStatus=" + qrStatus;
            note = note == null || note.isBlank() ? closeSummary : fitLength(note + " | " + closeSummary, 255);
        }

        int affected = jdbcTemplate.update(
                "UPDATE feedback_record SET status=?,handle_user=?,handle_note=?,handle_time=NOW() WHERE feedback_id=?",
                nextStatus,
                safeText(operator),
                note,
                feedbackId.trim()
        );
        if (affected <= 0) {
            return R.fail("状态更新失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("feedbackId", feedbackId.trim());
        result.put("status", nextStatus);
        result.put("handleUser", safeText(operator));
        result.put("handleNote", note);
        result.put("companyId", safeText(companyId));
        return R.ok(result);
    }

    public Resource loadImage(String fileName) {
        Path path = Paths.get(imageDir, fileName).toAbsolutePath().normalize();
        return new FileSystemResource(path.toFile());
    }

    private String saveWithWatermark(String feedbackId, MultipartFile image) throws IOException {
        Files.createDirectories(Paths.get(imageDir));

        BufferedImage source = ImageIO.read(image.getInputStream());
        if (source == null) {
            throw new IllegalArgumentException("仅支持图片格式上传");
        }

        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, source.getWidth() / 24)));
        g.setColor(new Color(255, 0, 0, 90));
        g.rotate(Math.toRadians(-20), source.getWidth() / 2.0, source.getHeight() / 2.0);
        g.drawString("仅供监管取证", Math.max(20, source.getWidth() / 5), source.getHeight() / 2);
        g.dispose();

        String fileName = feedbackId + ".png";
        File out = Paths.get(imageDir, fileName).toFile();
        ImageIO.write(target, "png", out);
        return fileName;
    }

    private boolean saveFeedbackProof(String feedbackId, String qsId, String feedbackType, String companyId) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventId", feedbackId);
            body.put("action", "FEEDBACK_SUBMITTED");
            body.put("qsId", qsId);
            body.put("companyId", companyId);
            body.put("feedbackType", feedbackType);
            body.put("createdAt", LocalDateTime.now().toString());
            R<?> resp = blockFeignClient.saveEventProof(body);
            return resp != null && resp.getCode() == 200;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean triggerFeedbackAlertIfNeeded(String feedbackId,
                                                 String qsId,
                                                 String companyId,
                                                 String feedbackType,
                                                 double complaintRate,
                                                 String riskLevel) {
        if (!"HIGH".equals(riskLevel) && !"MEDIUM".equals(riskLevel)) {
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("feedbackId", feedbackId);
            body.put("qsId", qsId);
            body.put("companyId", companyId == null ? "UNKNOWN" : companyId);
            body.put("feedbackType", feedbackType);
            body.put("complaintRate", complaintRate);
            body.put("riskLevel", riskLevel);
            R<?> resp = alertFeignClient.createFeedbackMessage(body);
            return resp != null && resp.getCode() == 200;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int countScan(String qsId) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM yx_scan_anomaly.scan_log WHERE qs_id=?",
                Integer.class,
                qsId
        );
        return cnt == null ? 0 : cnt;
    }

    private int countFeedback(String qsId) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM feedback_record WHERE qs_id=?",
                Integer.class,
                qsId
        );
        return cnt == null ? 0 : cnt;
    }

    private String decideRiskLevel(double complaintRate) {
        if (complaintRate >= highRate) {
            return "HIGH";
        }
        if (complaintRate >= mediumRate) {
            return "MEDIUM";
        }
        if (complaintRate >= lowRate) {
            return "LOW";
        }
        return "NONE";
    }

    private String generateFeedbackId() {
        return "FB" + LocalDateTime.now().format(FEEDBACK_ID_FMT)
                + ThreadLocalRandom.current().nextInt(10, 100);
    }

    private String fitLength(String text, int max) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? "" : text;
    }

    private Map<String, Object> mapFeedback(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("feedbackId", rs.getString("feedback_id"));
        row.put("qsId", rs.getString("qs_id"));
        row.put("companyId", rs.getString("company_id"));
        row.put("feedbackType", rs.getString("feedback_type"));
        row.put("region", rs.getString("region"));
        row.put("riskLevel", rs.getString("risk_level"));
        row.put("status", rs.getString("status"));
        row.put("submitterIpMasked", maskIp(rs.getString("submitter_ip")));
        row.put("createdAt", rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        return row;
    }

    private Map<String, Object> mapFeedbackDetail(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("feedbackId", rs.getString("feedback_id"));
        row.put("qsId", rs.getString("qs_id"));
        row.put("companyId", rs.getString("company_id"));
        row.put("feedbackType", rs.getString("feedback_type"));
        row.put("description", rs.getString("description"));
        row.put("region", rs.getString("region"));
        row.put("submitterIp", rs.getString("submitter_ip"));
        row.put("submitterIpMasked", maskIp(rs.getString("submitter_ip")));
        row.put("imagePath", "/scan/feedback/image/" + rs.getString("image_file"));
        row.put("complaintRate", rs.getDouble("complaint_rate"));
        row.put("riskLevel", rs.getString("risk_level"));
        row.put("feedbackStatus", rs.getString("status"));
        row.put("handleUser", rs.getString("handle_user"));
        row.put("handleNote", rs.getString("handle_note"));
        row.put("handleTime", rs.getTimestamp("handle_time") == null ? null : rs.getTimestamp("handle_time").toLocalDateTime());
        row.put("createdAt", rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        return row;
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='feedback_record' AND column_name=?",
                Integer.class,
                columnName
        );
        if (cnt != null && cnt == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "-";
        }
        String text = ip.trim();
        int idx = text.lastIndexOf('.');
        if (idx > 0) {
            return text.substring(0, idx) + ".*";
        }
        return text.length() <= 4 ? "****" : text.substring(0, 4) + "****";
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
    
    private Integer queryMaxAllowedScansFromDb(String qsId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT max_allowed_scans FROM yx_trace_core.qs_code WHERE qs_id = ?",
                    Integer.class,
                    qsId
            );
        } catch (Exception ignored) {
            return null;
        }
    }
}

