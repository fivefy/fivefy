import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { useMusic } from '../context/MusicContext';
import { TrackList } from '../components/TrackList';

export function Chart() {
  const { tracks } = useMusic();
  const [rankedTrackIds, setRankedTrackIds] = useState<string[]>([]);

  useEffect(() => {
    api.chart()
      .then((chart) => setRankedTrackIds(chart.sort((a, b) => a.rank - b.rank).map((item) => item.trackId)))
      .catch(() => setRankedTrackIds([]));
  }, []);

  const rankedTracks = rankedTrackIds.length > 0
    ? rankedTrackIds.map((trackId) => tracks.find((track) => track.id === trackId)).filter(Boolean)
    : tracks;

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-white">인기 차트 TOP 100</h1>
      <TrackList tracks={rankedTracks as any[]} showPlayCount />
    </div>
  );
}
