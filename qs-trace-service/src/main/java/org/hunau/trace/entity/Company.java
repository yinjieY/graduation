package org.hunau.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("company")
public class Company {

    @TableId(value = "company_id", type = IdType.INPUT)
    private String companyId;

    private String name;

    private String level;

    private String address;

    @TableField("contact_phone")
    private String contactPhone;

    private String email;

    private Double lat;

    private Double lng;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
