package com.fivefy.domain.playlist.contoller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.playlist.dto.request.PlaylistCreateRequest;
import com.fivefy.domain.playlist.dto.response.PlaylistResponse;
import com.fivefy.domain.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping("/playlists")
    public ResponseEntity<BaseResponse<PlaylistResponse>> createPlaylist(
            @RequestParam Long userId,
            @Valid @RequestBody PlaylistCreateRequest request
    ) {
        PlaylistResponse response = playlistService.createPlaylist(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "플레이리스트 생성 성공", response));
    }

    @GetMapping("/playlists")
    public ResponseEntity<BaseResponse<PageResponse<PlaylistResponse>>> getPlaylists(Pageable pageable) {
        PageResponse<PlaylistResponse> response = playlistService.getPlaylists(pageable);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 목록 조회 성공", response)
        );
    }

    @GetMapping("/playlists/{playlistId}")
    public ResponseEntity<BaseResponse<PlaylistResponse>> getPlaylist(@PathVariable Long playlistId) {
        PlaylistResponse response = playlistService.getPlaylist(playlistId);

        return ResponseEntity.ok(
                BaseResponse.success(HttpStatus.OK, "플레이리스트 조회 성공", response)
        );
    }
}
