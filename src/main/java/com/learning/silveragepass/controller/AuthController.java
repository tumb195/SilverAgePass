package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.auth.*;
import com.learning.silveragepass.entity.Role;
import com.learning.silveragepass.entity.Role.ERole;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.repository.RoleRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.security.jwt.JwtUtils;
import com.learning.silveragepass.security.services.UserDetailsImpl;
import com.learning.silveragepass.service.RefreshTokenService;
import com.learning.silveragepass.service.SmsService;
import com.learning.silveragepass.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证控制器
 * 处理用户登录、注册、验证码等认证相关功能
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    VerificationCodeService verificationCodeService;

    @Autowired
    SmsService smsService;

    @Autowired
    RefreshTokenService refreshTokenService;

    // 图片验证码的宽度和高度
    private static final int IMAGE_WIDTH = 100;
    private static final int IMAGE_HEIGHT = 40;

    /**
     * 生成图片验证码
     */
    @GetMapping(value = "/captcha", produces = MediaType.IMAGE_JPEG_VALUE)
    public void generateCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 创建图像缓冲区
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 设置字体
        g.setFont(new Font("Arial", Font.BOLD, 24));

        // 生成随机验证码
        String captchaCode = generateRandomCode(4);

        // 绘制干扰线
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 20; i++) {
            int x1 = new Random().nextInt(IMAGE_WIDTH);
            int y1 = new Random().nextInt(IMAGE_HEIGHT);
            int x2 = x1 + new Random().nextInt(20) - 10;
            int y2 = y1 + new Random().nextInt(20) - 10;
            g.drawLine(x1, y1, x2, y2);
        }

        // 绘制验证码
        g.setColor(Color.BLACK);
        for (int i = 0; i < captchaCode.length(); i++) {
            g.drawString(String.valueOf(captchaCode.charAt(i)), 20 * i + 10, 25);
        }

        // 释放图形上下文
        g.dispose();

        // 将验证码存储在Redis中
        String sessionId = request.getSession().getId();
        verificationCodeService.saveCaptchaCode(sessionId, captchaCode);

        // 设置响应头
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        response.setHeader("Cache-Control", "no-cache");

        // 将图像写入输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        response.getOutputStream().write(outputStream.toByteArray());
    }

    /**
     * 生成随机验证码
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 用户名登录（双Token认证）
     * 
     * @param loginRequest 登录请求（包含用户名和密码）
     * @return 双Token响应（Access Token + Refresh Token）
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 先检查用户是否存在
            if (!userRepository.existsByUsername(loginRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "用户不存在，请先注册"));
            }

            // 检查账号是否被禁用
            userRepository.findByUsername(loginRequest.getUsername()).ifPresent(user -> {
                if (user.getEnabled() != null && !user.getEnabled()) {
                    throw new org.springframework.security.authentication.DisabledException("账号已被禁用");
                }
            });

            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // 更新最后登录时间
            updateLastLoginTime(userDetails.getId());

            // 生成双Token响应
            DualTokenResponse response = generateDualTokenResponse(userDetails);

            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "账号已被禁用，请联系管理员"));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "用户名或密码错误"));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "用户不存在，请先注册"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "登录失败：" + e.getMessage()));
        }
    }

    /**
     * 手机号密码登录（双Token认证）
     * 
     * @param loginRequest 登录请求（包含手机号和密码）
     * @return 双Token响应（Access Token + Refresh Token）
     */
    @PostMapping("/phone-password-login")
    public ResponseEntity<?> phonePasswordLogin(@Valid @RequestBody PhonePasswordLoginRequest loginRequest) {
        // 根据手机号查找用户
        User user = userRepository.findByPhone(loginRequest.getPhone())
                .orElse(null);

        // 用户不存在
        if (user == null) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(400, "该手机号未注册，请先注册"));
        }

        // 检查账号是否被禁用
        if (user.getEnabled() != null && !user.getEnabled()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "账号已被禁用，请联系管理员"));
        }

        // 使用用户名进行认证（Spring Security配置的是用户名认证）
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // 更新最后登录时间
            updateLastLoginTime(userDetails.getId());

            // 生成双Token响应
            DualTokenResponse response = generateDualTokenResponse(userDetails);

            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "账号已被禁用，请联系管理员"));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "手机号或密码错误"));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "该手机号未注册，请先注册"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "登录失败：" + e.getMessage()));
        }
    }

    /**
     * 发送短信验证码
     * 同一手机号60秒内只能发送一次，防止恶意刷短信
     * 
     * @param request     验证码请求（包含手机号）
     * @param httpRequest HTTP请求对象
     * @return 发送结果
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody VerificationCodeRequest request,
            HttpServletRequest httpRequest) {
        // 检查是否可以发送验证码（60秒限制）
        if (!verificationCodeService.canSendSmsCode(request.getPhone())) {
            long remainingSeconds = verificationCodeService.getRemainingSendInterval(request.getPhone());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("发送过于频繁，请" + remainingSeconds + "秒后再试"));
        }

        // 生成短信验证码
        String code = verificationCodeService.generateSmsCode();

        // 调用短信服务发送验证码
        try {
            smsService.sendVerificationCode(request.getPhone(), code);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("验证码发送失败，请稍后重试"));
        }

        // 存储验证码
        verificationCodeService.saveSmsCode(request.getPhone(), code);

        // 记录发送时间（用于60秒限制）
        verificationCodeService.recordSmsSendTime(request.getPhone());

        // 为了测试方便，直接返回验证码（生产环境应该移除）
        return ResponseEntity.ok(ApiResponse.success("验证码发送成功，测试阶段直接返回: " + code, code));
    }

    /**
     * 手机验证码登录（双Token认证）
     * 如果手机号未注册，将自动创建新用户
     * 
     * @param request 手机登录请求（包含手机号和验证码）
     * @return 双Token响应（Access Token + Refresh Token）
     */
    @PostMapping("/phone-login")
    public ResponseEntity<?> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        // 验证验证码（测试阶段：默认验证码"123456"可直接通过）
        boolean isTestCode = "123456".equals(request.getVerificationCode());
        if (!isTestCode
                && !verificationCodeService.validateSmsCode(request.getPhone(), request.getVerificationCode())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "验证码错误或已过期，请重新获取"));
        }

        // 查找用户，如果不存在则创建新用户
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setPhone(request.getPhone());
                    newUser.setUsername(request.getPhone()); // 手机号作为用户名
                    newUser.setPassword(encoder.encode(UUID.randomUUID().toString())); // 随机密码
                    newUser.setEnabled(true); // 新用户默认启用

                    Set<Role> roles = new HashSet<>();
                    Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
                    roles.add(userRole);
                    newUser.setRoles(roles);

                    return userRepository.save(newUser);
                });

        // 检查账号是否被禁用
        if (user.getEnabled() != null && !user.getEnabled()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "账号已被禁用，请联系管理员"));
        }

        // 更新最后登录时间
        updateLastLoginTime(user.getId());

        // 获取用户角色
        java.util.List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        // 生成双Token响应
        DualTokenResponse response = generateDualTokenResponseForUser(user, roles);

        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     * 需要先发送验证码，注册时验证短信验证码
     * 如果不提供用户名，将自动生成唯一的用户昵称（格式：用户+随机字母）
     * 测试阶段：默认验证码"123456"可直接通过验证
     * 
     * @param registerRequest 注册请求（包含手机号、密码、验证码，用户名可选）
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // 验证短信验证码（测试阶段：默认验证码"123456"直接通过）
        if (!"123456".equals(registerRequest.getVerificationCode())) {
            // 如果不是默认验证码，则验证真实的短信验证码
            if (!verificationCodeService.validateSmsCode(registerRequest.getPhone(),
                    registerRequest.getVerificationCode())) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("验证码错误或已过期"));
            }
        }

        // 检查手机号是否已存在
        if (userRepository.existsByPhone(registerRequest.getPhone())) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("该手机号已被注册"));
        }

        // 生成或验证用户名
        String username = registerRequest.getUsername();
        if (username == null || username.trim().isEmpty()) {
            // 自动生成唯一的用户昵称
            username = generateUniqueUsername();
        } else {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(username)) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("用户名已被使用"));
            }
        }

        // 创建新用户
        User user = new User(
                username,
                registerRequest.getPhone(),
                encoder.encode(registerRequest.getPassword()));

        user.setRealName(registerRequest.getRealName());
        user.setEmail(registerRequest.getEmail());

        // 设置用户角色
        Set<String> strRoles = registerRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("系统错误: 未找到用户角色"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("系统错误: 未找到管理员角色"));
                        roles.add(adminRole);
                        break;
                    case "volunteer":
                        Role volunteerRole = roleRepository.findByName(ERole.ROLE_VOLUNTEER)
                                .orElseThrow(() -> new RuntimeException("系统错误: 未找到志愿者角色"));
                        roles.add(volunteerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("系统错误: 未找到用户角色"));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("注册成功", Map.of(
                "userId", savedUser.getId(),
                "username", savedUser.getUsername(),
                "phone", savedUser.getPhone())));
    }

    /**
     * 生成唯一的用户昵称
     * 格式：用户 + 6位随机字母（大小写混合）
     * 
     * @return 唯一的用户昵称
     */
    private String generateUniqueUsername() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        String username;
        int maxAttempts = 10; // 最多尝试10次
        int attempt = 0;

        do {
            StringBuilder sb = new StringBuilder("用户");
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            username = sb.toString();
            attempt++;

            // 如果尝试多次仍然重复，增加字母数量
            if (attempt >= maxAttempts) {
                sb.append(System.currentTimeMillis() % 1000);
                username = sb.toString();
                break;
            }
        } while (userRepository.existsByUsername(username));

        return username;
    }

    /**
     * 更新用户最后登录时间
     * 
     * @param userId 用户ID
     */
    private void updateLastLoginTime(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginTime(java.time.LocalDateTime.now());
            userRepository.save(user);
        });
    }

    // ==================== 双Token认证相关端点 ====================

    /**
     * 刷新Token
     * 使用Refresh Token获取新的Access Token和Refresh Token
     * 
     * @param request Token刷新请求
     * @return 新的双Token响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        DualTokenResponse response = refreshTokenService.validateAndRotate(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * 登出
     * 撤销Refresh Token
     * 
     * @param request Token刷新请求（包含要撤销的Refresh Token）
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody TokenRefreshRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("登出成功"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 为UserDetailsImpl生成双Token响应
     */
    private DualTokenResponse generateDualTokenResponse(UserDetailsImpl userDetails) {
        java.util.List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 生成Access Token
        String accessToken = jwtUtils.generateAccessToken(userDetails.getId(), userDetails.getUsername(), roles);

        // 生成Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        // 计算过期时间
        long accessTokenExpiresAt = System.currentTimeMillis() + jwtUtils.getAccessTokenExpirationMs();
        long refreshTokenExpiresAt = System.currentTimeMillis() + jwtUtils.getRefreshTokenExpirationMs();

        return DualTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtils.getAccessTokenExpirationMs() / 1000)
                .refreshTokenExpiresIn(jwtUtils.getRefreshTokenExpirationMs() / 1000)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .realName(userDetails.getRealName())
                .phone(userDetails.getPhone())
                .roles(roles)
                .build();
    }

    /**
     * 为User实体生成双Token响应
     */
    private DualTokenResponse generateDualTokenResponseForUser(User user, java.util.List<String> roles) {
        // 生成Access Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), roles);

        // 生成Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 计算过期时间
        long accessTokenExpiresAt = System.currentTimeMillis() + jwtUtils.getAccessTokenExpirationMs();
        long refreshTokenExpiresAt = System.currentTimeMillis() + jwtUtils.getRefreshTokenExpirationMs();

        return DualTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtils.getAccessTokenExpirationMs() / 1000)
                .refreshTokenExpiresIn(jwtUtils.getRefreshTokenExpirationMs() / 1000)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .roles(roles)
                .build();
    }
}