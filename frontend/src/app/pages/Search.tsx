import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { Input } from '../components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Search as SearchIcon, X } from 'lucide-react';
import { TrackList } from '../components/TrackList';
import { useMusic } from '../context/MusicContext';
import { api } from '../lib/api';
import { Album, Artist, Track } from '../types';
import { Button } from '../components/ui/button';

export function Search() {
  const [query, setQuery] = useState('');
  const { tracks, albums, artists } = useMusic();
  const [remoteTracks, setRemoteTracks] = useState<Track[]>([]);
  const [remoteAlbums, setRemoteAlbums] = useState<Album[]>([]);
  const [remoteArtists, setRemoteArtists] = useState<Artist[]>([]);
  const [histories, setHistories] = useState<string[]>([]);

  const loadHistories = () => {
    api.searchHistories().then(setHistories).catch(() => setHistories([]));
  };

  useEffect(() => {
    loadHistories();
  }, []);

  useEffect(() => {
    const keyword = query.trim();
    if (!keyword) {
      setRemoteTracks([]);
      setRemoteAlbums([]);
      setRemoteArtists([]);
      return;
    }

    const timer = window.setTimeout(() => {
      api.search(keyword)
        .then((result) => {
          setRemoteTracks(result.tracks);
          setRemoteAlbums(result.albums);
          setRemoteArtists(result.artists);
          loadHistories();
        })
        .catch(() => {
          setRemoteTracks([]);
          setRemoteAlbums([]);
          setRemoteArtists([]);
        });
    }, 250);

    return () => window.clearTimeout(timer);
  }, [query]);

  const localTracks = tracks.filter(
    (track) =>
      track.title.toLowerCase().includes(query.toLowerCase()) ||
      track.artistName.toLowerCase().includes(query.toLowerCase()) ||
      track.albumTitle.toLowerCase().includes(query.toLowerCase())
  );

  const localAlbums = albums.filter(
    (album) =>
      album.title.toLowerCase().includes(query.toLowerCase()) ||
      album.artistName.toLowerCase().includes(query.toLowerCase())
  );

  const localArtists = artists.filter(
    (artist) =>
      artist.name.toLowerCase().includes(query.toLowerCase()) ||
      artist.genre.some((g) => g.toLowerCase().includes(query.toLowerCase()))
  );

  const filteredTracks = remoteTracks.length > 0 ? remoteTracks : localTracks;
  const filteredAlbums = remoteAlbums.length > 0 ? remoteAlbums : localAlbums;
  const filteredArtists = remoteArtists.length > 0 ? remoteArtists : localArtists;
  const hasResults = filteredTracks.length > 0 || filteredAlbums.length > 0 || filteredArtists.length > 0;

  return (
    <div className="space-y-6">
      <div className="relative">
        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-400" />
        <Input
          type="text"
          placeholder="곡, 아티스트, 앨범 검색..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="pl-10 h-12 bg-zinc-800 border-0 text-white placeholder:text-zinc-400"
        />
      </div>

      {!query && (
        <div className="space-y-8">
          {histories.length > 0 && (
            <section>
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-xl font-bold text-white">최근 검색</h2>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={async () => {
                    await api.deleteAllSearchHistories();
                    setHistories([]);
                  }}
                >
                  전체 삭제
                </Button>
              </div>
              <div className="flex flex-wrap gap-2">
                {histories.map((keyword) => (
                  <button
                    key={keyword}
                    className="inline-flex items-center gap-2 rounded-full bg-zinc-800 px-3 py-2 text-sm text-zinc-200 hover:bg-zinc-700"
                    onClick={() => setQuery(keyword)}
                  >
                    {keyword}
                    <X
                      className="h-3.5 w-3.5"
                      onClick={async (event) => {
                        event.stopPropagation();
                        await api.deleteSearchHistory(keyword);
                        setHistories((prev) => prev.filter((item) => item !== keyword));
                      }}
                    />
                  </button>
                ))}
              </div>
            </section>
          )}
          <div className="text-center py-20">
            <SearchIcon className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
            <h3 className="text-xl font-medium text-white mb-2">음악 검색</h3>
            <p className="text-zinc-400">
              좋아하는 곡, 아티스트, 앨범을 찾아보세요
            </p>
          </div>
        </div>
      )}

      {query && !hasResults && (
        <div className="text-center py-20">
          <h3 className="text-xl font-medium text-white mb-2">
            검색 결과가 없습니다
          </h3>
          <p className="text-zinc-400">
            다른 검색어를 시도해보세요
          </p>
        </div>
      )}

      {query && hasResults && (
        <Tabs defaultValue="all" className="w-full">
          <TabsList className="bg-zinc-800">
            <TabsTrigger value="all">전체</TabsTrigger>
            <TabsTrigger value="tracks">곡</TabsTrigger>
            <TabsTrigger value="albums">앨범</TabsTrigger>
            <TabsTrigger value="artists">아티스트</TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-8 mt-6">
            {filteredTracks.length > 0 && (
              <section>
                <h2 className="text-xl font-bold text-white mb-4">곡</h2>
                <TrackList tracks={filteredTracks.slice(0, 5)} />
              </section>
            )}

            {filteredAlbums.length > 0 && (
              <section>
                <h2 className="text-xl font-bold text-white mb-4">앨범</h2>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                  {filteredAlbums.slice(0, 5).map((album) => (
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
                        {album.artistName}
                      </div>
                    </Link>
                  ))}
                </div>
              </section>
            )}

            {filteredArtists.length > 0 && (
              <section>
                <h2 className="text-xl font-bold text-white mb-4">아티스트</h2>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                  {filteredArtists.slice(0, 5).map((artist) => (
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
            )}
          </TabsContent>

          <TabsContent value="tracks" className="mt-6">
            {filteredTracks.length > 0 ? (
              <TrackList tracks={filteredTracks} />
            ) : (
              <p className="text-zinc-400 text-center py-10">검색 결과가 없습니다</p>
            )}
          </TabsContent>

          <TabsContent value="albums" className="mt-6">
            {filteredAlbums.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                {filteredAlbums.map((album) => (
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
                      {album.artistName}
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <p className="text-zinc-400 text-center py-10">검색 결과가 없습니다</p>
            )}
          </TabsContent>

          <TabsContent value="artists" className="mt-6">
            {filteredArtists.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                {filteredArtists.map((artist) => (
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
            ) : (
              <p className="text-zinc-400 text-center py-10">검색 결과가 없습니다</p>
            )}
          </TabsContent>
        </Tabs>
      )}
    </div>
  );
}
