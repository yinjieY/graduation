package org.hunau.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_batch")
public class ProductBatch {

    @TableId(value = "batch_id", type = IdType.INPUT)
    private String batchId;

    @TableField("company_id")
    private String companyId;

    @TableField("production_date")
    private LocalDateTime productionDate;

    private String ingredients;

    @TableField("production_standard")
    private String productionStandard;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
