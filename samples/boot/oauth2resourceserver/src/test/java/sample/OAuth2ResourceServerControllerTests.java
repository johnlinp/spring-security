/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 *
 * @author Jérôme Wacongne &lt;ch4mp@c4-soft.com&gt;
 * @author Josh Cummings
 * @since 5.2.0
 *
 */
@RunWith(SpringRunner.class)
@WebMvcTest(OAuth2ResourceServerController.class)
public class OAuth2ResourceServerControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	JwtDecoder jwtDecoder;

	@Test
	public void indexGreetsAuthenticatedUser() throws Exception {
		mockMvc.perform(get("/").with(jwt(jwt -> jwt.subject("ch4mpy"))))
				.andExpect(content().string(is("Hello, ch4mpy!")));
	}

	@Test
	public void messageCanBeReadWithScopeMessageReadAuthority() throws Exception {
		mockMvc.perform(get("/message").with(jwt(jwt -> jwt.claim("scope", "message:read"))))
				.andExpect(content().string(is("secret message")));

		mockMvc.perform(get("/message")
				.with(jwt().authorities(new SimpleGrantedAuthority(("SCOPE_message:read")))))
				.andExpect(content().string(is("secret message")));
	}

	@Test
	public void messageCanNotBeReadWithoutScopeMessageReadAuthority() throws Exception {
		mockMvc.perform(get("/message").with(jwt()))
				.andExpect(status().isForbidden());
	}

	@Test
	public void messageCanNotBeCreatedWithoutAnyScope() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("scope", "")
				.build();
		when(jwtDecoder.decode(anyString())).thenReturn(jwt);
		mockMvc.perform(post("/message")
				.content("Hello message")
				.header("Authorization", "Bearer " + jwt.getTokenValue()))
				.andExpect(status().isForbidden());
	}

	@Test
	public void messageCanNotBeCreatedWithScopeMessageReadAuthority() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("scope", "message:read")
				.build();
		when(jwtDecoder.decode(anyString())).thenReturn(jwt);
		mockMvc.perform(post("/message")
				.content("Hello message")
				.header("Authorization", "Bearer " + jwt.getTokenValue()))
				.andExpect(status().isForbidden());
	}

	@Test
	public void messageCanBeCreatedWithScopeMessageWriteAuthority()
			throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("scope", "message:write")
				.build();
		when(jwtDecoder.decode(anyString())).thenReturn(jwt);
		mockMvc.perform(post("/message")
				.content("Hello message")
				.header("Authorization", "Bearer " + jwt.getTokenValue()))
				.andExpect(status().isOk())
				.andExpect(content().string(is("Message was created. Content: Hello message")));
	}
}
