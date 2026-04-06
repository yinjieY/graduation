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
        try {
            env = OrtEnvironment.getEnvironment();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("qs_risk_model.onnx")) {
                if (is == null) {
                    modelLoaded = false;
                    log.warn("AI model not found: qs_risk_model.onnx, fallback mode only");
                    return;
                }
                byte[] modelBytes = is.readAllBytes();
                session = env.createSession(modelBytes);
                modelLoaded = true;
                log.info("AI model loaded, bytes={}, inputs={}, outputs={}",
                        modelBytes.length,
                        session.getInputNames(),
                        session.getOutputNames());
            }
        } catch (Exception ex) {
            session = null;
            modelLoaded = false;
            log.warn("AI model init failed, fallback mode only: {}", ex.getMessage());
        }
    }

    public float predict(float scanCount, float timeVar, float locVar, float deviceCount) {
        return predictDetailed(scanCount, timeVar, locVar, deviceCount).score();
    }

    public PredictionResult predictDetailed(float scanCount, float timeVar, float locVar, float deviceCount) {
        if (session == null || env == null) {
            float score = fallbackScore(scanCount, timeVar, locVar, deviceCount);
            return new PredictionResult(score, "fallback:no-session", "null");
        }

        try {
            float[] input = {scanCount, timeVar, locVar, deviceCount};
            String inputName = resolveInputName(session.getInputNames());
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1, 4});
                 OrtSession.Result res = session.run(Collections.singletonMap(inputName, tensor))) {
                Object raw = res.get(0).getValue();
                Float parsed = parseScore(raw);
                if (parsed != null) {
                    return new PredictionResult(clamp01(parsed), "onnx", previewRaw(raw));
                }
                log.warn("ONNX output type is unsupported, fallback scoring will be used, rawType={}", raw == null ? "null" : raw.getClass().getName());
                float score = fallbackScore(scanCount, timeVar, locVar, deviceCount);
                return new PredictionResult(score, "fallback:unsupported-output", previewRaw(raw));
            }
        } catch (Exception e) {
            log.warn("ONNX inference failed, fallback scoring will be used: {}", e.getMessage());
            float score = fallbackScore(scanCount, timeVar, locVar, deviceCount);
            return new PredictionResult(score, "fallback:onnx-exception", e.getClass().getSimpleName() + ":" + e.getMessage());
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
            // In auto mode, values outside [0,1] are very likely logits.
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
        // auto mode: if any value out of probability range, or sum isn't close to 1, treat as logits.
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

    private float fallbackScore(float scanCount, float timeVar, float locVar, float deviceCount) {
        float score = scanCount * 0.08f + timeVar * 0.2f + locVar * 0.3f + deviceCount * 0.1f;
        return clamp01(score / 2f);
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