package project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import project.service.JwtService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extrai o cabeçalho "Authorization" da requisição HTTP que veio do Angular
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. Se não existir cabeçalho ou não começar com "Bearer ", ignora e passa pro próximo filtro.
        // O próprio Spring Security vai estourar o erro 403 lá na frente por falta de autenticação.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // ADICIONE ESTA CHECAGEM: Se o que sobrou for a palavra "null" ou estiver vazio, barra logo aqui.
        if (jwt.equals("null") || jwt.isBlank() || jwt.equals("undefined")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Usa o seu JwtService para ler quem é o dono do token
        username = jwtService.extractUsername(jwt);

        // 5. Se encontrou um username e o contexto de segurança atual ainda está vazio
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Busca o usuário no banco de dados usando a interface padrão do Spring
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 6. Usa o seu JwtService para verificar se o token pertence a esse cara e se não está vencido
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 7. Cria o objeto "Crachá de Acesso" do Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Anexa detalhes extras da requisição (como IP do usuário)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 8. Salva o crachá no Contexto de Segurança. A partir desta linha, o usuário está LOGADO!
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. Libera o fluxo para o Spring continuar processando a API (agora com o usuário logado)
        filterChain.doFilter(request, response);
    }
}