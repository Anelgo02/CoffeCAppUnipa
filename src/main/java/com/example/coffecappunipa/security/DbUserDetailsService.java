package com.example.coffecappunipa.security;

import com.example.coffecappunipa.persistence.dao.UserDAO;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        var opt = userDAO.findByUsernameWithPassword(username); // da aggiungere
        if (opt.isEmpty()) throw new UsernameNotFoundException("User not found");

        var u = opt.get();

        String role = (u.getRole() == null) ? "" : u.getRole().trim().toUpperCase();
        String pwdHash = u.getPasswordHash();

        if (pwdHash == null || pwdHash.isBlank()) {
            throw new UsernameNotFoundException("User has no password_hash");
        }

        return new User(
                u.getUsername(),
                pwdHash,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
