package br.ce.wcaquino.tasksfrontend.controller;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

import br.ce.wcaquino.tasksfrontend.model.Todo;

@Controller
public class TasksController {

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	@Value("${backend.host}")
	private String BACKEND_HOST;

	@Value("${backend.port}")
	private String BACKEND_PORT;

	@Value("${app.version}")
	private String VERSION;

	public String getBackendURL() {
		return "http://" + BACKEND_HOST + ":" + BACKEND_PORT;
	}

	// monta o header Authorization: Bearer <token> usando o token do Keycloak
	private HttpHeaders buildAuthHeaders(Authentication authentication) {
		HttpHeaders headers = new HttpHeaders();
		if (authentication instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
			OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
				oauthToken.getAuthorizedClientRegistrationId(),
				oauthToken.getName()
			);
			if (client != null && client.getAccessToken() != null) {
				headers.setBearerAuth(client.getAccessToken().getTokenValue());
			}
		}
		return headers;
	}

	@GetMapping("")
	public String index(Model model, Authentication authentication) {
		String role = extractRole(authentication);
		model.addAttribute("username", authentication.getName());
		model.addAttribute("role", role);
		if (VERSION.startsWith("build"))
			model.addAttribute("version", VERSION);

		// QA nao tem acesso a tasks — exibe pagina sem a listagem
		if ("qa".equals(role)) {
			model.addAttribute("todos", java.util.Collections.emptyList());
			model.addAttribute("info", "Seu perfil (QA) nao tem acesso a tarefas.");
		} else {
			model.addAttribute("todos", getTodos(authentication));
		}
		return "index";
	}

	@SuppressWarnings("unchecked")
	private String extractRole(Authentication authentication) {
		if (!(authentication instanceof OAuth2AuthenticationToken)) return "";
		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
		if (!(token.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser)) return "";

		org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser =
			(org.springframework.security.oauth2.core.oidc.user.OidcUser) token.getPrincipal();

		java.util.Map<String, Object> realmAccess = oidcUser.getAttribute("realm_access");
		if (realmAccess == null) return "";

		java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
		if (roles == null) return "";

		// prioridade: ADMIN > QA > USER
		for (String priority : new String[]{"ADMIN", "QA", "USER"}) {
			if (roles.stream().anyMatch(r -> r.equalsIgnoreCase(priority)))
				return priority.toLowerCase();
		}
		return "";
	}

	@GetMapping("debug")
	@org.springframework.web.bind.annotation.ResponseBody
	public Object debug(Authentication authentication) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = buildAuthHeaders(authentication);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		return restTemplate.exchange(
			getBackendURL() + "/tasks-backend/debug/me",
			HttpMethod.GET, entity, Object.class).getBody();
	}

	@GetMapping("add")
	public String add(Model model) {
		model.addAttribute("todo", new Todo());
		return "add";
	}

	@PostMapping("save")
	public String save(Todo todo, Model model, Authentication authentication) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = buildAuthHeaders(authentication);
			headers.set("Content-Type", "application/json");
			HttpEntity<Todo> entity = new HttpEntity<>(todo, headers);
			restTemplate.postForObject(
				getBackendURL() + "/tasks-backend/todo", entity, Object.class);
			model.addAttribute("success", "Success!!");
			model.addAttribute("todos", getTodos(authentication));
			return "index";
		} catch (Exception e) {
			try {
				Pattern compile = Pattern.compile("message\":\"(.*)\",");
				Matcher m = compile.matcher(e.getMessage());
				m.find();
				model.addAttribute("error", m.group(1));
			} catch (Exception ex) {
				model.addAttribute("error", e.getMessage());
			}
			model.addAttribute("todo", todo);
			model.addAttribute("todos", getTodos(authentication));
			return "add";
		}
	}

	@GetMapping("delete/{id}")
	public String delete(@PathVariable Long id, Model model, Authentication authentication) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = buildAuthHeaders(authentication);
			HttpEntity<Void> entity = new HttpEntity<>(headers);
			restTemplate.exchange(
				getBackendURL() + "/tasks-backend/todo/" + id,
				HttpMethod.DELETE, entity, Void.class);
			model.addAttribute("success", "Success!");
		} catch (Exception e) {
			model.addAttribute("error", "Failed to delete task: " + e.getMessage());
		}
		model.addAttribute("todos", getTodos(authentication));
		return "index";
	}

	@SuppressWarnings("unchecked")
	private List<Todo> getTodos(Authentication authentication) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = buildAuthHeaders(authentication);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		ResponseEntity<List> response = restTemplate.exchange(
			getBackendURL() + "/tasks-backend/todo",
			HttpMethod.GET, entity, List.class);
		return response.getBody();
	}
}
