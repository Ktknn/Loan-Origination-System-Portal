package com.example.los.repository;

import com.example.los.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByCccd(String cccd);

    boolean existsByEmail(String email);

    boolean existsByCccd(String cccd);

    /** Login bằng email hoặc số điện thoại */
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);
}
