import { Track } from '../types';
import { useMusic } from '../context/MusicContext';
import { Play, Pause, Heart, MoreHorizontal } from 'lucide-react';
import { Button } from './ui/button';
import { Link } from 'react-router';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu';

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const formatPlayCount = (count: number) => {
  if (count >= 1000000) {
    return `${(count / 1000000).toFixed(1)}M`;
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}K`;
  }
  return count.toString();
};

interface TrackListProps {
  tracks: Track[];
  showAlbum?: boolean;
  showPlayCount?: boolean;
}

export function TrackList({ tracks, showAlbum = true, showPlayCount = false }: TrackListProps) {
  const { playTrack, pauseTrack, currentTrack, isPlaying, likedTrackIds, toggleLike, userPlaylists, addToPlaylist } = useMusic();

  const handlePlayPause = (track: Track) => {
    if (currentTrack?.id === track.id) {
      if (isPlaying) {
        pauseTrack();
      } else {
        void playTrack(track, tracks);
      }
    } else {
      void playTrack(track, tracks);
    }
  };

  return (
    <div className="space-y-1">
      {tracks.map((track, index) => {
        const isCurrentTrack = currentTrack?.id === track.id;
        const isLiked = likedTrackIds.includes(track.id);

        return (
          <div
            key={track.id}
            className={`group flex items-center gap-4 px-4 py-2 rounded-md hover:bg-zinc-800/50 transition-colors ${
              isCurrentTrack ? 'bg-zinc-800/50' : ''
            }`}
          >
            {/* Index / Play button */}
            <div className="w-8 flex items-center justify-center">
              <span className="text-zinc-400 text-sm group-hover:hidden">
                {index + 1}
              </span>
              <Button
                variant="ghost"
                size="icon"
                className="hidden group-hover:flex w-8 h-8 text-white"
                onClick={() => handlePlayPause(track)}
              >
                {isCurrentTrack && isPlaying ? (
                  <Pause className="w-4 h-4" fill="currentColor" />
                ) : (
                  <Play className="w-4 h-4" fill="currentColor" />
                )}
              </Button>
            </div>

            {/* Track info */}
            <div className="flex items-center gap-3 flex-1 min-w-0">
              <img
                src={track.coverImage}
                alt={track.title}
                className="w-10 h-10 rounded object-cover"
              />
              <div className="min-w-0 flex-1">
                <Link
                  to={`/track/${track.id}`}
                  className={`font-medium truncate ${
                    isCurrentTrack ? 'text-green-500' : 'text-white'
                  }`}
                >
                  {track.title}
                </Link>
                <div className="text-sm text-zinc-400 truncate">
                  {track.artistName}
                </div>
              </div>
            </div>

            {/* Album */}
            {showAlbum && (
              <div className="hidden md:block text-sm text-zinc-400 truncate w-48">
                {track.albumTitle}
              </div>
            )}

            {/* Play count */}
            {showPlayCount && (
              <div className="hidden lg:block text-sm text-zinc-400 w-24">
                {formatPlayCount(track.playCount)}
              </div>
            )}

            {/* Duration & actions */}
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="icon"
                className="text-zinc-400 hover:text-white"
                onClick={() => toggleLike(track.id)}
              >
                <Heart
                  className={`w-4 h-4 ${isLiked ? 'fill-red-500 text-red-500' : ''}`}
                />
              </Button>
              <span className="text-sm text-zinc-400 w-12 text-right">
                {formatTime(track.duration)}
              </span>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="text-zinc-400 hover:text-white"
                  >
                    <MoreHorizontal className="w-4 h-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {userPlaylists.length === 0 ? (
                    <DropdownMenuItem disabled>플레이리스트 없음</DropdownMenuItem>
                  ) : (
                    userPlaylists.map((playlist) => (
                      <DropdownMenuItem key={playlist.id} onClick={() => addToPlaylist(playlist.id, track.id)}>
                        {playlist.title}에 추가
                      </DropdownMenuItem>
                    ))
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        );
      })}
    </div>
  );
}
