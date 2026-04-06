package com.stgian.service;

import com.stgian.dto.AuthDTOs;
import com.stgian.model.User;
import com.stgian.repository.UserRepository;
import com.stgian.security.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service
public class AuthService {

    private static final Logger log = Logger.getLogger(AuthService.class.getName());

    private static final int  MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS    = 15 * 60 * 1000L;

    // Rate limiting por email E por IP
    private final ConcurrentHashMap<String, long[]> loginAttempts = new ConcurrentHashMap<>();

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtil              jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService   userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository       = userRepository;
        this.passwordEncoder      = passwordEncoder;
        this.jwtUtil              = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService   = userDetailsService;
    }

    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest req) {
        String ip = getClientIp();

        // Rate limit por email e por IP (dupla proteção)
        checkRateLimit(req.email());
        checkRateLimit("ip:" + ip);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );
        } catch (BadCredentialsException e) {
            registerFailedAttempt(req.email());
            registerFailedAttempt("ip:" + ip);
            throw new BadCredentialsException("Credenciais invalidas.");
        }

        loginAttempts.remove(req.email());
        loginAttempts.remove("ip:" + ip);

        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        UserDetails details = userDetailsService.loadUserByUsername(req.email());
        String token = jwtUtil.generateToken(details, user.getRole().name(), user.getId());

        return new AuthDTOs.AuthResponse(token, toUserDTO(user));
    }

    public AuthDTOs.AuthResponse register(AuthDTOs.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Nao foi possivel criar a conta com esses dados.");
        }

        User user = User.builder()
            .name(req.name().trim())
            .email(req.email().toLowerCase().trim())
            .password(passwordEncoder.encode(req.password()))
            .role(User.Role.CLIENT)
            .build();

        userRepository.save(user);

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(details, user.getRole().name(), user.getId());

        return new AuthDTOs.AuthResponse(token, toUserDTO(user));
    }

    private void checkRateLimit(String key) {
        long now = System.currentTimeMillis();
        long[] data = loginAttempts.get(key);
        if (data != null) {
            long windowStart = data[0];
            int  count       = (int) data[1];
            if (now - windowStart < WINDOW_MS && count >= MAX_ATTEMPTS) {
                log.warning("Rate limit atingido: " + key);
                throw new IllegalArgumentException("Muitas tentativas. Tente novamente em 15 minutos.");
            }
            if (now - windowStart >= WINDOW_MS) {
                loginAttempts.remove(key);
            }
        }
    }

    private void registerFailedAttempt(String key) {
        long now = System.currentTimeMillis();
        loginAttempts.compute(key, (k, data) -> {
            if (data == null || now - data[0] >= WINDOW_MS)
                return new long[]{ now, 1 };
            data[1]++;
            return data;
        });
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private AuthDTOs.UserDTO toUserDTO(User u) {
        int spent = u.getOrders().stream().mapToInt(o -> o.getTotal()).sum();
        return new AuthDTOs.UserDTO(
            u.getId(), u.getName(), u.getEmail(),
            u.getRole().name(), u.getCreatedAt(),
            u.getOrders().size(), spent
        );
    }
}
