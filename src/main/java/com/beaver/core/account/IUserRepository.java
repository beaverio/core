package com.beaver.core.account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    User findByEmail(String email);
}