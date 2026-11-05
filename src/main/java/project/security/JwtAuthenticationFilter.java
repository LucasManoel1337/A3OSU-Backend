package project.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import project.repository.UserRepository;
import project.service.JwtService;
import project.service.RefreshSessionService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshSessionService refreshSessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extração do Header
        final String authHeader = request.getHeader("Authorization");

        // Log básico de todas as requisições que passam por aqui
        //System.out.println("--- DEBUG: Filtro JWT ---");
        //System.out.println("URI: " + request.getRequestURI());
        //System.out.println("AuthHeader: " + authHeader);

        // 2. Validação inicial
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("DEBUG: AuthHeader ausente ou sem 'Bearer'");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extração e validação do token
        final String jwt = authHeader.substring(7);

        if (jwt.equals("null") || jwt.isBlank()) {
            System.out.println("DEBUG: Token é nulo ou vazio");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);
            //System.out.println("DEBUG: Username extraído: " + username);

            // 4. Verificação de Segurança
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário do token não encontrado."));

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    String sessionId = jwtService.extractSessionId(jwt);
                    if (sessionId == null || !refreshSessionService.recordActivity(sessionId, username)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sessão inválida ou expirada.");
                        return;
                    }

                    //System.out.println("DEBUG: Token válido! Logando usuário: " + username);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    System.out.println("DEBUG: Token inválido para o usuário " + username);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("DEBUG: Erro ao processar JWT: " + e.getMessage());
        }

        // 5. Continua a corrente
        filterChain.doFilter(request, response);
    }
}