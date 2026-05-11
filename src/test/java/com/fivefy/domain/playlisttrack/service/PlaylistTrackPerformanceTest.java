package com.fivefy.domain.playlisttrack.service;

import com.fivefy.domain.playlist.entity.Playlist;
import com.fivefy.domain.playlist.repository.PlaylistRepository;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackOrderUpdateRequest;
import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import com.fivefy.domain.playlisttrack.repository.PlaylistTrackRepository;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.repository.TrackRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("성능/동시성 관찰용 테스트 - CI 자동 실행 제외")
@SpringBootTest
@ActiveProfiles("test")
class PlaylistTrackPerformanceTest {

    @Autowired
    private PlaylistTrackService playlistTrackService;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaylistTrackRepository playlistTrackRepository;

    private static final Long USER_ID = 1L;
    private static final int MEASURE_COUNT = 30;

    @AfterEach
    void cleanUp() {
        playlistTrackRepository.deleteAllInBatch();
        playlistRepository.deleteAllInBatch();
        trackRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("트랙 수별 순서 변경 평균 응답 시간과 p95 응답 시간을 측정한다")
    void measureReorderPerformanceByTrackCount() {
        measurePerformance(100);
        measurePerformance(1_000);
        measurePerformance(10_000);
    }

    private void measurePerformance(int trackCount) {
        Playlist playlist = createPlaylist(USER_ID, trackCount);
        List<Track> tracks = createTracks(trackCount);
        createPlaylistTracks(playlist.getId(), tracks);

        List<Long> times = new ArrayList<>();

        for (int i = 0; i < MEASURE_COUNT; i++) {
            Long targetTrackId = tracks.get(trackCount - 1).getId();

            PlaylistTrackOrderUpdateRequest request =
                    new PlaylistTrackOrderUpdateRequest(targetTrackId, 1);

            long start = System.nanoTime();

            playlistTrackService.updateTrackOrder(USER_ID, playlist.getId(), request);

            long end = System.nanoTime();

            times.add((end - start) / 1_000_000);
        }

        Collections.sort(times);

        double avg = times.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long p95 = times.get((int) Math.ceil(times.size() * 0.95) - 1);

        System.out.println();
        System.out.println("======================================");
        System.out.println("트랙 수: " + trackCount);
        System.out.println("평균 응답 시간: " + avg + "ms");
        System.out.println("p95 응답 시간: " + p95 + "ms");
        System.out.println("측정 횟수: " + MEASURE_COUNT);
        System.out.println("======================================");
        System.out.println();
    }

    @Test
    @DisplayName("동시에 순서 변경 요청이 들어왔을 때 충돌 여부를 확인한다")
    void concurrentReorderConflictTest() throws InterruptedException {
        int trackCount = 100;

        Playlist playlist = createPlaylist(USER_ID, trackCount);
        List<Track> tracks = createTracks(trackCount);
        createPlaylistTracks(playlist.getId(), tracks);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        Runnable task1 = () -> {
            try {
                startLatch.await();

                PlaylistTrackOrderUpdateRequest request =
                        new PlaylistTrackOrderUpdateRequest(tracks.get(99).getId(), 1);

                playlistTrackService.updateTrackOrder(USER_ID, playlist.getId(), request);
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                endLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();

                PlaylistTrackOrderUpdateRequest request =
                        new PlaylistTrackOrderUpdateRequest(tracks.get(98).getId(), 2);

                playlistTrackService.updateTrackOrder(USER_ID, playlist.getId(), request);
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                endLatch.countDown();
            }
        };

        executorService.submit(task1);
        executorService.submit(task2);

        startLatch.countDown();
        endLatch.await();

        executorService.shutdown();

        assertTrue(exceptions.isEmpty(), () -> "동시 reorder 중 예외 발생: " + exceptions);

        List<PlaylistTrack> ordered = playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(playlist.getId());

        assertEquals(trackCount, ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            assertEquals(i + 1, ordered.get(i).getPosition());
        }

        System.out.println();
        System.out.println("======================================");
        System.out.println("동시 순서 변경 테스트");
        System.out.println("발생한 예외 수: " + exceptions.size());

        for (Exception exception : exceptions) {
            System.out.println("예외 타입: " + exception.getClass().getSimpleName());
            System.out.println("예외 메시지: " + exception.getMessage());
        }

        System.out.println("======================================");
        System.out.println();
    }

    private Playlist createPlaylist(Long userId, int trackCount) {
        Playlist playlist = Playlist.create(
                userId,
                "성능 테스트 플레이리스트-" + trackCount + "-" + System.nanoTime(),
                "성능 테스트 설명"
        );

        return playlistRepository.saveAndFlush(playlist);
    }

    private List<Track> createTracks(int count) {
        List<Track> tracks = new ArrayList<>();

        for (int i = 1; i <= count; i++) {

            Track track = Track.createFreeCreation(
                    null,
                    USER_ID,
                    "track-" + i,
                    "lyrics-" + i,
                    "POP",
                    "https://test-audio-url.com/" + i,
                    180L
            );

            tracks.add(track);
        }

        return trackRepository.saveAllAndFlush(tracks);
    }

    private void createPlaylistTracks(Long playlistId, List<Track> tracks) {
        List<PlaylistTrack> playlistTracks = new ArrayList<>();

        for (int i = 0; i < tracks.size(); i++) {
            PlaylistTrack playlistTrack = PlaylistTrack.create(
                    playlistId,
                    tracks.get(i).getId(),
                    i + 1
            );

            playlistTracks.add(playlistTrack);
        }

        playlistTrackRepository.saveAllAndFlush(playlistTracks);
    }
}
