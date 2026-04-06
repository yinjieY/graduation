package org.hunau.trace.service;

import cn.hutool.extra.qrcode.QrCodeUtil;
import org.hunau.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class QrCodeImageService {

    @Value("${app.trace.qr-image-dir:./data/qr}")
    private String qrImageDir;

    @Value("${app.trace.public-base-url:http://localhost:9092}")
    private String publicBaseUrl;

    public String generate(String qsId, String content) {
        try {
            Path dir = Paths.get(qrImageDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path output = dir.resolve(qsId + ".png");

            QrCodeUtil.generate(content, 320, 320, output.toFile());
            return buildPublicImageUrl(qsId);
        } catch (IOException ex) {
            throw new BusinessException("二维码图片生成失败: " + ex.getMessage());
        }
    }

    public Resource loadImage(String fileName) {
        Path file = Paths.get(qrImageDir).toAbsolutePath().normalize().resolve(fileName);
        return new FileSystemResource(file);
    }

    public String buildPublicImageUrl(String qsId) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/trace/qs/image/" + qsId + ".png";
    }
}


