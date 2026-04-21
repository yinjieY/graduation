package org.hunau.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import org.hunau.common.enums.RiskLevel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qs_code")
public class QsCode {

    @TableId(value = "qs_id", type = IdType.INPUT)
    private String qsId;

    @TableField("batch_id")
    private String batchId;

    @TableField("company_id")
    private String companyId;

    @TableField("qs_url")
    private String qsUrl;

    @TableField("sm2_sign")
    private String sm2Sign;

    @TableField("issue_time")
    private LocalDateTime issueTime;

    private String status;

    @TableField("max_allowed_scans")
    private Integer maxAllowedScans;

    @TableField("freeze_time")
    private LocalDateTime freezeTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private RiskLevel riskLevel;
}
