package com.fivefy.domain.playlisttrack.entity;

import com.fivefy.common.entity.BaseEntity;
import static com.fivefy.common.util.ValidationUtils.validateNonNull;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "playlist_tracks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_playlist_track_playlist_position",
                        columnNames = {"playlist_id", "position"}
                ),
                @UniqueConstraint(
                        name = "uk_playlist_track_playlist_track",
                        columnNames = {"playlist_id", "track_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Integer position;

    public static PlaylistTrack create(Long playlistId, Long trackId, Integer position){
        validateNonNull(playlistId, "playlistId");
        validateNonNull(trackId, "trackId");
        validateNonNull(position, "position");

        if (position <= 0) {
            throw new BusinessException(PlaylistErrorCode.INVALID_POSITION);
        }

        PlaylistTrack playlistTrack = new PlaylistTrack();
        playlistTrack.playlistId = playlistId;
        playlistTrack.trackId = trackId;
        playlistTrack.position = position;

        return playlistTrack;
    }

    public void updatePosition(Integer position) {
        validateNonNull(position, "position");

        if (position <= 0) {
            throw new BusinessException(PlaylistErrorCode.INVALID_POSITION);
        }

        this.position = position;
    }

    public void moveToTemporaryPosition(int tempPosition) {
        this.position = tempPosition;
    }
}
