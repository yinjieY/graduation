package org.hunau.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.IdFormatUtil;
import org.hunau.common.util.Sm2Util;
import org.hunau.trace.client.TraceExternalClient;
import org.hunau.trace.entity.Company;
import org.hunau.trace.entity.ProductBatch;
import org.hunau.trace.entity.QsCode;
import org.hunau.trace.mapper.CompanyMapper;
import org.hunau.trace.mapper.ProductBatchMapper;
import org.hunau.trace.mapper.QsCodeMapper;
import org.hunau.trace.model.vo.TraceDetailVO;
import org.hunau.trace.security.AuthPrincipal;
import org.hunau.trace.service.QrCodeImageService;
import org.hunau.trace.service.QsCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class QsCodeServiceImpl implements QsCodeService {

    private static final Set<String> ALLOWED_STATUS = Set.of("active", "invalid", "frozen", "cancelled");
    private static final int QS_URL_MAX_LENGTH = 1024;
    private static final Logger log = LoggerFactory.getLogger(QsCodeServiceImpl.class);

    @Value("${app.crypto.sm2.private-key:TRACE_PRIVATE_KEY}")
    private String sm2PrivateKey;

    @Resource
    private QsCodeMapper qsCodeMapper;
    @Resource
    private ProductBatchMapper productBatchMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private TraceExternalClient traceExternalClient;
    @Resource
    private QrCodeImageService qrCodeImageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<?> generateQs(QsCode qsCode) {
        AssertUtil.notNull(qsCode, "二维码参数不能为空");
        AssertUtil.notEmpty(qsCode.getBatchId(), "批次ID不能为空");
        String effectiveCompanyId = resolveTargetCompanyId(qsCode.getCompanyId(), "生成二维码");
        qsCode.setCompanyId(effectiveCompanyId);

        ProductBatch batch = productBatchMapper.selectById(qsCode.getBatchId());
        if (batch == null) {
            throw new BusinessException("批次不存在");
        }
        if (!qsCode.getCompanyId().equals(batch.getCompanyId())) {
            throw new BusinessException("批次与企业不匹配");
        }

        Company company = companyMapper.selectById(qsCode.getCompanyId());
        if (company == null || !Integer.valueOf(1).equals(company.getStatus())) {
            throw new BusinessException("企业不可用，无法生成二维码");
        }

        if (!traceExternalClient.isCompanyApproved(qsCode.getCompanyId())) {
            throw new BusinessException("企业未通过认证审核，无法生成二维码");
        }

        String qsId = generateQsId();
        LocalDateTime now = LocalDateTime.now();
        String signPayload = qsId + "|" + qsCode.getBatchId() + "|" + qsCode.getCompanyId() + "|" + now;
        String signature = Sm2Util.sign(signPayload, sm2PrivateKey);
        String qsUrl = buildScanEntryUrl(qsId, qsCode.getBatchId(), qsCode.getCompanyId(), signPayload, signature);
        validateQsUrlLength(qsId, qsUrl);

        qsCode.setQsId(qsId);
        qsCode.setQsUrl(qsUrl);
        qsCode.setIssueTime(now);
        qsCode.setSm2Sign(signature);
        if (qsCode.getStatus() == null || qsCode.getStatus().isBlank()) {
            qsCode.setStatus("active");
        }
        if (qsCode.getMaxAllowedScans() == null) {
            qsCode.setMaxAllowedScans(5);
        }

        qsCodeMapper.insert(qsCode);
        String qrImageUrl = qrCodeImageService.generate(qsId, qsUrl);

        try {
            traceExternalClient.saveQrProof(
                    qsCode.getQsId(),
                    qsCode.getBatchId(),
                    qsCode.getCompanyId(),
                    qsCode.getSm2Sign(),
                    signPayload,
                    qsCode.getQsUrl(),
                    qsCode.getIssueTime() == null ? null : qsCode.getIssueTime().toString()
            );
        } catch (Exception ignored) {
            // Do not fail the issuance path if proof service is temporarily unavailable.
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("qsCode", qsCode);
        resp.put("qrImageUrl", qrImageUrl);
        resp.put("traceQueryUrl", "http://localhost:9090/trace/query/" + qsId);
        resp.put("scanEntryUrl", qsUrl);
        resp.put("scanReportUrl", "http://localhost:9090/scan/report");
        return R.ok(resp);
    }

    private String buildScanEntryUrl(String qsId, String batchId, String companyId, String signPayload, String signature) {
        String encodedPayload = URLEncoder.encode(signPayload, StandardCharsets.UTF_8);
        String encodedSignature = URLEncoder.encode(signature, StandardCharsets.UTF_8);
        return "http://localhost:9090/trace/scan/index.html"
                + "?qsId=" + qsId
                + "&batchId=" + batchId
                + "&companyId=" + companyId
                + "&payload=" + encodedPayload
                + "&signature=" + encodedSignature;
    }

    private void validateQsUrlLength(String qsId, String qsUrl) {
        int actualLength = qsUrl == null ? 0 : qsUrl.length();
        if (actualLength > QS_URL_MAX_LENGTH) {
            log.warn("Generated qs_url exceeds DB capacity, qsId={}, length={}, limit={}", qsId, actualLength, QS_URL_MAX_LENGTH);
            throw new BusinessException("生成二维码失败：访问链接长度超限，请联系管理员调整配置");
        }
    }

    private String resolveTargetCompanyId(String requestCompanyId, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("未登录或登录状态已失效");
        }
        String normalizedRequestCompanyId = requestCompanyId == null ? "" : requestCompanyId.trim();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            if (normalizedRequestCompanyId.isEmpty()) {
                throw new BusinessException("管理员" + action + "必须传 companyId");
            }
            return normalizedRequestCompanyId;
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof AuthPrincipal principal)) {
            throw new BusinessException("用户身份上下文异常，请重新登录");
        }

        if (!"COMPANY".equalsIgnoreCase(principal.getRole())) {
            throw new BusinessException("仅管理员或企业账号可" + action);
        }
        if (principal.getCompanyId() == null || principal.getCompanyId().isBlank()) {
            throw new BusinessException("企业账号缺少 companyId，请重新登录");
        }
        String tokenCompanyId = principal.getCompanyId().trim();
        if (!normalizedRequestCompanyId.isEmpty() && !tokenCompanyId.equals(normalizedRequestCompanyId)) {
            throw new BusinessException("企业账号只能生成本企业的二维码");
        }
        return tokenCompanyId;
    }

    @Override
    public R<?> getByQsId(String qsId) {
        AssertUtil.notEmpty(qsId, "qsId不能为空");
        QsCode qsCode = qsCodeMapper.selectById(qsId);
        if (qsCode == null) {
            throw new BusinessException("二维码不存在");
        }

        ProductBatch batch = productBatchMapper.selectById(qsCode.getBatchId());
        Company company = companyMapper.selectById(qsCode.getCompanyId());

        TraceDetailVO traceDetailVO = new TraceDetailVO();
        traceDetailVO.setQsCode(qsCode);
        traceDetailVO.setBatch(batch);
        traceDetailVO.setCompany(company);
        return R.ok(traceDetailVO);
    }

    @Override
    public R<?> listAll() {
        List<QsCode> qsCodes = qsCodeMapper.selectList(new LambdaQueryWrapper<QsCode>()
                .orderByDesc(QsCode::getCreatedAt));
        return R.ok(qsCodes);
    }

    @Override
    public R<?> changeStatus(String qsId, String status) {
        AssertUtil.notEmpty(qsId, "qsId不能为空");
        AssertUtil.notEmpty(status, "目标状态不能为空");
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BusinessException("不支持的二维码状态: " + status);
        }

        QsCode qsCode = qsCodeMapper.selectById(qsId);
        if (qsCode == null) {
            throw new BusinessException("二维码不存在");
        }

        LambdaUpdateWrapper<QsCode> updateWrapper = new LambdaUpdateWrapper<QsCode>()
                .eq(QsCode::getQsId, qsId)
                .set(QsCode::getStatus, status);
        if ("frozen".equals(status)) {
            updateWrapper.set(QsCode::getFreezeTime, LocalDateTime.now());
        } else {
            updateWrapper.set(QsCode::getFreezeTime, null);
        }
        qsCodeMapper.update(null, updateWrapper);

        if ("frozen".equals(status) || "cancelled".equals(status)) {
            try {
                traceExternalClient.saveFreezeProof(qsId, status, "status_changed_by_trace_service");
            } catch (Exception ignored) {
                // Keep status update available even if block service is down.
            }
        }
        return getByQsId(qsId);
    }

    private String generateQsId() {
        for (int i = 0; i < 8; i++) {
            String candidate = IdFormatUtil.newQsId();
            if (qsCodeMapper.selectById(candidate) == null) {
                return candidate;
            }
        }
        throw new BusinessException("二维码ID生成失败，请重试");
    }
}
