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
			.authorizeRequests()
				// toda requisicao precisa estar autenticado
				.anyRequest().authenticated()
			.and()

			// login via Keycloak — redireciona automaticamente para a tela de login
			.oauth2Login()
				.defaultSuccessUrl("/tasks/", true)
			.and()

			// logout: limpa sessao local E propaga para o Keycloak
			.logout()
				.logoutSuccessHandler(oidcLogoutSuccessHandler());
	}

	private LogoutSuccessHandler oidcLogoutSuccessHandler() {
		OidcClientInitiatedLogoutSuccessHandler handler =
			new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
		// apos logout no Keycloak, volta para a raiz da aplicacao
		handler.setPostLogoutRedirectUri("{baseUrl}/tasks/");
		return handler;
	}
}
