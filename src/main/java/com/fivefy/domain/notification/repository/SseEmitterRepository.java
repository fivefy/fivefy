package com.fivefy.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class SseEmitterRepository {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(Long userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public List<SseEmitter> findAllByUserId(Long userId) {
        return emitters.getOrDefault(userId, new CopyOnWriteArrayList<>());
    }

    public void delete(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userId);
        }
    }

    public Set<Long> findAllConnectedUserIds() {
        return emitters.keySet();
    }
}
