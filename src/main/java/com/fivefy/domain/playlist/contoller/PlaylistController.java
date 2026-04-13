package com.fivefy.domain.playlist.contoller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.playlist.dto.response.PlaylistResponse;
import com.fivefy.domain.playlist.service.PlaylistService;
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
