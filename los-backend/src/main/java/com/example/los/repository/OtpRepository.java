package com.example.los.repository;

import com.example.los.entity.OtpRecord;
import com.example.los.entity.enums.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OtpRepository extends JpaRepository<OtpRecord, String> {

    List<OtpRecord> findByRecipientEmailAndStatus(String email, OtpStatus status);

    @Modifying  
    @Query("UPDATE OtpRecord o SET o.status = 'EXPIRED' WHERE o.recipientEmail = :email AND o.status = 'PENDING'")
    void expireAllPendingForEmail(@Param("email") String email);
}
