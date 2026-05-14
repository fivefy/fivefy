import { useParams, Link, useNavigate } from 'react-router';
import { useMusic } from '../context/MusicContext';
import { TrackList } from '../components/TrackList';
import { Button } from '../components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '../components/ui/dialog';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Play, Music, Plus, Trash2, Pencil } from 'lucide-react';
import { useEffect, useState } from 'react';

export function Playlist() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const {
    userPlaylists,
    tracks,
    playTrack,
    addToPlaylist,
    removeFromPlaylist,
    updatePlaylist,
    deletePlaylist,
  } = useMusic();
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  
  const playlist = userPlaylists.find((p) => p.id === id);

  useEffect(() => {
    if (!playlist) return;
    setEditTitle(playlist.title);
    setEditDescription(playlist.description ?? '');
  }, [playlist?.id, playlist?.title, playlist?.description]);

  if (!playlist) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-white mb-2">
          플레이리스트를 찾을 수 없습니다
        </h2>
        <Link to="/library" className="text-purple-500 hover:underline">
          보관함으로 돌아가기
        </Link>
      </div>
    );
  }

  const playlistTracks = playlist.trackIds
    .map((trackId) => tracks.find((t) => t.id === trackId))
    .filter(Boolean) as any[];
  const availableTracks = tracks.filter((track) => !playlist.trackIds.includes(track.id));

  const totalDuration = playlistTracks.reduce(
    (sum, track) => sum + track.duration,
    0
  );
  const totalMinutes = Math.floor(totalDuration / 60);

  const handlePlayPlaylist = () => {
    if (playlistTracks.length > 0) {
      void playTrack(playlistTracks[0], playlistTracks);
    }
  };

  return (
    <div className="space-y-8">
      {/* Playlist Header */}
      <div className="flex flex-col md:flex-row gap-8 items-start">
        <div className="w-full md:w-64 aspect-square rounded-lg overflow-hidden bg-gradient-to-br from-purple-900 to-zinc-900 flex-shrink-0 shadow-2xl flex items-center justify-center">
          {playlist.coverImage ? (
            <img
              src={playlist.coverImage}
              alt={playlist.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <Music className="w-24 h-24 text-purple-400" />
          )}
        </div>
        
        <div className="flex-1">
          <div className="text-sm text-zinc-400 mb-2">플레이리스트</div>
          <h1 className="text-5xl font-bold text-white mb-4">
            {playlist.title}
          </h1>
          
          {playlist.description && (
            <p className="text-zinc-300 mb-4">{playlist.description}</p>
          )}

          <div className="flex items-center gap-2 text-zinc-400 mb-6">
            <span>{playlist.trackIds.length}곡</span>
            {totalMinutes > 0 && (
              <>
                <span>•</span>
                <span>{totalMinutes}분</span>
              </>
            )}
            <span>•</span>
            <span>{playlist.isPublic ? '공개' : '비공개'}</span>
          </div>

          <div className="flex items-center gap-4">
            <Button
              size="lg"
              className="rounded-full gap-2"
              onClick={handlePlayPlaylist}
              disabled={playlistTracks.length === 0}
            >
              <Play className="w-5 h-5" fill="currentColor" />
              재생
            </Button>
            <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
              <DialogTrigger asChild>
                <Button size="lg" variant="outline" className="rounded-full gap-2">
                  <Plus className="w-5 h-5" />
                  곡 추가
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-2xl">
                <DialogHeader>
                  <DialogTitle>플레이리스트에 곡 추가</DialogTitle>
                </DialogHeader>
                <div className="max-h-[420px] overflow-y-auto space-y-2">
                  {availableTracks.length > 0 ? (
                    availableTracks.map((track) => (
                      <div key={track.id} className="flex items-center gap-3 rounded-md bg-zinc-800/60 p-2">
                        <img src={track.coverImage} alt={track.title} className="w-10 h-10 rounded object-cover" />
                        <div className="min-w-0 flex-1">
                          <div className="text-white truncate">{track.title}</div>
                          <div className="text-sm text-zinc-400 truncate">{track.artistName}</div>
                        </div>
                        <Button size="sm" onClick={() => addToPlaylist(playlist.id, track.id)}>
                          추가
                        </Button>
                      </div>
                    ))
                  ) : (
                    <p className="text-zinc-400 py-8 text-center">추가할 수 있는 곡이 없습니다.</p>
                  )}
                </div>
              </DialogContent>
            </Dialog>
            <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
              <DialogTrigger asChild>
                <Button size="lg" variant="outline" className="rounded-full gap-2">
                  <Pencil className="w-5 h-5" />
                  수정
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>플레이리스트 수정</DialogTitle>
                </DialogHeader>
                <div className="space-y-4">
                  <Input value={editTitle} onChange={(event) => setEditTitle(event.target.value)} placeholder="제목" />
                  <Textarea value={editDescription} onChange={(event) => setEditDescription(event.target.value)} placeholder="설명" />
                  <Button
                    className="w-full"
                    onClick={() => {
                      updatePlaylist(playlist.id, editTitle, editDescription);
                      setIsEditOpen(false);
                    }}
                  >
                    저장
                  </Button>
                </div>
              </DialogContent>
            </Dialog>
            <Button
              size="lg"
              variant="outline"
              className="rounded-full gap-2"
              onClick={() => {
                deletePlaylist(playlist.id);
                navigate('/library');
              }}
            >
              <Trash2 className="w-5 h-5" />
              삭제
            </Button>
          </div>
        </div>
      </div>

      {/* Track List */}
      <div>
        {playlistTracks.length > 0 ? (
          <div className="space-y-2">
            {playlistTracks.map((track, index) => (
              <div key={track.id} className="flex items-center gap-2">
                <div className="flex-1">
                  <TrackList tracks={[track]} showAlbum={true} />
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-zinc-400 hover:text-red-400"
                  onClick={() => removeFromPlaylist(playlist.id, track.id)}
                >
                  <Trash2 className="w-4 h-4" />
                </Button>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-20">
            <Music className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
            <h3 className="text-xl font-medium text-white mb-2">
              플레이리스트가 비어있습니다
            </h3>
            <p className="text-zinc-400">
              좋아하는 곡을 추가해보세요
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
