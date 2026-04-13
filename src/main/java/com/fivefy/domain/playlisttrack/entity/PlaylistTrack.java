package com.fivefy.domain.playlisttrack.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.util.ValidationUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "playlist_track",
        uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "index"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playlistId;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Integer index;

    public static PlaylistTrack create(Long playlistId, Long trackId, Integer index){
        ValidationUtils.validateNonNull(playlistId, "playlistId");
        ValidationUtils.validateNonNull(trackId, "trackId");
        ValidationUtils.validateNonNull(index, "index");

        PlaylistTrack playlistTrack = new PlaylistTrack();
        playlistTrack.playlistId = playlistId;
        playlistTrack.trackId = trackId;
        playlistTrack.index = index;

        return playlistTrack;
    }
}
