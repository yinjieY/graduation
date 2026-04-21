package org.hunau.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Service
public class FeatureNormalizer {

    private static final Logger log = LoggerFactory.getLogger(FeatureNormalizer.class);

    private ScalerConfig scalerConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ScalerConfig {
        private String version;
        private String scalerType;
        private String[] featureNames;
        private double[] means;
        private double[] stds;
        private double[] minValues;
        private double[] maxValues;
        private String updateTime;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getScalerType() { return scalerType; }
        public void setScalerType(String scalerType) { this.scalerType = scalerType; }
        public String[] getFeatureNames() { return featureNames; }
        public void setFeatureNames(String[] featureNames) { this.featureNames = featureNames; }
        public double[] getMeans() { return means; }
        public void setMeans(double[] means) { this.means = means; }
        public double[] getStds() { return stds; }
        public void setStds(double[] stds) { this.stds = stds; }
        public double[] getMinValues() { return minValues; }
        public void setMinValues(double[] minValues) { this.minValues = minValues; }
        public double[] getMaxValues() { return maxValues; }
        public void setMaxValues(double[] maxValues) { this.maxValues = maxValues; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    }

    @PostConstruct
    public void init() {
        loadScalerConfig();
    }

    private void loadScalerConfig() {
        try (InputStream is = getClass().getResourceAsStream("/model/qs_risk_scaler.json")) {
            if (is != null) {
                scalerConfig = objectMapper.readValue(is, ScalerConfig.class);
                log.info("✓ 标准化配置加载成功: type={}, features={}",
                        scalerConfig.getScalerType(),
                        Arrays.toString(scalerConfig.getFeatureNames()));
            } else {
                log.warn("⚠️ 未找到标准化配置文件，将使用默认参数");
                initDefaultConfig();
            }
        } catch (IOException e) {
            log.error("❌ 加载标准化配置失败: {}", e.getMessage());
            initDefaultConfig();
        }
    }

    private void initDefaultConfig() {
        scalerConfig = new ScalerConfig();
        scalerConfig.setVersion("1.0");
        scalerConfig.setScalerType("StandardScaler");
        scalerConfig.setFeatureNames(new String[]{"scan_count", "time_variance", "location_variance", "device_count", "ip_count"});
        scalerConfig.setMeans(new double[]{10.2, 0.28, 0.23, 2.8, 1.9});
        scalerConfig.setStds(new double[]{7.8, 0.18, 0.19, 2.2, 1.4});
        log.info("✓ 使用默认标准化配置");
    }

    public float[] normalize(float[] rawFeatures) {
        if (scalerConfig == null || scalerConfig.getMeans() == null) {
            return rawFeatures;
        }

        double[] means = scalerConfig.getMeans();
        double[] stds = scalerConfig.getStds();
        float[] normalized = new float[rawFeatures.length];

        for (int i = 0; i < rawFeatures.length && i < means.length; i++) {
            double std = stds[i];
            if (std == 0) {
                normalized[i] = (float) (rawFeatures[i] - means[i]);
            } else {
                normalized[i] = (float) ((rawFeatures[i] - means[i]) / std);
            }
        }
        return normalized;
    }

    public ScalerConfig getScalerConfig() {
        return scalerConfig;
    }
}