package br.ce.wcaquino.tasksfrontend.config;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	// URL que o NAVEGADOR usa para acessar o Keycloak
	@Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri}")
	private String keycloakAuthUri;

	@Value("${app.base-url:http://localhost:9999}")
	private String appBaseUrl;

	@Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
	private String clientId;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// CSRF desabilitado: os templates Thymeleaf nao incluem token CSRF
		http.csrf().disable();

		http
			.authorizeRequests()
				.anyRequest().authenticated()
			.and()

			// "/" = raiz relativa ao context path do Tomcat (/tasks)
			// evita o duplo path tasks/tasks/
			.oauth2Login()
				.defaultSuccessUrl("/", true)
			.and()

			.logout()
				.logoutSuccessHandler(keycloakLogoutSuccessHandler());
	}

	private LogoutSuccessHandler keycloakLogoutSuccessHandler() {
		// Keycloak 18+ exige client_id ou id_token_hint junto com post_logout_redirect_uri
		return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
			String logoutEndpoint = keycloakAuthUri.replace("/auth", "/logout");
			String redirectUri = encode(appBaseUrl + "/tasks/");

			StringBuilder url = new StringBuilder(logoutEndpoint)
				.append("?client_id=").append(clientId)
				.append("&post_logout_redirect_uri=").append(redirectUri);

			// inclui id_token_hint para garantir que o Keycloak aceite o redirect
			if (authentication instanceof OAuth2AuthenticationToken) {
				OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
				if (token.getPrincipal() instanceof OidcUser) {
					String idToken = ((OidcUser) token.getPrincipal()).getIdToken().getTokenValue();
					url.append("&id_token_hint=").append(idToken);
				}
			}

			response.sendRedirect(url.toString());
		};
	}

	private String encode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}
}
