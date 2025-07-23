package com.beaver.core.repository;

import com.beaver.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    User findByEmail(String email);
    User findByEmail(String email);
