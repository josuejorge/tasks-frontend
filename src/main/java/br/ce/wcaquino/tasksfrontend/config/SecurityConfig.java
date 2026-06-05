package br.ce.wcaquino.tasksfrontend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			// CSRF desabilitado: os templates Thymeleaf nao incluem token CSRF
			// seguranca garantida pelo OAuth2 session
			.csrf().disable()
			.and()

			.authorizeRequests()
				.anyRequest().authenticated()
			.and()

			// "/" = raiz relativa ao context path do Tomcat (/tasks)
			// evita o duplo path tasks/tasks/
			.oauth2Login()
				.defaultSuccessUrl("/", true)
			.and()

			.logout()
				.logoutSuccessHandler(oidcLogoutSuccessHandler());
	}

	private LogoutSuccessHandler oidcLogoutSuccessHandler() {
		// apos logout no Keycloak, redireciona para a tela de login do Keycloak
		// (Spring Security 5.2.x nao suporta template {baseUrl} aqui)
		return new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
	}
}
