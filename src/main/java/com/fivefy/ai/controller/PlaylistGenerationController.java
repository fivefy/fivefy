package com.fivefy.ai.controller;

import com.fivefy.ai.dto.request.PlaylistGenerateRequest;
import com.fivefy.ai.dto.response.PlaylistGenerateResponse;
import com.fivefy.ai.service.PlaylistGenerationService;
import com.fivefy.common.dto.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/playlists")
@RequiredArgsConstructor
public class PlaylistGenerationController {

    private final PlaylistGenerationService playlistGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<BaseResponse<PlaylistGenerateResponse>> generate(
            @Valid @RequestBody PlaylistGenerateRequest request) {

        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 자동 생성 성공", playlistGenerationService.generate(request))
        );
    }
}
