package com.stgian.service;

import com.stgian.dto.AuthDTOs;
import com.stgian.model.User;
import com.stgian.repository.OrderRepository;
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
    private final ConcurrentHashMap<String, long[]> loginAttempts = new ConcurrentHashMap<>();

    private final UserRepository       userRepository;
    private final OrderRepository      orderRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtil              jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService   userDetailsService;

    public AuthService(UserRepository userRepository, OrderRepository orderRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository; this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder; this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest req) {
        String ip = getClientIp();
        checkRateLimit(req.email()); checkRateLimit("ip:" + ip);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (BadCredentialsException e) {
            registerFailedAttempt(req.email()); registerFailedAttempt("ip:" + ip);
            throw new BadCredentialsException("Credenciais invalidas.");
        }
        loginAttempts.remove(req.email()); loginAttempts.remove("ip:" + ip);
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
        String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(req.email()), user.getRole().name(), user.getId());
        return new AuthDTOs.AuthResponse(token, toUserDTO(user));
    }

    public AuthDTOs.AuthResponse register(AuthDTOs.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new IllegalArgumentException("Nao foi possivel criar a conta com esses dados.");
        User user = User.builder().name(req.name().trim()).email(req.email().toLowerCase().trim())
            .password(passwordEncoder.encode(req.password())).role(User.Role.CLIENT).build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()), user.getRole().name(), user.getId());
        return new AuthDTOs.AuthResponse(token, toUserDTO(user));
    }

    private void checkRateLimit(String key) {
        long now = System.currentTimeMillis();
        long[] data = loginAttempts.get(key);
        if (data != null && now - data[0] < WINDOW_MS && (int) data[1] >= MAX_ATTEMPTS) {
            log.warning("Rate limit: " + key);
            throw new IllegalArgumentException("Muitas tentativas. Tente novamente em 15 minutos.");
        }
        if (data != null && now - data[0] >= WINDOW_MS) loginAttempts.remove(key);
    }

    private void registerFailedAttempt(String key) {
        long now = System.currentTimeMillis();
        loginAttempts.compute(key, (k, d) -> {
            if (d == null || now - d[0] >= WINDOW_MS) return new long[]{ now, 1 };
            d[1]++; return d;
        });
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (a == null) return "unknown";
            HttpServletRequest r = a.getRequest();
            String fwd = r.getHeader("X-Forwarded-For");
            return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : r.getRemoteAddr();
        } catch (Exception e) { return "unknown"; }
    }

    private AuthDTOs.UserDTO toUserDTO(User u) {
        // Queries agregadas — sem carregar lista de pedidos em memória
        long orders = orderRepository.countByUserId(u.getId());
        long spent  = orderRepository.totalSpentByUserId(u.getId());
        return new AuthDTOs.UserDTO(u.getId(), u.getName(), u.getEmail(),
            u.getRole().name(), u.getCreatedAt(), (int) orders, (int) spent);
    }
}
