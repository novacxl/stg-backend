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

        // Limpa CPF — armazena só os 11 dígitos
        String cpfLimpo = req.cpf().replaceAll("[^0-9]", "");
        if (cpfLimpo.length() != 11) {
            throw new IllegalArgumentException("CPF inválido.");
        }

        // Validação básica de CPF (evita sequências óbvias como 111.111.111-11)
        if (!isCpfValido(cpfLimpo)) {
            throw new IllegalArgumentException("CPF inválido.");
        }

        User user = User.builder()
            .name(req.name().trim())
            .email(req.email().toLowerCase().trim())
            .password(passwordEncoder.encode(req.password()))
            .cpf(cpfLimpo)
            .role(User.Role.CLIENT)
            .build();

        userRepository.save(user);

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(details, user.getRole().name(), user.getId());

        return new AuthDTOs.AuthResponse(token, toUserDTO(user));
    }

    // Validação matemática básica do CPF
    private boolean isCpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false; // todos iguais
        int[] d = cpf.chars().map(c -> c - '0').toArray();
        int s1 = 0, s2 = 0;
        for (int i = 0; i < 9; i++) s1 += d[i] * (10 - i);
        int r1 = (s1 * 10) % 11; if (r1 == 10 || r1 == 11) r1 = 0;
        if (r1 != d[9]) return false;
        for (int i = 0; i < 10; i++) s2 += d[i] * (11 - i);
        int r2 = (s2 * 10) % 11; if (r2 == 10 || r2 == 11) r2 = 0;
        return r2 == d[10];
    }

    private void checkRateLimit(String key) {
        long now = System.currentTimeMillis();
        long[] data = loginAttempts.get(key);
        if (data != null) {
            if (now - data[0] < WINDOW_MS && (int) data[1] >= MAX_ATTEMPTS) {
                log.warning("Rate limit: " + key);
                throw new IllegalArgumentException("Muitas tentativas. Tente em 15 minutos.");
            }
            if (now - data[0] >= WINDOW_MS) loginAttempts.remove(key);
        }
    }

    private void registerFailedAttempt(String key) {
        long now = System.currentTimeMillis();
        loginAttempts.compute(key, (k, data) -> {
            if (data == null || now - data[0] >= WINDOW_MS) return new long[]{ now, 1 };
            data[1]++; return data;
        });
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes a =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (a == null) return "unknown";
            HttpServletRequest req = a.getRequest();
            String fwd = req.getHeader("X-Forwarded-For");
            return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) { return "unknown"; }
    }

    public AuthDTOs.UserDTO toUserDTO(User u) {
        int spent = u.getOrders().stream().mapToInt(o -> o.getTotal()).sum();
        return new AuthDTOs.UserDTO(
            u.getId(), u.getName(), u.getEmail(), u.getCpf(),
            u.getRole().name(), u.getCreatedAt(),
            u.getOrders().size(), spent
        );
    }
}
