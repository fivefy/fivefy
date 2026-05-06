package com.fivefy.domain.chat.repository;

import com.fivefy.domain.chat.entity.ChatMessageTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageTrackRepository extends JpaRepository<ChatMessageTrack, ChatMessageTrack.PK> {
}
