package com.nigeria.health.drug.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserDetailsService for the Drug Verification module.
 * Queries the shared users table directly using JdbcTemplate.
 */
@Service
@RequiredArgsConstructor
public class DrugUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String sql = """
                SELECT email, password_hash, role, is_verified, is_active
                FROM users
                WHERE email = ?
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                throw new UsernameNotFoundException(
                        "User not found with email: " + email);
            }
            return User.builder()
                    .username(rs.getString("email"))
                    .password(rs.getString("password_hash"))
                    .authorities(List.of(new SimpleGrantedAuthority(
                            "ROLE_" + rs.getString("role"))))
                    .accountExpired(false)
                    .accountLocked(!rs.getBoolean("is_active"))
                    .credentialsExpired(false)
                    .disabled(!rs.getBoolean("is_verified"))
                    .build();
        }, email);
    }
}