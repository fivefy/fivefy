import React, { createContext, ReactNode, useContext, useEffect, useRef, useState } from 'react';
import { Album, Artist, Playlist, Track, User } from '../types';
import { mockAlbums, mockArtists, mockFollowedArtistIds, mockLikedTrackIds, mockPlaylists, mockTracks, mockUser } from '../data/mockData';
import { api } from '../lib/api';

interface MusicContextType {
  currentTrack: Track | null;
  isPlaying: boolean;
  currentTime: number;
  volume: number;
  queue: Track[];
  user: User;
  tracks: Track[];
  albums: Album[];
  artists: Artist[];
  likedTrackIds: string[];
  followedArtistIds: string[];
  userPlaylists: Playlist[];
  isAuthenticated: boolean;
  isLoading: boolean;
  playbackError: string | null;
  actionMessage: string | null;
  refreshData: () => Promise<void>;
  playTrack: (track: Track, newQueue?: Track[]) => Promise<void>;
  pauseTrack: () => void;
  resumeTrack: () => Promise<void>;
  nextTrack: () => void;
  previousTrack: () => void;
  seekTo: (time: number) => void;
  setVolume: (volume: number) => void;
  toggleLike: (trackId: string) => void;
  toggleAlbumLike: (albumId: string) => void;
  toggleFollowArtist: (artistId: string) => void;
  addToPlaylist: (playlistId: string, trackId: string) => void;
  removeFromPlaylist: (playlistId: string, trackId: string) => void;
  createPlaylist: (title: string, description?: string) => void;
  updatePlaylist: (playlistId: string, title: string, description?: string) => void;
  deletePlaylist: (playlistId: string) => void;
}

const MusicContext = createContext<MusicContextType | undefined>(undefined);

export const MusicProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const audioRef = useRef<HTMLAudioElement>(new Audio());
  const [currentTrack, setCurrentTrack] = useState<Track | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [volume, setVolumeState] = useState(0.7);
  const [queue, setQueue] = useState<Track[]>([]);
  const [user, setUser] = useState<User>(mockUser);
  const [tracks, setTracks] = useState<Track[]>(mockTracks);
  const [albums, setAlbums] = useState<Album[]>(mockAlbums);
  const [artists, setArtists] = useState<Artist[]>(mockArtists);
  const [likedTrackIds, setLikedTrackIds] = useState<string[]>(mockLikedTrackIds);
  const [likeIdByTrackId, setLikeIdByTrackId] = useState<Record<string, string>>({});
  const [likeIdByAlbumId, setLikeIdByAlbumId] = useState<Record<string, string>>({});
  const [followedArtistIds, setFollowedArtistIds] = useState<string[]>(mockFollowedArtistIds);
  const [userPlaylists, setUserPlaylists] = useState<Playlist[]>(mockPlaylists);
  const [isLoading, setIsLoading] = useState(true);
  const [playbackError, setPlaybackError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const isAuthenticated = Boolean(localStorage.getItem('fivefy.accessToken') || import.meta.env.VITE_ACCESS_TOKEN);

  const showActionMessage = (message: string) => {
    setActionMessage(message);
    window.setTimeout(() => setActionMessage(null), 2500);
  };

  const refreshData = async () => {
    setIsLoading(true);
    try {
      const apiTracks = await api.tracks();
      if (apiTracks.length > 0) {
        setTracks(apiTracks);
      }

      const uniqueArtists = new Map<string, Artist>();
      const uniqueAlbums = new Map<string, Album>();
      apiTracks.forEach((track) => {
        if (track.artistId) {
          uniqueArtists.set(track.artistId, {
            id: track.artistId,
            name: track.artistName,
            genre: [],
            followers: 0,
          });
        }
        if (track.albumId) {
          uniqueAlbums.set(track.albumId, {
            id: track.albumId,
            title: track.albumTitle,
            artistId: track.artistId,
            artistName: track.artistName,
            releaseDate: '',
            trackCount: apiTracks.filter((item) => item.albumId === track.albumId).length,
          });
        }
      });
      if (uniqueArtists.size > 0) {
        setArtists(Array.from(uniqueArtists.values()));
      }
      if (uniqueAlbums.size > 0) {
        setAlbums(Array.from(uniqueAlbums.values()));
      }
    } catch (error) {
      console.warn('공개 트랙 로딩 실패, mock 데이터 사용', error);
    }

    try {
      setUser(await api.me());
    } catch {
      // 비로그인 상태에서는 mock 사용자 정보로 UI를 유지한다.
    }

    try {
      setUserPlaylists(await api.playlists());
    } catch {
      // 로그인 전에는 내 라이브러리 API가 401일 수 있다.
    }

    try {
      const likes = await api.likes();
      const trackLikes = likes.filter((like) => like.targetType === 'TRACK');
      const albumLikes = likes.filter((like) => like.targetType === 'ALBUM');
      setLikedTrackIds(trackLikes.map((like) => String(like.targetId)));
      setLikeIdByTrackId(Object.fromEntries(trackLikes.map((like) => [String(like.targetId), String(like.id)])));
      setLikeIdByAlbumId(Object.fromEntries(albumLikes.map((like) => [String(like.targetId), String(like.id)])));
    } catch {
      // 로그인 전에는 좋아요 API가 401일 수 있다.
    }

    try {
      const follows = await api.follows();
      setFollowedArtistIds(follows.map((follow) => String(follow.artistId)));
    } catch {
      // 로그인 전에는 팔로우 API가 401일 수 있다.
    }

    setIsLoading(false);
  };

  useEffect(() => {
    void refreshData();

    const audio = audioRef.current;
    const handleTimeUpdate = () => setCurrentTime(Math.floor(audio.currentTime));
    const handleEnded = () => nextTrack();
    const handleError = () => {
      setIsPlaying(false);
      setPlaybackError('오디오 파일을 재생할 수 없습니다. 재생 URL 또는 S3 권한을 확인하세요.');
    };

    audio.volume = volume;
    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('error', handleError);

    return () => {
      audio.pause();
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('error', handleError);
    };
  }, []);

  useEffect(() => {
    audioRef.current.volume = volume;
  }, [volume]);

  const playTrack = async (track: Track, newQueue?: Track[]) => {
    setPlaybackError(null);
    try {
      const response = track.audioUrl ? { audioUrl: track.audioUrl, playCount: track.playCount } : await api.playTrack(track.id);
      const playableTrack = { ...track, audioUrl: response.audioUrl, playCount: response.playCount ?? track.playCount };

      if (!playableTrack.audioUrl) {
        throw new Error('재생 URL이 비어 있습니다.');
      }

      if (newQueue) {
        setQueue(newQueue);
      }
      setCurrentTrack(playableTrack);
      setCurrentTime(0);

      const audio = audioRef.current;
      audio.src = playableTrack.audioUrl;
      audio.currentTime = 0;
      await audio.play();
      setIsPlaying(true);
    } catch (error) {
      console.error('재생 실패', error);
      setIsPlaying(false);
      setPlaybackError(error instanceof Error ? error.message : '재생에 실패했습니다.');
    }
  };

  const pauseTrack = () => {
    audioRef.current.pause();
    setIsPlaying(false);
  };

  const resumeTrack = async () => {
    if (!currentTrack) return;
    try {
      await audioRef.current.play();
      setIsPlaying(true);
    } catch (error) {
      console.error('재생 재개 실패', error);
      setPlaybackError(error instanceof Error ? error.message : '재생 재개에 실패했습니다.');
    }
  };

  const nextTrack = () => {
    if (queue.length === 0 || !currentTrack) return;
    const currentIndex = queue.findIndex((track) => track.id === currentTrack.id);
    const nextIndex = (currentIndex + 1) % queue.length;
    void playTrack(queue[nextIndex], queue);
  };

  const previousTrack = () => {
    if (audioRef.current.currentTime > 3) {
      audioRef.current.currentTime = 0;
      setCurrentTime(0);
      return;
    }
    if (queue.length === 0 || !currentTrack) return;
    const currentIndex = queue.findIndex((track) => track.id === currentTrack.id);
    const prevIndex = currentIndex <= 0 ? queue.length - 1 : currentIndex - 1;
    void playTrack(queue[prevIndex], queue);
  };

  const seekTo = (time: number) => {
    audioRef.current.currentTime = time;
    setCurrentTime(time);
  };

  const setVolume = (newVolume: number) => {
    setVolumeState(newVolume);
  };

  const toggleLike = (trackId: string) => {
    if (!isAuthenticated) {
      showActionMessage('로그인이 필요한 기능입니다.');
      return;
    }

    const wasLiked = likedTrackIds.includes(trackId);
    setLikedTrackIds((prev) => (wasLiked ? prev.filter((id) => id !== trackId) : [...prev, trackId]));

    if (wasLiked) {
      const likeId = likeIdByTrackId[trackId];
      if (likeId) {
        api.unlike(likeId)
          .then(() => showActionMessage('좋아요를 취소했습니다.'))
          .catch(() => setLikedTrackIds((prev) => [...prev, trackId]));
      }
      return;
    }

    api.likeTrack(trackId)
      .then((like) => {
        setLikeIdByTrackId((prev) => ({ ...prev, [trackId]: String(like.id) }));
        showActionMessage('좋아요에 추가했습니다.');
      })
      .catch((error) => {
        setLikedTrackIds((prev) => prev.filter((id) => id !== trackId));
        showActionMessage(error instanceof Error ? error.message : '좋아요 처리에 실패했습니다.');
      });
  };

  const toggleAlbumLike = (albumId: string) => {
    if (!isAuthenticated) {
      showActionMessage('로그인이 필요한 기능입니다.');
      return;
    }

    const likeId = likeIdByAlbumId[albumId];
    if (likeId) {
      api.unlike(likeId)
        .then(() => {
          setLikeIdByAlbumId((prev) => {
            const next = { ...prev };
            delete next[albumId];
            return next;
          });
          showActionMessage('앨범 좋아요를 취소했습니다.');
        })
        .catch((error) => showActionMessage(error instanceof Error ? error.message : '좋아요 취소에 실패했습니다.'));
      return;
    }

    api.likeAlbum(albumId)
      .then((like) => {
        setLikeIdByAlbumId((prev) => ({ ...prev, [albumId]: String(like.id) }));
        showActionMessage('앨범을 좋아요에 추가했습니다.');
      })
      .catch((error) => showActionMessage(error instanceof Error ? error.message : '앨범 좋아요 처리에 실패했습니다.'));
  };

  const toggleFollowArtist = (artistId: string) => {
    if (!isAuthenticated) {
      showActionMessage('로그인이 필요한 기능입니다.');
      return;
    }

    const wasFollowing = followedArtistIds.includes(artistId);
    setFollowedArtistIds((prev) => (wasFollowing ? prev.filter((id) => id !== artistId) : [...prev, artistId]));

    const request = wasFollowing ? api.unfollowArtist(artistId) : api.followArtist(artistId);
    request
      .then(() => showActionMessage(wasFollowing ? '팔로우를 취소했습니다.' : '아티스트를 팔로우했습니다.'))
      .catch((error) => {
        setFollowedArtistIds((prev) => (wasFollowing ? [...prev, artistId] : prev.filter((id) => id !== artistId)));
        showActionMessage(error instanceof Error ? error.message : '팔로우 처리에 실패했습니다.');
      });
  };

  const addToPlaylist = (playlistId: string, trackId: string) => {
    if (!isAuthenticated) {
      showActionMessage('로그인이 필요한 기능입니다.');
      return;
    }

    setUserPlaylists((prev) =>
      prev.map((playlist) =>
        playlist.id === playlistId && !playlist.trackIds.includes(trackId)
          ? { ...playlist, trackIds: [...playlist.trackIds, trackId] }
          : playlist,
      ),
    );
    api.addTrackToPlaylist(playlistId, trackId)
      .then(() => showActionMessage('플레이리스트에 곡을 추가했습니다.'))
      .catch((error) => {
        showActionMessage(error instanceof Error ? error.message : '곡 추가에 실패했습니다.');
        void refreshData();
      });
  };

  const removeFromPlaylist = (playlistId: string, trackId: string) => {
    setUserPlaylists((prev) =>
      prev.map((playlist) =>
        playlist.id === playlistId
          ? { ...playlist, trackIds: playlist.trackIds.filter((id) => id !== trackId) }
          : playlist,
      ),
    );
    api.removeTrackFromPlaylist(playlistId, trackId)
      .then(() => showActionMessage('플레이리스트에서 곡을 제거했습니다.'))
      .catch((error) => {
        showActionMessage(error instanceof Error ? error.message : '곡 제거에 실패했습니다.');
        void refreshData();
      });
  };

  const createPlaylist = (title: string, description?: string) => {
    if (!isAuthenticated) {
      showActionMessage('로그인이 필요한 기능입니다.');
      return;
    }

    const temporaryPlaylist: Playlist = {
      id: `temp-${Date.now()}`,
      title,
      description,
      userId: user.id,
      trackIds: [],
      isPublic: true,
      createdAt: new Date().toISOString(),
    };
    setUserPlaylists((prev) => [...prev, temporaryPlaylist]);
    api.createPlaylist(title, description)
      .then((playlist) => {
        setUserPlaylists((prev) => prev.map((item) => (item.id === temporaryPlaylist.id ? playlist : item)));
        showActionMessage('플레이리스트를 만들었습니다.');
      })
      .catch((error) => {
        showActionMessage(error instanceof Error ? error.message : '플레이리스트 생성에 실패했습니다.');
        void refreshData();
      });
  };

  const updatePlaylist = (playlistId: string, title: string, description?: string) => {
    setUserPlaylists((prev) =>
      prev.map((playlist) => playlist.id === playlistId ? { ...playlist, title, description } : playlist),
    );
    api.updatePlaylist(playlistId, title, description)
      .then((playlist) => {
        setUserPlaylists((prev) => prev.map((item) => (item.id === playlistId ? { ...playlist, trackIds: item.trackIds } : item)));
        showActionMessage('플레이리스트를 수정했습니다.');
      })
      .catch((error) => {
        showActionMessage(error instanceof Error ? error.message : '플레이리스트 수정에 실패했습니다.');
        void refreshData();
      });
  };

  const deletePlaylist = (playlistId: string) => {
    const previous = userPlaylists;
    setUserPlaylists((prev) => prev.filter((playlist) => playlist.id !== playlistId));
    api.deletePlaylist(playlistId)
      .then(() => showActionMessage('플레이리스트를 삭제했습니다.'))
      .catch((error) => {
        setUserPlaylists(previous);
        showActionMessage(error instanceof Error ? error.message : '플레이리스트 삭제에 실패했습니다.');
      });
  };

  return (
    <MusicContext.Provider
      value={{
        currentTrack,
        isPlaying,
        currentTime,
        volume,
        queue,
        user,
        tracks,
        albums,
        artists,
        likedTrackIds,
        followedArtistIds,
        userPlaylists,
        isAuthenticated,
        isLoading,
        playbackError,
        actionMessage,
        refreshData,
        playTrack,
        pauseTrack,
        resumeTrack,
        nextTrack,
        previousTrack,
        seekTo,
        setVolume,
        toggleLike,
        toggleAlbumLike,
        toggleFollowArtist,
        addToPlaylist,
        removeFromPlaylist,
        createPlaylist,
        updatePlaylist,
        deletePlaylist,
      }}
    >
      {children}
    </MusicContext.Provider>
  );
};

export const useMusic = () => {
  const context = useContext(MusicContext);
  if (!context) {
    throw new Error('useMusic must be used within MusicProvider');
  }
  return context;
};
