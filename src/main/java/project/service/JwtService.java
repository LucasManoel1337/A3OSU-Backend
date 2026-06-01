package project.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${api.security.token.secret}")
    private String secretKey;

    // Gera o token apenas usando os dados essenciais do usuário
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // Método principal de geração: constrói o JWT com claims, validade e a assinatura
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // O "dono" do token
                .setIssuedAt(new Date(System.currentTimeMillis())) // Data de criação
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Expira em 24 horas
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Assinatura com a chave secreta
                .compact();
    }

    // Lê o token e devolve apenas o nome de usuário (Subject)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Verifica se o token pertence de fato a este usuário e se não está vencido
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // --- Métodos Privados de Apoio Interno ---

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Função genérica para extrair qualquer informação (Claim) do corpo do token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Descriptografa o token usando a mesma chave secreta para poder ler o conteúdo
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Transforma a string estática da chave em um objeto Key legível para o algoritmo HS256
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}