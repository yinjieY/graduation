package org.hunau.scan.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.hunau.common.R;
import org.hunau.common.util.JwtUtil;
import org.hunau.scan.service.FeedbackService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/scan/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<?> submit(@RequestParam String qsId,
                       @RequestParam String feedbackType,
                       @RequestParam(required = false) String deviceFingerprint,
                       @RequestParam String region,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) Double latitude,
                       @RequestParam(required = false) Double longitude,
                       @RequestPart("image") MultipartFile image,
                       HttpServletRequest request) {
        String fp = (deviceFingerprint == null || deviceFingerprint.isBlank())
                ? fallbackFingerprint(request)
                : deviceFingerprint.trim();
        return feedbackService.submit(qsId, feedbackType, fp, region, description, latitude, longitude, image);
    }

    @GetMapping("/status/{feedbackId}")
    public R<?> status(@PathVariable String feedbackId) {
        return feedbackService.status(feedbackId);
    }

    @GetMapping("/list")
    public R<?> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                     Authentication authentication) {
        String role = "";
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String item = authority.getAuthority();
                if (item != null && item.startsWith("ROLE_")) {
                    role = item.substring(5);
                    break;
                }
            }
        }
        String companyId = "";
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (JwtUtil.validate(token)) {
                String claim = JwtUtil.getCompanyId(token);
                companyId = claim == null ? "" : claim.trim();
            }
        }
        return feedbackService.list(role, companyId);
    }

    @GetMapping(value = "/image/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> image(@PathVariable String fileName) {
        Resource image = feedbackService.loadImage(fileName);
        if (!image.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    private String fallbackFingerprint(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        return "anon-" + Math.abs((String.valueOf(ua) + "|" + ip).hashCode());
    }
}

