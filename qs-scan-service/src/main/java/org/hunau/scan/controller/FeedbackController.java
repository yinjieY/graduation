package org.hunau.scan.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.hunau.common.R;
import org.hunau.common.util.JwtUtil;
import org.hunau.scan.model.FeedbackStatusUpdateRequest;
import org.hunau.scan.service.FeedbackService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
        String submitterIp = request.getRemoteAddr();
        return feedbackService.submit(qsId, feedbackType, fp, submitterIp, region, description, latitude, longitude, image);
    }

    @GetMapping("/status/{feedbackId}")
    public R<?> status(@PathVariable String feedbackId) {
        return feedbackService.status(feedbackId);
    }

    @GetMapping("/list")
    public R<?> list(@RequestParam(value = "companyId", required = false) String companyIdParam,
                     @RequestHeader(value = "Authorization", required = false) String authorization,
                     Authentication authentication) {
        String role = resolveRole(authentication);
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return feedbackService.list(role, companyId);
    }

    @GetMapping("/detail/{feedbackId}")
    public R<?> detail(@PathVariable String feedbackId,
                       @RequestHeader(value = "Authorization", required = false) String authorization,
                       Authentication authentication) {
        return feedbackService.detail(feedbackId, resolveRole(authentication), resolveCompanyId(authorization));
    }

    @PutMapping("/status/{feedbackId}")
    public R<?> updateStatus(@PathVariable String feedbackId,
                             @RequestBody FeedbackStatusUpdateRequest request,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = resolveRole(authentication);
        String operator = authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
        String companyId = resolveCompanyId(authorization);
        return feedbackService.updateStatus(
                feedbackId,
                request == null ? null : request.getStatus(),
                request == null ? null : request.getHandleNote(),
                role,
                operator,
                companyId
        );
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

    private String resolveRole(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String item = authority.getAuthority();
            if (item != null && item.startsWith("ROLE_")) {
                return item.substring(5);
            }
        }
        return "";
    }

    private String resolveCompanyId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        String token = authorization.substring(7);
        if (!JwtUtil.validate(token)) {
            return "";
        }
        String claim = JwtUtil.getCompanyId(token);
        return claim == null ? "" : claim.trim();
    }
}

