package br.edu.ifsp.arq.prss6.apieconomarket.facade;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.ifsp.arq.prss6.apieconomarket.config.JWTBuilder;
import br.edu.ifsp.arq.prss6.apieconomarket.config.TokenTypeEnum;
import br.edu.ifsp.arq.prss6.apieconomarket.domain.model.User;
import br.edu.ifsp.arq.prss6.apieconomarket.repository.UserRepository;
import br.edu.ifsp.arq.prss6.apieconomarket.utils.UtilsCons;
import br.edu.ifsp.arq.prss6.apieconomarket.utils.UtilsFunc;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthFacade {

	@Autowired
	private UserRepository userRepository;
	
	public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String attribute = request.getHeader(UtilsCons.HEADER_ATTRIBUTE);
		if(attribute != null && attribute.startsWith(UtilsCons.BEARER_ATTRIBUTE_PREFIX)) {
			try {
				String refreshToken = attribute.replace(UtilsCons.BEARER_ATTRIBUTE_PREFIX, "");
				
				DecodedJWT decodedJWT = JWTBuilder.getDecodedJWT(refreshToken);
				User user = userRepository.findByEmail(decodedJWT.getSubject()).orElse(new User());
				
				String accessToken = JWTBuilder.createAccessToken(user.getEmail(), 
						UtilsFunc.permissionsToRoleList(user.getPermissions()), TokenTypeEnum.ACCESS_TOKEN);
				
				Map<String, String> tokens = new HashMap<>();
				tokens.put("access_token", accessToken);
//				tokens.put("refresh_token", refreshToken);
				
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				new ObjectMapper().writeValue(response.getOutputStream(), tokens);
			}
			catch(Exception e) {
				log.error("Erro ao realizar login: {}", e.getMessage());
				response.setHeader("error", e.getMessage());
				response.setStatus(HttpStatus.FORBIDDEN.value());
				
				Map<String, String> error = new HashMap<>();
				error.put("error_message", e.getMessage());
				
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				new ObjectMapper().writeValue(response.getOutputStream(), error);
			}
		}
		else {
			throw new RuntimeException("Refresh token inválido ou não existe");
		}
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String attribute = request.getHeader(UtilsCons.HEADER_ATTRIBUTE);
		if(attribute != null && attribute.startsWith(UtilsCons.BEARER_ATTRIBUTE_PREFIX)) {
			try {
				String refreshToken = attribute.replace(UtilsCons.BEARER_ATTRIBUTE_PREFIX, "");
				
				DecodedJWT decodedJWT = JWTBuilder.getDecodedJWT(refreshToken);
				User user = userRepository.findByEmail(decodedJWT.getSubject()).orElse(new User());
				
				String accessToken = JWTBuilder.createAccessToken(user.getEmail(), 
						UtilsFunc.permissionsToRoleList(user.getPermissions()), TokenTypeEnum.ACCESS_TOKEN);
				
				Map<String, String> tokens = new HashMap<>();
				tokens.put("access_token", accessToken);
				
				//TODO: Invalidar RefreshToken ou excluir
				/* Uma idéia é armazenar o token no banco junto com
				 * o usuário a qual ele pertence, e controlar a validade
				 * dele. É necessário garantir que caso o usuário realize logout
				 * o token não possa mais ser usado por ninguém
				 * */
				
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				new ObjectMapper().writeValue(response.getOutputStream(), tokens);
			}
			catch(Exception e) {
				log.error("Erro ao realizar login: {}", e.getMessage());
				response.setHeader("error", e.getMessage());
				response.setStatus(HttpStatus.FORBIDDEN.value());
				
				Map<String, String> error = new HashMap<>();
				error.put("error_message", e.getMessage());
				
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				new ObjectMapper().writeValue(response.getOutputStream(), error);
			}
		}
		else {
			throw new RuntimeException("Refresh token inválido ou não existe");
		}
	}
}
