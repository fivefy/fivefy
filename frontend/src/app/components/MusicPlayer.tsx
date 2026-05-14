import { useMusic } from '../context/MusicContext';
import { Slider } from './ui/slider';
import { Button } from './ui/button';
import { Play, Pause, SkipBack, SkipForward, Heart, Volume2, VolumeX } from 'lucide-react';
import { useEffect, useState } from 'react';

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

export function MusicPlayer() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    volume,
    playTrack,
    pauseTrack,
    resumeTrack,
    nextTrack,
    previousTrack,
    seekTo,
    setVolume,
    toggleLike,
    likedTrackIds,
  } = useMusic();

  const [isMuted, setIsMuted] = useState(false);
  const [previousVolume, setPreviousVolume] = useState(volume);

  if (!currentTrack) {
    return null;
  }

  const isLiked = likedTrackIds.includes(currentTrack.id);

  const handleVolumeToggle = () => {
    if (isMuted) {
      setVolume(previousVolume);
      setIsMuted(false);
    } else {
      setPreviousVolume(volume);
      setVolume(0);
      setIsMuted(true);
    }
  };

  const handleVolumeChange = (value: number[]) => {
    const newVolume = value[0];
    setVolume(newVolume);
    setIsMuted(newVolume === 0);
  };

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-zinc-900 border-t border-zinc-800 px-4 py-3">
      <div className="max-w-screen-2xl mx-auto flex items-center gap-4">
        {/* Track info */}
        <div className="flex items-center gap-3 min-w-0 w-64">
          <img
            src={currentTrack.coverImage}
            alt={currentTrack.title}
            className="w-14 h-14 rounded object-cover"
          />
          <div className="min-w-0 flex-1">
            <div className="text-sm font-medium text-white truncate">
              {currentTrack.title}
            </div>
            <div className="text-xs text-zinc-400 truncate">
              {currentTrack.artistName}
            </div>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="flex-shrink-0"
            onClick={() => toggleLike(currentTrack.id)}
          >
            <Heart
              className={`w-4 h-4 ${isLiked ? 'fill-red-500 text-red-500' : 'text-zinc-400'}`}
            />
          </Button>
        </div>

        {/* Player controls */}
        <div className="flex-1 flex flex-col items-center gap-2">
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={previousTrack}
              className="text-zinc-400 hover:text-white"
            >
              <SkipBack className="w-5 h-5" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={isPlaying ? pauseTrack : resumeTrack}
              className="w-10 h-10 rounded-full bg-white text-black hover:bg-white/90"
            >
              {isPlaying ? (
                <Pause className="w-5 h-5" fill="currentColor" />
              ) : (
                <Play className="w-5 h-5" fill="currentColor" />
              )}
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={nextTrack}
              className="text-zinc-400 hover:text-white"
            >
              <SkipForward className="w-5 h-5" />
            </Button>
          </div>

          {/* Progress bar */}
          <div className="w-full max-w-2xl flex items-center gap-2">
            <span className="text-xs text-zinc-400 w-10 text-right">
              {formatTime(currentTime)}
            </span>
            <Slider
              value={[currentTime]}
              max={currentTrack.duration}
              step={1}
              onValueChange={(value) => seekTo(value[0])}
              className="flex-1"
            />
            <span className="text-xs text-zinc-400 w-10">
              {formatTime(currentTrack.duration)}
            </span>
          </div>
        </div>

        {/* Volume control */}
        <div className="flex items-center gap-2 w-40">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleVolumeToggle}
            className="text-zinc-400 hover:text-white flex-shrink-0"
          >
            {isMuted || volume === 0 ? (
              <VolumeX className="w-5 h-5" />
            ) : (
              <Volume2 className="w-5 h-5" />
            )}
          </Button>
          <Slider
            value={[volume]}
            max={1}
            step={0.01}
            onValueChange={handleVolumeChange}
            className="flex-1"
          />
        </div>
      </div>
    </div>
  );
}
