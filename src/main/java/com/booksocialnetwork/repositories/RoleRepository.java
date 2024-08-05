package com.booksocialnetwork.repositories;

import com.booksocialnetwork.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Integer> {
    Optional<Role> findByName(String roleName);
}
