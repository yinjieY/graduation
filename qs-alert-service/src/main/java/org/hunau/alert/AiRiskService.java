package org.hunau.alert;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Map;

@Service
public class AiRiskService {

    private OrtEnvironment env;
    private OrtSession session;

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("qs_risk_model.onnx")) {
                if (is == null) {
                    return;
                }
                byte[] modelBytes = is.readAllBytes();
                session = env.createSession(modelBytes);
            }
        } catch (Exception ignored) {
            session = null;
        }
    }

    public float predict(float scanCount, float timeVar, float locVar, float deviceCount) {
        if (session == null || env == null) {
            float score = scanCount * 0.08f + timeVar * 0.2f + locVar * 0.3f + deviceCount * 0.1f;
            return Math.max(0f, Math.min(1f, score / 2f));
        }

        try {
            float[] input = {scanCount, timeVar, locVar, deviceCount};
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1, 4});
                 OrtSession.Result res = session.run(Map.of("features", tensor))) {
                Object value = res.get(0).getValue();
                if (value instanceof float[] floats && floats.length > 1) {
                    return floats[1];
                }
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}