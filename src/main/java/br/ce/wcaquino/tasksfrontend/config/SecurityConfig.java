package br.ce.wcaquino.tasksfrontend.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	// URL que o NAVEGADOR usa para acessar o Keycloak
	@Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri}")
	private String keycloakAuthUri;

	@Value("${app.base-url:http://localhost:9999}")
	private String appBaseUrl;

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
		// Deriva a URL base do Keycloak a partir da authorization-uri já configurada
		// ex: .../protocol/openid-connect/auth -> .../protocol/openid-connect/logout
		return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
			String logoutEndpoint = keycloakAuthUri.replace("/auth", "/logout");
			String redirectUri = appBaseUrl + "/tasks/";
			response.sendRedirect(logoutEndpoint + "?redirect_uri=" + redirectUri);
		};
	}
}
