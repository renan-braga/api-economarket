package br.edu.ifsp.arq.prss6.apieconomarket.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifsp.arq.prss6.apieconomarket.config.JWTBuilder;
import br.edu.ifsp.arq.prss6.apieconomarket.config.JWTParametersConfig;
import br.edu.ifsp.arq.prss6.apieconomarket.config.TokenTypeEnum;
import br.edu.ifsp.arq.prss6.apieconomarket.domain.model.User;
import br.edu.ifsp.arq.prss6.apieconomarket.security.authorization.UserDetail;
import br.edu.ifsp.arq.prss6.apieconomarket.service.RefreshTokenService;
import br.edu.ifsp.arq.prss6.apieconomarket.utils.EndpointsConstMapping;
import br.edu.ifsp.arq.prss6.apieconomarket.utils.UtilsFunc;

public class JWTAuthFilter extends UsernamePasswordAuthenticationFilter {
	
	private final AuthenticationManager authenticationManager;
	
	private final RefreshTokenService refreshTokenService;
	
	public JWTAuthFilter(AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
		this.authenticationManager = authenticationManager;
		this.refreshTokenService = refreshTokenService;
	}
	
	//Realiza a autenticação de usuário
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		
		try {
			User user = new ObjectMapper().readValue(request.getInputStream(), User.class);
			
			return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					user.getEmail(),
					user.getPassword(),
					user.getPermissions().stream().map(p -> new SimpleGrantedAuthority(p.getName())).collect(Collectors.toList())
			));
			
		} catch (IOException | EntityNotFoundException e) {
			throw new RuntimeException("Falha ao autenticar usuário", e);
		}
	}
	
	@Override
	protected void successfulAuthentication(HttpServletRequest request, 
			HttpServletResponse response, 
			FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		
		UserDetail userDetail = (UserDetail) authResult.getPrincipal();
				
		String accessToken = JWTBuilder.createToken(userDetail.getUsername(), userDetail.getId(),
				UtilsFunc.authoritiesToRoleList(userDetail.getAuthorities()), TokenTypeEnum.ACCESS_TOKEN);
		
		String refreshToken = JWTBuilder.createToken(userDetail.getUsername(), userDetail.getId(), 
				UtilsFunc.authoritiesToRoleList(userDetail.getAuthorities()), TokenTypeEnum.REFRESH_TOKEN);
		
		Map<String, String> loginResponse = new HashMap<>();
		loginResponse.put("access_token", accessToken);
		loginResponse.put("refresh_token", refreshToken);
		
		User user = new User();
		user.setId(userDetail.getId());
		
		String userAgent = JWTBuilder.getUserAgent(request);
		
		refreshTokenService.registerRefreshTokenOnDatabase(user, 
				userAgent, 
				refreshToken,
				LocalDateTime.now());
		
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
				.httpOnly(false)
				.secure(true)
				.path(EndpointsConstMapping.AuthEP.MAIN)
				.sameSite("None")
				.maxAge(JWTParametersConfig.REFRESH_TOKEN_EXPIRATION)
				.build();
		
		response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), loginResponse);
	}
}
