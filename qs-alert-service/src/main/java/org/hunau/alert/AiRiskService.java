package org.hunau.alert;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

@Service
public class AiRiskService {

    private static final Logger log = LoggerFactory.getLogger(AiRiskService.class);

    private OrtEnvironment env;
    private OrtSession session;

    @Value("${app.ai.positive-class-index:1}")
    private int positiveClassIndex;

    @Value("${app.ai.output-kind:auto}")
    private String outputKind;

    private volatile boolean modelLoaded;

    public record PredictionResult(float score, String mode, String rawPreview) {}

    @PostConstruct
    public void init() {
        log.info("========== AI Risk Service Initialization ==========");
        
        try {
            log.info("Step 1/3: Creating ONNX environment...");
            env = OrtEnvironment.getEnvironment();
            log.info("✓ ONNX environment created successfully");
            
            log.info("Step 2/3: Loading model resource...");
            String resourcePath = "qs_risk_model.onnx";
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            
            if (is == null) {
                log.error("✗ AI model not found in classpath: {}", resourcePath);
                log.error("  - Please verify the file exists in: src/main/resources/");
                modelLoaded = false;
                log.warn("AI model init failed - using fallback mode only");
                return;
            }
            
            log.info("✓ Model resource found, reading bytes...");
            byte[] modelBytes = is.readAllBytes();
            log.info("✓ Model bytes read: {} KB", modelBytes.length / 1024);
            
            log.info("Step 3/3: Creating ONNX session...");
            session = env.createSession(modelBytes);
            modelLoaded = true;
            
            Set<String> inputNames = session.getInputNames();
            Set<String> outputNames = session.getOutputNames();
            log.info("✓ AI model loaded successfully!");
            log.info("  - Inputs: {}", inputNames);
            log.info("  - Outputs: {}", outputNames);
            
            try (OnnxTensor dummy = OnnxTensor.createTensor(env, FloatBuffer.wrap(new float[5]), new long[]{1, 5})) {
                String inputName = inputNames.iterator().next();
                try (OrtSession.Result res = session.run(Collections.singletonMap(inputName, dummy))) {
                    Object output = res.get(0).getValue();
                    log.info("✓ Model inference test passed, output type: {}", 
                            output == null ? "null" : output.getClass().getSimpleName());
                }
            }
            
        } catch (OrtException ex) {
            session = null;
            modelLoaded = false;
            log.error("✗ ONNX Runtime Exception: {}", ex.getMessage());
            log.error("  - Please check ONNX Runtime version compatibility");
            log.warn("AI model init failed - using fallback mode only");
        } catch (Exception ex) {
            session = null;
            modelLoaded = false;
            log.error("✗ Unexpected error during model initialization: {}", ex.getMessage(), ex);
            log.warn("AI model init failed - using fallback mode only");
        }
        
        log.info("=====================================================");
    }

    public float predict(float scanCount, float timeVar, float locVar, float deviceCount) {
        return predictDetailed(scanCount, timeVar, locVar, deviceCount, 0f).score();
    }

    public float predict(float scanCount, float timeVar, float locVar, float deviceCount, float ipCount) {
        return predictDetailed(scanCount, timeVar, locVar, deviceCount, ipCount).score();
    }

    public PredictionResult predictDetailed(float scanCount, float timeVar, float locVar, float deviceCount) {
        return predictDetailed(scanCount, timeVar, locVar, deviceCount, 0f);
    }

    public PredictionResult predictDetailed(float scanCount, float timeVar, float locVar, float deviceCount, float ipCount) {
        if (session == null || env == null) {
            float score = fallbackScore(scanCount, timeVar, locVar, deviceCount, ipCount);
            return new PredictionResult(score, "fallback:no-session", "null");
        }

        try {
            float[] input = {scanCount, timeVar, locVar, deviceCount, ipCount};
            String inputName = resolveInputName(session.getInputNames());
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1, 5});
                 OrtSession.Result res = session.run(Collections.singletonMap(inputName, tensor))) {
                Object raw = res.get(0).getValue();
                Float parsed = parseScore(raw);
                if (parsed != null) {
                    return new PredictionResult(clamp01(parsed), "onnx", previewRaw(raw));
                }
                log.warn("ONNX output type is unsupported, fallback scoring will be used, rawType={}", raw == null ? "null" : raw.getClass().getName());
                float score = fallbackScore(scanCount, timeVar, locVar, deviceCount, ipCount);
                return new PredictionResult(score, "fallback:unsupported-output", previewRaw(raw));
            }
        } catch (OrtException e) {
            log.warn("ONNX inference failed (OrtException): {}", e.getMessage());
            float score = fallbackScore(scanCount, timeVar, locVar, deviceCount, ipCount);
            return new PredictionResult(score, "fallback:ort-exception", e.getMessage());
        } catch (Exception e) {
            log.warn("ONNX inference failed (Exception): {}", e.getMessage());
            float score = fallbackScore(scanCount, timeVar, locVar, deviceCount, ipCount);
            return new PredictionResult(score, "fallback:exception", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    private String resolveInputName(Set<String> inputNames) {
        if (inputNames == null || inputNames.isEmpty()) {
            return "features";
        }
        return inputNames.iterator().next();
    }

    private Float parseScore(Object value) {
        float[] row = extractRow(value);
        if (row == null || row.length == 0) {
            return null;
        }
        if (row.length == 1) {
            return normalizeSingleValue(row[0]);
        }
        int idx = Math.max(0, Math.min(positiveClassIndex, row.length - 1));
        if (isLogitsVector(row)) {
            return softmaxProb(row, idx);
        }
        return row[idx];
    }

    private float[] extractRow(Object value) {
        if (value instanceof float[] arr) {
            return arr;
        }
        if (value instanceof float[][] arr2) {
            if (arr2.length == 0 || arr2[0] == null) {
                return null;
            }
            return arr2[0];
        }
        if (value instanceof double[] darr) {
            float[] row = new float[darr.length];
            for (int i = 0; i < darr.length; i++) {
                row[i] = (float) darr[i];
            }
            return row;
        }
        if (value instanceof double[][] darr2) {
            if (darr2.length == 0 || darr2[0] == null) {
                return null;
            }
            float[] row = new float[darr2[0].length];
            for (int i = 0; i < darr2[0].length; i++) {
                row[i] = (float) darr2[0][i];
            }
            return row;
        }
        return null;
    }

    private float normalizeSingleValue(float v) {
        if (isLogitsMode()) {
            return sigmoid(v);
        }
        if (v < 0f || v > 1f) {
            return sigmoid(v);
        }
        return v;
    }

    private boolean isLogitsVector(float[] row) {
        if (isLogitsMode()) {
            return true;
        }
        if (isProbMode()) {
            return false;
        }
        float sum = 0f;
        for (float v : row) {
            if (v < 0f || v > 1f) {
                return true;
            }
            sum += v;
        }
        return Math.abs(sum - 1f) > 0.15f;
    }

    private boolean isLogitsMode() {
        return "logits".equalsIgnoreCase(outputKind);
    }

    private boolean isProbMode() {
        return "prob".equalsIgnoreCase(outputKind) || "probability".equalsIgnoreCase(outputKind);
    }

    private float softmaxProb(float[] row, int idx) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : row) {
            max = Math.max(max, v);
        }
        double sum = 0d;
        for (float v : row) {
            sum += Math.exp(v - max);
        }
        return (float) (Math.exp(row[idx] - max) / Math.max(sum, 1e-12));
    }

    private float sigmoid(float x) {
        return (float) (1.0d / (1.0d + Math.exp(-x)));
    }

    private float fallbackScore(float scanCount, float timeVar, float locVar, float deviceCount, float ipCount) {
        float score = scanCount * 0.08f + timeVar * 0.2f + locVar * 0.3f + deviceCount * 0.1f + ipCount * 0.08f;
        return clamp01(score / 2.5f);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private String previewRaw(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof float[] arr) {
            return "float[] len=" + arr.length + ", first=" + Arrays.toString(Arrays.copyOf(arr, Math.min(arr.length, 4)));
        }
        if (value instanceof float[][] arr2) {
            if (arr2.length > 0 && arr2[0] != null) {
                float[] first = Arrays.copyOf(arr2[0], Math.min(arr2[0].length, 4));
                return "float[][] rows=" + arr2.length + ", cols=" + arr2[0].length + ", first=" + Arrays.toString(first);
            }
            return "float[][] rows=" + arr2.length;
        }
        if (value instanceof double[] arr) {
            return "double[] len=" + arr.length + ", first=" + Arrays.toString(Arrays.copyOf(arr, Math.min(arr.length, 4)));
        }
        if (value instanceof double[][] arr2) {
            if (arr2.length > 0 && arr2[0] != null) {
                double[] first = Arrays.copyOf(arr2[0], Math.min(arr2[0].length, 4));
                return "double[][] rows=" + arr2.length + ", cols=" + arr2[0].length + ", first=" + Arrays.toString(first);
            }
            return "double[][] rows=" + arr2.length;
        }
        return value.getClass().getSimpleName();
    }
}
