package com.fivefy.domain.playback.enums;

public enum PlaybackStatus {

    PLAYING,    // 현재 재생 중
    PAUSED,     // 일시정지 상태
    STOPPED,    // 사용자가 재생을 중단한 상태
    SKIPPED,    // 다음 곡으로 넘어가 종료된 상태
    COMPLETED   // 곡이 끝까지 재생된 상태
}
