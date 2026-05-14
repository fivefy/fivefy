import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { AlertCircle, ListPlus, RefreshCw, Search, Wand2 } from 'lucide-react';
import { api } from '../lib/api';
import { useMusic } from '../context/MusicContext';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Textarea } from '../components/ui/textarea';

type AiTrack = {
  trackId: number;
  title: string;
  artist?: string;
  albumCoverUrl?: string;
  relevanceScore?: number;
  finalScore?: number;
  metaScore?: number;
  lyricsScore?: number;
  hasLyrics?: boolean;
};

export function Ai() {
  const { refreshData } = useMusic();
  const [recommendations, setRecommendations] = useState<AiTrack[]>([]);
  const [reasoning, setReasoning] = useState('');
  const [moodQuery, setMoodQuery] = useState('');
  const [moodMode, setMoodMode] = useState<'LYRICS' | 'BALANCED' | 'METADATA'>('BALANCED');
  const [moodTracks, setMoodTracks] = useState<AiTrack[]>([]);
  const [prompt, setPrompt] = useState('');
  const [seedTrackIds, setSeedTrackIds] = useState('');
  const [generatedTracks, setGeneratedTracks] = useState<AiTrack[]>([]);
  const [playlistTitle, setPlaylistTitle] = useState('');
  const [status, setStatus] = useState<{ area: string; message: string; tone: 'info' | 'error' } | null>(null);
  const [loading, setLoading] = useState<string | null>(null);

  const loadRecommendations = async () => {
    setStatus(null);
    setLoading('recommend');
    try {
      const data = await api.aiRecommendations(20);
      setRecommendations(data.tracks ?? []);
      setReasoning(data.reasoningHint ?? '');
      if ((data.tracks ?? []).length === 0) {
        setStatus({ area: 'recommend', message: '추천할 재생 기록이 없거나 임베딩 데이터가 비어 있습니다.', tone: 'info' });
      }
    } catch (error) {
      setStatus({ area: 'recommend', message: explainAiError(error), tone: 'error' });
    } finally {
      setLoading(null);
    }
  };

  useEffect(() => {
    void loadRecommendations();
  }, []);

  const runMoodSearch = async () => {
    if (!moodQuery.trim()) return;
    setStatus(null);
    setLoading('mood');
    try {
      const data = await api.moodSearch({ query: moodQuery, limit: 20, mode: moodMode });
      setMoodTracks(data.tracks ?? []);
      if ((data.tracks ?? []).length === 0) {
        setStatus({ area: 'mood', message: '검색 결과가 없습니다. 더 구체적인 분위기나 장르를 입력해보세요.', tone: 'info' });
      }
    } catch (error) {
      setStatus({ area: 'mood', message: explainAiError(error), tone: 'error' });
    } finally {
      setLoading(null);
    }
  };

  const generatePlaylist = async () => {
    setStatus(null);
    setLoading('generate');
    try {
      const data = await api.generatePlaylist({
        prompt,
        seedTrackIds: seedTrackIds.split(',').map((value) => Number(value.trim())).filter(Boolean),
        size: 20,
      });
      setGeneratedTracks(data.tracks ?? []);
      if ((data.tracks ?? []).length === 0) {
        setStatus({ area: 'generate', message: '생성 결과가 없습니다. 프롬프트나 시드 트랙을 바꿔보세요.', tone: 'info' });
      }
    } catch (error) {
      setStatus({ area: 'generate', message: explainAiError(error), tone: 'error' });
    } finally {
      setLoading(null);
    }
  };

  const saveGeneratedPlaylist = async () => {
    if (!playlistTitle.trim() || generatedTracks.length === 0) return;
    setStatus(null);
    setLoading('save');
    try {
      await api.createPlaylistWithTracks(
        playlistTitle.trim(),
        prompt ? `AI 생성: ${prompt}` : 'AI 생성 플레이리스트',
        generatedTracks.map((track) => String(track.trackId)),
      );
      await refreshData();
      setStatus({ area: 'generate', message: 'AI 결과를 내 플레이리스트로 저장했습니다.', tone: 'info' });
    } catch (error) {
      setStatus({ area: 'generate', message: error instanceof Error ? error.message : '플레이리스트 저장에 실패했습니다.', tone: 'error' });
    } finally {
      setLoading(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-white">AI 음악 도구</h1>
        <p className="mt-2 text-zinc-400">
          추천, 무드 검색, 플레이리스트 생성을 실제 라이브러리 작업으로 이어서 사용할 수 있습니다.
        </p>
      </div>

      {status && (
        <div className={`flex items-start gap-3 rounded-md px-4 py-3 text-sm ${status.tone === 'error' ? 'bg-red-950/50 text-red-200' : 'bg-zinc-800 text-zinc-200'}`}>
          <AlertCircle className="mt-0.5 h-4 w-4 flex-shrink-0" />
          <span>{status.message}</span>
        </div>
      )}

      <Tabs defaultValue="recommend">
        <TabsList className="bg-zinc-800">
          <TabsTrigger value="recommend">개인 추천</TabsTrigger>
          <TabsTrigger value="mood">무드 검색</TabsTrigger>
          <TabsTrigger value="generate">플레이리스트 생성</TabsTrigger>
        </TabsList>

        <TabsContent value="recommend" className="mt-6">
          <Card className="p-5 space-y-5">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-bold text-white">내 재생 기록 기반 추천</h2>
                {reasoning && <p className="mt-1 text-sm text-zinc-400">{reasoning}</p>}
              </div>
              <Button variant="outline" className="gap-2" onClick={loadRecommendations} disabled={loading === 'recommend'}>
                <RefreshCw className="h-4 w-4" />
                새로고침
              </Button>
            </div>
            <AiTrackList tracks={recommendations} empty="추천 결과가 없습니다." />
          </Card>
        </TabsContent>

        <TabsContent value="mood" className="mt-6">
          <Card className="p-5 space-y-5">
            <div>
              <h2 className="text-xl font-bold text-white">분위기로 찾기</h2>
              <p className="mt-1 text-sm text-zinc-400">예: 새벽에 집중할 때, 비 오는 날 산책, 빠른 운동용</p>
            </div>
            <div className="grid gap-3 md:grid-cols-[1fr_160px_auto]">
              <Input value={moodQuery} onChange={(event) => setMoodQuery(event.target.value)} placeholder="원하는 분위기 입력" />
              <select
                value={moodMode}
                onChange={(event) => setMoodMode(event.target.value as typeof moodMode)}
                className="rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm text-white"
              >
                <option value="BALANCED">균형</option>
                <option value="LYRICS">가사 중심</option>
                <option value="METADATA">메타데이터 중심</option>
              </select>
              <Button className="gap-2" onClick={runMoodSearch} disabled={!moodQuery.trim() || loading === 'mood'}>
                <Search className="h-4 w-4" />
                검색
              </Button>
            </div>
            <AiTrackList tracks={moodTracks} empty="검색 결과가 없습니다." />
          </Card>
        </TabsContent>

        <TabsContent value="generate" className="mt-6">
          <Card className="p-5 space-y-5">
            <div>
              <h2 className="text-xl font-bold text-white">AI 플레이리스트 생성</h2>
              <p className="mt-1 text-sm text-zinc-400">결과를 확인한 뒤 내 플레이리스트로 저장할 수 있습니다.</p>
            </div>
            <Textarea value={prompt} onChange={(event) => setPrompt(event.target.value)} placeholder="어떤 플레이리스트가 필요한지 입력" />
            <Input value={seedTrackIds} onChange={(event) => setSeedTrackIds(event.target.value)} placeholder="시드 트랙 ID, 쉼표 구분" />
            <div className="flex flex-wrap gap-2">
              <Button className="gap-2" onClick={generatePlaylist} disabled={loading === 'generate' || (!prompt.trim() && !seedTrackIds.trim())}>
                <Wand2 className="h-4 w-4" />
                생성
              </Button>
              <Input className="max-w-xs" value={playlistTitle} onChange={(event) => setPlaylistTitle(event.target.value)} placeholder="저장할 플레이리스트 이름" />
              <Button variant="outline" className="gap-2" onClick={saveGeneratedPlaylist} disabled={!playlistTitle.trim() || generatedTracks.length === 0 || loading === 'save'}>
                <ListPlus className="h-4 w-4" />
                내 플레이리스트로 저장
              </Button>
            </div>
            <AiTrackList tracks={generatedTracks} empty="생성 결과가 없습니다." />
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

function AiTrackList({ tracks, empty }: { tracks: AiTrack[]; empty: string }) {
  if (tracks.length === 0) return <p className="text-zinc-400">{empty}</p>;
  return (
    <div className="divide-y divide-zinc-800 rounded-md border border-zinc-800">
      {tracks.map((track, index) => (
        <div key={`${track.trackId}-${track.title}-${index}`} className="flex items-center gap-3 px-3 py-2">
          <div className="w-8 text-sm text-zinc-500">{index + 1}</div>
          {track.albumCoverUrl ? (
            <img src={track.albumCoverUrl} alt={track.title} className="h-11 w-11 rounded object-cover" />
          ) : (
            <div className="h-11 w-11 rounded bg-zinc-800" />
          )}
          <div className="min-w-0 flex-1">
            <Link to={`/track/${track.trackId}`} className="truncate text-sm font-medium text-white hover:underline">
              {track.title}
            </Link>
            <div className="truncate text-xs text-zinc-400">{track.artist ?? `Track #${track.trackId}`}</div>
          </div>
          <Score track={track} />
        </div>
      ))}
    </div>
  );
}

function Score({ track }: { track: AiTrack }) {
  const value = track.relevanceScore ?? track.finalScore;
  if (value == null) return null;
  return <div className="text-xs text-zinc-500">{Math.round(value * 100)}%</div>;
}

function explainAiError(error: unknown) {
  const message = error instanceof Error ? error.message : 'AI 요청에 실패했습니다.';
  if (message.includes('Connection') || message.includes('Ollama') || message.includes('refused')) {
    return `${message} 로컬 AI 모델 또는 임베딩 서비스가 실행 중인지 확인해야 합니다.`;
  }
  return message;
}
