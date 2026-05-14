import { Link } from 'react-router';
import { TrendingUp, TrendingDown, Minus, Sparkles } from 'lucide-react';
import { Card } from '../components/ui/card';
import { useMusic } from '../context/MusicContext';

export function Home() {
  const { tracks, albums, artists, playTrack, playbackError } = useMusic();

  const popularTracks = tracks.slice(0, 10).map((track, index) => ({
    ...track,
    rank: index + 1,
    change: 0,
  }));
  const recommendedTracks = tracks.slice(0, 5).map((track) => ({
    ...track,
    reason: '최근 인기 트랙 기반 추천',
  }));
  const newReleases = albums.slice(0, 6);
  const trendingArtists = artists.slice(0, 5);

  return (
    <div className="space-y-8">
      {playbackError && (
        <div className="rounded-md border border-red-800/50 bg-red-950/30 px-4 py-3 text-sm text-red-200">
          {playbackError}
        </div>
      )}

      {/* AI Recommendations */}
      <section>
        <div className="flex items-center gap-2 mb-4">
          <Sparkles className="w-6 h-6 text-purple-500" />
          <h2 className="text-2xl font-bold text-white">AI 맞춤 추천</h2>
        </div>
        <div className="space-y-3">
          {recommendedTracks.map((track: any) => (
            <Card
              key={track.id}
              className="p-4 bg-gradient-to-r from-purple-900/20 to-transparent border-purple-800/30 cursor-pointer"
              onClick={() => void playTrack(track, tracks)}
            >
              <div className="flex items-center gap-4">
                <img
                  src={track.coverImage}
                  alt={track.title}
                  className="w-16 h-16 rounded object-cover"
                />
                <div className="flex-1 min-w-0">
                  <div className="text-white font-medium">{track.title}</div>
                  <div className="text-sm text-zinc-400">{track.artistName}</div>
                  <div className="text-xs text-purple-400 mt-1 flex items-center gap-1">
                    <Sparkles className="w-3 h-3" />
                    {track.reason}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* Popular Chart */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl font-bold text-white">인기 차트 TOP 100</h2>
          <Link to="/chart" className="text-sm text-zinc-400 hover:text-white">
            전체보기
          </Link>
        </div>
        <div className="space-y-1">
          {popularTracks.map((track: any) => (
            <div
              key={track.id}
              onClick={() => void playTrack(track, tracks)}
              className="group flex items-center gap-4 px-4 py-2 rounded-md hover:bg-zinc-800/50 transition-colors"
            >
              {/* Rank */}
              <div className="w-8 text-center">
                <div className="text-white font-bold text-lg">{track.rank}</div>
              </div>

              {/* Change indicator */}
              <div className="w-6 flex items-center justify-center">
                {track.change > 0 && (
                  <TrendingUp className="w-4 h-4 text-green-500" />
                )}
                {track.change < 0 && (
                  <TrendingDown className="w-4 h-4 text-red-500" />
                )}
                {track.change === 0 && (
                  <Minus className="w-4 h-4 text-zinc-500" />
                )}
              </div>

              {/* Track info */}
              <img
                src={track.coverImage}
                alt={track.title}
                className="w-12 h-12 rounded object-cover"
              />
              <div className="flex-1 min-w-0">
                <div className="text-white font-medium truncate">{track.title}</div>
                <div className="text-sm text-zinc-400 truncate">{track.artistName}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* New Releases */}
      <section>
        <h2 className="text-2xl font-bold text-white mb-4">최신 앨범</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {newReleases.map((album) => (
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
              <div className="text-sm text-zinc-400 truncate">{album.artistName}</div>
            </Link>
          ))}
        </div>
      </section>

      {/* Trending Artists */}
      <section>
        <h2 className="text-2xl font-bold text-white mb-4">인기 아티스트</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {trendingArtists.map((artist) => (
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
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
