package org.hunau.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.Sm2Util;
import org.hunau.trace.client.TraceExternalClient;
import org.hunau.trace.entity.Company;
import org.hunau.trace.entity.ProductBatch;
import org.hunau.trace.entity.QsCode;
import org.hunau.trace.mapper.CompanyMapper;
import org.hunau.trace.mapper.ProductBatchMapper;
import org.hunau.trace.mapper.QsCodeMapper;
import org.hunau.trace.model.vo.TraceDetailVO;
import org.hunau.trace.service.QsCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class QsCodeServiceImpl implements QsCodeService {

    private static final Set<String> ALLOWED_STATUS = Set.of("active", "invalid", "frozen", "cancelled");

    @Resource
    private QsCodeMapper qsCodeMapper;
    @Resource
    private ProductBatchMapper productBatchMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private TraceExternalClient traceExternalClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<?> generateQs(QsCode qsCode) {
        AssertUtil.notNull(qsCode, "二维码参数不能为空");
        AssertUtil.notEmpty(qsCode.getBatchId(), "批次ID不能为空");
        AssertUtil.notEmpty(qsCode.getCompanyId(), "企业ID不能为空");

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

        String qsId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        String qsUrl = "https://trace.qsguard.com/qs/" + qsId;
        String signPayload = qsId + "|" + qsCode.getBatchId() + "|" + qsCode.getCompanyId() + "|" + now;

        qsCode.setQsId(qsId);
        qsCode.setQsUrl(qsUrl);
        qsCode.setIssueTime(now);
        qsCode.setSm2Sign(Sm2Util.sign(signPayload, "TRACE_PRIVATE_KEY"));
        if (qsCode.getStatus() == null || qsCode.getStatus().isBlank()) {
            qsCode.setStatus("active");
        }
        if (qsCode.getMaxAllowedScans() == null) {
            qsCode.setMaxAllowedScans(5);
        }

        qsCodeMapper.insert(qsCode);
        try {
            traceExternalClient.saveQrProof(qsCode.getQsId(), qsCode.getBatchId(), qsCode.getCompanyId(), qsCode.getSm2Sign());
        } catch (Exception ignored) {
            // Do not fail the issuance path if proof service is temporarily unavailable.
        }

        return R.ok(qsCode);
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
}
