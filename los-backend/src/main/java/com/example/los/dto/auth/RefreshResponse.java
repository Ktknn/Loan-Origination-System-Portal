package com.example.los.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {
    private String accessToken;

    /** Refresh token moi (sau rotation) - chi dung noi bo de set cookie, khong tra client */
    @JsonIgnore
    private String newRefreshToken;
}