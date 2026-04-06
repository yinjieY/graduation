-- Patch: enlarge qs_url to avoid truncation when storing signed scan entry URLs
USE `yx_trace_core`;

ALTER TABLE `qs_code`
    MODIFY COLUMN `qs_url` VARCHAR(1024) NOT NULL COMMENT '二维码扫码访问地址';

