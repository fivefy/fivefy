import { useParams, Link } from 'react-router';
import { TrackList } from '../components/TrackList';
import { Button } from '../components/ui/button';
import { Play, Heart } from 'lucide-react';
import { useMusic } from '../context/MusicContext';
import { useEffect, useState } from 'react';
import { Album as AlbumType, Track } from '../types';
import { api } from '../lib/api';

export function Album() {
  const { id } = useParams<{ id: string }>();
  const { albums, tracks, playTrack, toggleAlbumLike } = useMusic();
  const [remoteAlbum, setRemoteAlbum] = useState<AlbumType | null>(null);
  const [remoteTracks, setRemoteTracks] = useState<Track[]>([]);
  
  useEffect(() => {
    if (!id) return;
    api.album(id)
      .then((data) => {
        setRemoteAlbum(data.album);
        setRemoteTracks(data.tracks);
      })
      .catch(() => undefined);
  }, [id]);

  const album = remoteAlbum ?? albums.find((a) => a.id === id);

  if (!album) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-white mb-2">
          앨범을 찾을 수 없습니다
        </h2>
        <Link to="/" className="text-purple-500 hover:underline">
          홈으로 돌아가기
        </Link>
      </div>
    );
  }

  const albumTracks = remoteTracks.length > 0 ? remoteTracks : tracks.filter((track) => track.albumId === album.id);
  const totalDuration = albumTracks.reduce((sum, track) => sum + track.duration, 0);
  const totalMinutes = Math.floor(totalDuration / 60);

  const handlePlayAlbum = () => {
    if (albumTracks.length > 0) {
      void playTrack(albumTracks[0], albumTracks);
    }
  };

  return (
    <div className="space-y-8">
      {/* Album Header */}
      <div className="flex flex-col md:flex-row gap-8 items-start">
        <div className="w-full md:w-64 aspect-square rounded-lg overflow-hidden bg-zinc-800 flex-shrink-0 shadow-2xl">
          <img
            src={album.coverImage}
            alt={album.title}
            className="w-full h-full object-cover"
          />
        </div>
        
        <div className="flex-1">
          <div className="text-sm text-zinc-400 mb-2">앨범</div>
          <h1 className="text-5xl font-bold text-white mb-4">{album.title}</h1>
          
          <Link
            to={`/artist/${album.artistId}`}
            className="text-xl text-white hover:underline inline-block mb-4"
          >
            {album.artistName}
          </Link>

          <div className="flex items-center gap-2 text-zinc-400 mb-6">
            <span>{new Date(album.releaseDate).getFullYear()}</span>
            <span>•</span>
            <span>{album.trackCount}곡</span>
            <span>•</span>
            <span>{totalMinutes}분</span>
          </div>

          <div className="flex items-center gap-4">
            <Button
              size="lg"
              className="rounded-full gap-2"
              onClick={handlePlayAlbum}
            >
              <Play className="w-5 h-5" fill="currentColor" />
              재생
            </Button>
            <Button
              variant="outline"
              size="lg"
              className="rounded-full"
              onClick={() => toggleAlbumLike(album.id)}
            >
              <Heart className="w-5 h-5" />
            </Button>
          </div>
        </div>
      </div>

      {/* Track List */}
      <div>
        <TrackList tracks={albumTracks} showAlbum={false} />
      </div>
    </div>
  );
}
