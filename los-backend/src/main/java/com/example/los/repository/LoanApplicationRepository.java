package com.example.los.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.los.entity.LoanApplication;
import com.example.los.entity.enums.LoanStatus;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {
    @Query("SELECT la FROM LoanApplication la JOIN la.user u WHERE u.cccd = :cccd ORDER BY la.createdAt DESC")
    List<LoanApplication> findByUserCccdOrderByCreatedAtDesc(@Param("cccd") String cccd);

    @Query("SELECT la FROM LoanApplication la JOIN la.user u WHERE u.cccd = :cccd AND la.createdAt BETWEEN :startDate AND :endDate ORDER BY la.createdAt DESC")
    List<LoanApplication> findByUserCccdAndCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("cccd") String cccd,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    List<LoanApplication> findByUserUserId(String userId);

    List<LoanApplication> findByStatus(LoanStatus status);

    Optional<LoanApplication> findByLoanApplicationId(String loanApplicationId);

    @Query("SELECT la FROM LoanApplication la JOIN la.user u WHERE u.email = :email ORDER BY la.createdAt DESC")
    List<LoanApplication> findByRecipientEmail(@Param("email") String email);
}
