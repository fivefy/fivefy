package com.fivefy.domain.playlisttrack.entity;

import com.fivefy.common.entity.BaseEntity;
import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "playlist_tracks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "position"})
)
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
    private Integer position;

    public static PlaylistTrack create(Long playlistId, Long trackId, Integer position){
        validateNonNull(playlistId, "playlistId");
        validateNonNull(trackId, "trackId");
        validateNonNull(position, "position");

        PlaylistTrack playlistTrack = new PlaylistTrack();
        playlistTrack.playlistId = playlistId;
        playlistTrack.trackId = trackId;
        playlistTrack.position = position;

        return playlistTrack;
    }

    public void updatePosition(Integer position) {
        validateNonNull(position, "position");
        this.position = position;
    }
}
