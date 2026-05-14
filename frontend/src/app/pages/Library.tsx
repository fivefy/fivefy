import { Link } from 'react-router';
import { useMusic } from '../context/MusicContext';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { TrackList } from '../components/TrackList';
import { Heart, Music, Users } from 'lucide-react';

export function Library() {
  const { tracks, artists, likedTrackIds, followedArtistIds, userPlaylists } = useMusic();

  const likedTracks = tracks.filter((track) =>
    likedTrackIds.includes(track.id)
  );

  const followedArtists = artists.filter((artist) =>
    followedArtistIds.includes(artist.id)
  );

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-white">보관함</h1>

      <Tabs defaultValue="liked" className="w-full">
        <TabsList className="bg-zinc-800">
          <TabsTrigger value="liked">
            <Heart className="w-4 h-4 mr-2" />
            좋아요한 곡
          </TabsTrigger>
          <TabsTrigger value="playlists">
            <Music className="w-4 h-4 mr-2" />
            플레이리스트
          </TabsTrigger>
          <TabsTrigger value="artists">
            <Users className="w-4 h-4 mr-2" />
            팔로우한 아티스트
          </TabsTrigger>
        </TabsList>

        <TabsContent value="liked" className="mt-6">
          {likedTracks.length > 0 ? (
            <div>
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-white mb-2">
                  좋아요한 곡
                </h2>
                <p className="text-zinc-400">{likedTracks.length}곡</p>
              </div>
              <TrackList tracks={likedTracks} />
            </div>
          ) : (
            <div className="text-center py-20">
              <Heart className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
              <h3 className="text-xl font-medium text-white mb-2">
                좋아요한 곡이 없습니다
              </h3>
              <p className="text-zinc-400">
                마음에 드는 곡에 좋아요를 눌러보세요
              </p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="playlists" className="mt-6">
          {userPlaylists.length > 0 ? (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {userPlaylists.map((playlist) => (
                <Link
                  key={playlist.id}
                  to={`/playlist/${playlist.id}`}
                  className="group bg-zinc-800/50 hover:bg-zinc-800 rounded-lg p-4 transition-colors"
                >
                  <div className="relative aspect-square rounded-md overflow-hidden mb-4 bg-zinc-700 flex items-center justify-center">
                    {playlist.coverImage ? (
                      <img
                        src={playlist.coverImage}
                        alt={playlist.title}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <Music className="w-12 h-12 text-zinc-500" />
                    )}
                  </div>
                  <div className="text-white font-medium truncate group-hover:underline">
                    {playlist.title}
                  </div>
                  <div className="text-sm text-zinc-400 truncate">
                    {playlist.trackIds.length}곡
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="text-center py-20">
              <Music className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
              <h3 className="text-xl font-medium text-white mb-2">
                플레이리스트가 없습니다
              </h3>
              <p className="text-zinc-400">
                사이드바에서 새 플레이리스트를 만들어보세요
              </p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="artists" className="mt-6">
          {followedArtists.length > 0 ? (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              {followedArtists.map((artist) => (
                <Link
                  key={artist.id}
                  to={`/artist/${artist.id}`}
                  className="group"
                >
                  <div className="relative aspect-square rounded-full overflow-hidden mb-3 bg-zinc-800">
                    <img
                      src={artist.profileImage}
                      alt={artist.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    />
                  </div>
                  <div className="text-white font-medium text-center truncate group-hover:underline">
                    {artist.name}
                  </div>
                  <div className="text-sm text-zinc-400 text-center truncate">
                    {artist.genre.join(', ')}
                  </div>
                  <div className="text-xs text-zinc-500 text-center mt-1">
                    팔로워 {(artist.followers / 1000000).toFixed(1)}M
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="text-center py-20">
              <Users className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
              <h3 className="text-xl font-medium text-white mb-2">
                팔로우한 아티스트가 없습니다
              </h3>
              <p className="text-zinc-400">
                좋아하는 아티스트를 팔로우해보세요
              </p>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
