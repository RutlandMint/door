package org.rutlandmint.mgmt.door.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rutlandmint.mgmt.door.Member;
import org.rutlandmint.mgmt.door.MemberDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenFilter extends OncePerRequestFilter {

	@Autowired
	private MemberDatabase db;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final String email = "bkuker@billkuker.com";// request.getHeader("X-Email");

		Optional.ofNullable(email)//
				.flatMap(db::getMemberByEmail)//
				.map(this::createAuthentication)//
				.ifPresent(this::setAuthentication);

		filterChain.doFilter(request, response);
	}

	private void setAuthentication(Authentication authentication) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	private Authentication createAuthentication(Member m) {
		final List<GrantedAuthority> authorities;
		if ("MINT Staff".equals(m.level)) {
			authorities = AuthorityUtils.createAuthorityList("ROLE_STAFF", "ROLE_MEMBER");
		} else {
			authorities = AuthorityUtils.createAuthorityList("ROLE_MEMBER");
		}
		return new PreAuthenticatedAuthenticationToken(m.email, "na", authorities);
	}
}
