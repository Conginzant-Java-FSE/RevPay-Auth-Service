package com.revpay.authservice.repository;

import com.revpay.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = :value OR u.phone = :value")
    Optional<User> findByEmailOrPhone(@Param("value") String value);
}
