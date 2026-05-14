import { useParams, Link } from 'react-router';
import { useMusic } from '../context/MusicContext';
import { Button } from '../components/ui/button';
import { TrackList } from '../components/TrackList';
import { UserPlus, UserCheck } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Album, Artist as ArtistType } from '../types';
import { api } from '../lib/api';

export function Artist() {
  const { id } = useParams<{ id: string }>();
  const { artists, tracks, albums, followedArtistIds, toggleFollowArtist } = useMusic();
  const [remoteArtist, setRemoteArtist] = useState<ArtistType | null>(null);
  const [remoteAlbums, setRemoteAlbums] = useState<Album[]>([]);
  
  useEffect(() => {
    if (!id) return;
    api.artist(id).then(setRemoteArtist).catch(() => undefined);
    api.artistAlbums(id).then(setRemoteAlbums).catch(() => setRemoteAlbums([]));
  }, [id]);

  const artist = remoteArtist ?? artists.find((a) => a.id === id);

  if (!artist) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-white mb-2">
          아티스트를 찾을 수 없습니다
        </h2>
        <Link to="/" className="text-purple-500 hover:underline">
          홈으로 돌아가기
        </Link>
      </div>
    );
  }

  const artistTracks = tracks.filter((track) => track.artistId === artist.id);
  const artistAlbums = remoteAlbums.length > 0 ? remoteAlbums : albums.filter((album) => album.artistId === artist.id);
  const isFollowing = followedArtistIds.includes(artist.id);

  return (
    <div className="space-y-8">
      {/* Artist Header */}
      <div className="flex flex-col md:flex-row gap-8 items-start">
        <div className="w-full md:w-64 aspect-square rounded-full overflow-hidden bg-zinc-800 flex-shrink-0">
          <img
            src={artist.profileImage}
            alt={artist.name}
            className="w-full h-full object-cover"
          />
        </div>
        
        <div className="flex-1">
          <div className="text-sm text-zinc-400 mb-2">아티스트</div>
          <h1 className="text-5xl font-bold text-white mb-4">{artist.name}</h1>
          
          <div className="flex items-center gap-4 mb-6">
            <div className="text-zinc-400">
              팔로워 {(artist.followers / 1000000).toFixed(1)}M
            </div>
            <div className="text-zinc-400">•</div>
            <div className="text-zinc-400">
              {artist.genre.join(', ')}
            </div>
          </div>

          {artist.bio && (
            <p className="text-zinc-300 mb-6 max-w-2xl">{artist.bio}</p>
          )}

          <Button
            onClick={() => toggleFollowArtist(artist.id)}
            variant={isFollowing ? 'outline' : 'default'}
            className="gap-2"
          >
            {isFollowing ? (
              <>
                <UserCheck className="w-4 h-4" />
                팔로잉
              </>
            ) : (
              <>
                <UserPlus className="w-4 h-4" />
                팔로우
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Popular Tracks */}
      <section>
        <h2 className="text-2xl font-bold text-white mb-4">인기곡</h2>
        <TrackList tracks={artistTracks.slice(0, 5)} showAlbum={true} />
      </section>

      {/* Albums */}
      <section>
        <h2 className="text-2xl font-bold text-white mb-4">앨범</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {artistAlbums.map((album) => (
            <Link
              key={album.id}
              to={`/album/${album.id}`}
              className="group"
            >
              <div className="relative aspect-square rounded-lg overflow-hidden mb-3 bg-zinc-800">
                <img
                  src={album.coverImage}
                  alt={album.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                />
              </div>
              <div className="text-white font-medium truncate group-hover:underline">
                {album.title}
              </div>
              <div className="text-sm text-zinc-400 truncate">
                {new Date(album.releaseDate).getFullYear()} • {album.trackCount}곡
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* All Tracks */}
      <section>
        <h2 className="text-2xl font-bold text-white mb-4">모든 곡</h2>
        <TrackList tracks={artistTracks} showAlbum={true} />
      </section>
    </div>
  );
}
