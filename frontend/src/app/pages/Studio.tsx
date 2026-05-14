import { useEffect, useMemo, useState } from 'react';
import { FileAudio, RefreshCw, Upload } from 'lucide-react';
import { api } from '../lib/api';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Textarea } from '../components/ui/textarea';

type ApplicationRow = Record<string, any> & { source: 'artist' | 'album' | 'track' };

export function Studio() {
  const [artistName, setArtistName] = useState('');
  const [artistBio, setArtistBio] = useState('');
  const [artistType, setArtistType] = useState('SOLO');
  const [albumArtistId, setAlbumArtistId] = useState('');
  const [albumTitle, setAlbumTitle] = useState('');
  const [albumDescription, setAlbumDescription] = useState('');
  const [trackTitle, setTrackTitle] = useState('');
  const [trackGenre, setTrackGenre] = useState('');
  const [trackDuration, setTrackDuration] = useState('');
  const [trackLyrics, setTrackLyrics] = useState('');
  const [audioFile, setAudioFile] = useState<File | null>(null);
  const [officialArtistId, setOfficialArtistId] = useState('');
  const [officialAlbumId, setOfficialAlbumId] = useState('');
  const [officialTrackNumber, setOfficialTrackNumber] = useState('');
  const [officialTitle, setOfficialTitle] = useState('');
  const [officialGenre, setOfficialGenre] = useState('');
  const [officialDuration, setOfficialDuration] = useState('');
  const [officialLyrics, setOfficialLyrics] = useState('');
  const [officialFile, setOfficialFile] = useState<File | null>(null);
  const [officialAlbums, setOfficialAlbums] = useState<any[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [applications, setApplications] = useState<ApplicationRow[]>([]);
  const [myArtists, setMyArtists] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const artistApplications = useMemo(() => applications.filter((item) => item.source === 'artist'), [applications]);
  const albumApplications = useMemo(() => applications.filter((item) => item.source === 'album'), [applications]);
  const trackApplications = useMemo(() => applications.filter((item) => item.source === 'track'), [applications]);

  const refreshApplications = async () => {
    setIsLoading(true);
    const results = await Promise.allSettled([
      api.myArtistApplications(),
      api.myAlbumApplications(),
      api.myTrackApplications(),
      api.myArtists(),
    ]);
    const nextApplications: ApplicationRow[] = [];
    if (results[0].status === 'fulfilled') nextApplications.push(...results[0].value.map((item: any) => ({ ...item, source: 'artist' as const })));
    if (results[1].status === 'fulfilled') nextApplications.push(...results[1].value.map((item: any) => ({ ...item, source: 'album' as const })));
    if (results[2].status === 'fulfilled') nextApplications.push(...results[2].value.map((item: any) => ({ ...item, source: 'track' as const })));
    setApplications(nextApplications.sort((a, b) => String(b.createdAt ?? '').localeCompare(String(a.createdAt ?? ''))));
    if (results[3].status === 'fulfilled') setMyArtists(results[3].value);
    setIsLoading(false);
  };

  useEffect(() => {
    void refreshApplications();
  }, []);

  useEffect(() => {
    if (!officialArtistId) {
      setOfficialAlbums([]);
      setOfficialAlbumId('');
      return;
    }
    api.artistAlbums(officialArtistId)
      .then((albums) => {
        setOfficialAlbums(albums);
        if (!albums.some((album) => album.id === officialAlbumId)) setOfficialAlbumId('');
      })
      .catch(() => {
        setOfficialAlbums([]);
        setOfficialAlbumId('');
      });
  }, [officialArtistId]);

  const submitArtist = async () => {
    setMessage(null);
    try {
      await api.createArtistApplication({
        requestedName: artistName,
        artistType,
        bio: artistBio,
      });
      setArtistName('');
      setArtistBio('');
      setMessage('아티스트 등록 신청이 완료되었습니다.');
      await refreshApplications();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '아티스트 신청에 실패했습니다.');
    }
  };

  const submitAlbum = async () => {
    setMessage(null);
    try {
      await api.createAlbumApplication({
        artistId: Number(albumArtistId),
        title: albumTitle,
        description: albumDescription,
        publishDelayDays: 0,
      });
      setAlbumArtistId('');
      setAlbumTitle('');
      setAlbumDescription('');
      setMessage('앨범 등록 신청이 완료되었습니다.');
      await refreshApplications();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '앨범 신청에 실패했습니다.');
    }
  };

  const submitFreeTrack = async () => {
    if (!audioFile) {
      setMessage('자유 창작 MP3 파일을 선택하세요.');
      return;
    }

    setMessage(null);
    try {
      await api.createFreeTrackApplication({
        title: trackTitle,
        genre: trackGenre,
        durationSec: Number(trackDuration),
        lyrics: trackLyrics,
        audioFile,
      });
      setTrackTitle('');
      setTrackGenre('');
      setTrackDuration('');
      setTrackLyrics('');
      setAudioFile(null);
      setMessage('자유 창작 트랙 등록 신청이 완료되었습니다.');
      await refreshApplications();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '트랙 신청에 실패했습니다.');
    }
  };

  const submitOfficialTrack = async () => {
    if (!officialFile) {
      setMessage('정식 발매 MP3 파일을 선택하세요.');
      return;
    }

    setMessage(null);
    try {
      await api.createOfficialTrackApplication({
        artistId: Number(officialArtistId),
        albumId: Number(officialAlbumId),
        trackNumber: Number(officialTrackNumber),
        title: officialTitle,
        genre: officialGenre,
        durationSec: Number(officialDuration),
        lyrics: officialLyrics,
        publishDelayDays: 0,
        audioFile: officialFile,
      });
      setOfficialTrackNumber('');
      setOfficialTitle('');
      setOfficialGenre('');
      setOfficialDuration('');
      setOfficialLyrics('');
      setOfficialFile(null);
      setMessage('정식 발매 트랙 등록 신청이 완료되었습니다.');
      await refreshApplications();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '정식 발매 신청에 실패했습니다.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">스튜디오</h1>
          <p className="mt-2 text-zinc-400">아티스트 등록부터 앨범, 트랙 발매 신청까지 순서대로 진행합니다.</p>
        </div>
        <Button variant="outline" className="gap-2" onClick={() => void refreshApplications()} disabled={isLoading}>
          <RefreshCw className="h-4 w-4" />
          새로고침
        </Button>
      </div>

      {message && <div className="rounded-md bg-zinc-800 px-4 py-3 text-sm text-zinc-200">{message}</div>}

      <Tabs defaultValue="submit">
        <TabsList className="bg-zinc-800">
          <TabsTrigger value="submit">등록 신청</TabsTrigger>
          <TabsTrigger value="artists">내 아티스트</TabsTrigger>
          <TabsTrigger value="applications">내 신청 현황</TabsTrigger>
        </TabsList>

        <TabsContent value="submit" className="mt-6 space-y-6">
          <div className="grid lg:grid-cols-2 gap-6">
            <Card className="p-5 space-y-4">
              <h2 className="text-xl font-bold text-white">1. 아티스트 신청</h2>
              <Input value={artistName} onChange={(event) => setArtistName(event.target.value)} placeholder="아티스트 이름" />
              <select value={artistType} onChange={(event) => setArtistType(event.target.value)} className="w-full rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm text-white">
                <option value="SOLO">솔로</option>
                <option value="GROUP">그룹</option>
                <option value="BAND">밴드</option>
              </select>
              <Textarea value={artistBio} onChange={(event) => setArtistBio(event.target.value)} placeholder="소개" />
              <Button onClick={() => void submitArtist()} disabled={!artistName.trim()} className="w-full">아티스트 신청</Button>
            </Card>

            <Card className="p-5 space-y-4">
              <h2 className="text-xl font-bold text-white">2. 앨범 신청</h2>
              <ArtistSelect value={albumArtistId} onChange={setAlbumArtistId} artists={myArtists} placeholder="아티스트 선택" />
              <Input value={albumTitle} onChange={(event) => setAlbumTitle(event.target.value)} placeholder="앨범 제목" />
              <Textarea value={albumDescription} onChange={(event) => setAlbumDescription(event.target.value)} placeholder="앨범 설명" />
              <Button onClick={() => void submitAlbum()} disabled={!albumArtistId || !albumTitle.trim()} className="w-full">앨범 신청</Button>
            </Card>
          </div>

          <div className="grid lg:grid-cols-2 gap-6">
            <Card className="p-5 space-y-4">
              <h2 className="text-xl font-bold text-white">3-A. 자유 창작 트랙 신청</h2>
              <Input value={trackTitle} onChange={(event) => setTrackTitle(event.target.value)} placeholder="트랙 제목" />
              <Input value={trackGenre} onChange={(event) => setTrackGenre(event.target.value)} placeholder="장르" />
              <Input value={trackDuration} onChange={(event) => setTrackDuration(event.target.value)} placeholder="재생 시간(초)" />
              <Textarea value={trackLyrics} onChange={(event) => setTrackLyrics(event.target.value)} placeholder="가사" />
              <FilePicker file={audioFile} onChange={setAudioFile} />
              <Button onClick={() => void submitFreeTrack()} disabled={!trackTitle.trim() || !trackGenre.trim() || !trackDuration || !audioFile} className="w-full">
                자유 창작 트랙 신청
              </Button>
            </Card>

            <Card className="p-5 space-y-4">
              <h2 className="text-xl font-bold text-white">3-B. 정식 발매 트랙 신청</h2>
              <ArtistSelect value={officialArtistId} onChange={setOfficialArtistId} artists={myArtists} placeholder="아티스트 선택" />
              <select value={officialAlbumId} onChange={(event) => setOfficialAlbumId(event.target.value)} className="w-full rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm text-white">
                <option value="">승인된 앨범 선택</option>
                {officialAlbums.map((album) => (
                  <option key={album.id} value={album.id}>
                    {album.title} (ID {album.id})
                  </option>
                ))}
              </select>
              {officialArtistId && officialAlbums.length === 0 && (
                <p className="text-sm text-zinc-500">선택한 아티스트의 승인된 앨범이 없습니다. 먼저 앨범 신청이 승인되어야 정식 발매 트랙을 신청할 수 있습니다.</p>
              )}
              <Input value={officialTrackNumber} onChange={(event) => setOfficialTrackNumber(event.target.value)} placeholder="트랙 번호" />
              <Input value={officialTitle} onChange={(event) => setOfficialTitle(event.target.value)} placeholder="트랙 제목" />
              <Input value={officialGenre} onChange={(event) => setOfficialGenre(event.target.value)} placeholder="장르" />
              <Input value={officialDuration} onChange={(event) => setOfficialDuration(event.target.value)} placeholder="재생 시간(초)" />
              <Textarea value={officialLyrics} onChange={(event) => setOfficialLyrics(event.target.value)} placeholder="가사" />
              <FilePicker file={officialFile} onChange={setOfficialFile} />
              <Button
                onClick={() => void submitOfficialTrack()}
                disabled={!officialArtistId || !officialAlbumId || !officialTrackNumber || !officialTitle.trim() || !officialGenre.trim() || !officialDuration || !officialFile}
                className="w-full"
              >
                정식 발매 트랙 신청
              </Button>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="artists" className="mt-6">
          <Card className="p-5">
            <h2 className="text-xl font-bold text-white mb-4">내 아티스트</h2>
            {myArtists.length > 0 ? (
              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-3">
                {myArtists.map((artist) => (
                  <div key={artist.artistId} className="rounded-md bg-zinc-800/60 px-3 py-3">
                    <div className="text-zinc-200">{artist.name}</div>
                    <div className="text-sm text-zinc-400">ID {artist.artistId} · {artist.artistType}</div>
                    {artist.bio && <div className="mt-2 line-clamp-2 text-sm text-zinc-500">{artist.bio}</div>}
                  </div>
                ))}
              </div>
            ) : (
              <Empty text="승인된 내 아티스트가 없습니다. 먼저 아티스트 신청을 진행하세요." />
            )}
          </Card>
        </TabsContent>

        <TabsContent value="applications" className="mt-6 grid lg:grid-cols-3 gap-6">
          <ApplicationList title="아티스트 신청" rows={artistApplications} empty="아티스트 신청 내역이 없습니다." />
          <ApplicationList title="앨범 신청" rows={albumApplications} empty="앨범 신청 내역이 없습니다." />
          <ApplicationList title="트랙 신청" rows={trackApplications} empty="트랙 신청 내역이 없습니다." />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function ArtistSelect({ value, onChange, artists, placeholder }: { value: string; onChange: (value: string) => void; artists: any[]; placeholder: string }) {
  return (
    <select value={value} onChange={(event) => onChange(event.target.value)} className="w-full rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm text-white">
      <option value="">{placeholder}</option>
      {artists.map((artist) => (
        <option key={artist.artistId} value={artist.artistId}>
          {artist.name} (ID {artist.artistId})
        </option>
      ))}
    </select>
  );
}

function FilePicker({ file, onChange }: { file: File | null; onChange: (file: File | null) => void }) {
  return (
    <label className="flex items-center gap-2 rounded-md border border-zinc-700 px-3 py-2 text-sm text-zinc-300 cursor-pointer">
      <Upload className="w-4 h-4" />
      {file ? file.name : 'MP3 파일 선택'}
      <input className="hidden" type="file" accept="audio/mpeg,.mp3" onChange={(event) => onChange(event.target.files?.[0] ?? null)} />
    </label>
  );
}

function ApplicationList({ title, rows, empty }: { title: string; rows: ApplicationRow[]; empty: string }) {
  return (
    <Card className="p-5">
      <h2 className="text-xl font-bold text-white mb-4">{title}</h2>
      {rows.length > 0 ? (
        <div className="space-y-2">
          {rows.map((application, index) => (
            <div key={`${application.applicationId ?? index}`} className="rounded-md bg-zinc-800/60 px-3 py-3">
              <div className="flex items-center justify-between gap-3">
                <span className="truncate text-zinc-200">{application.title ?? application.requestedName ?? `신청 ${index + 1}`}</span>
                <StatusBadge status={application.status ?? application.applicationStatus ?? '-'} />
              </div>
              <div className="mt-1 text-xs text-zinc-500">
                ID {application.applicationId ?? '-'} {application.createdAt ? `· ${new Date(application.createdAt).toLocaleString('ko-KR')}` : ''}
              </div>
              {application.rejectionReason && <div className="mt-2 text-sm text-red-300">거절 사유: {application.rejectionReason}</div>}
            </div>
          ))}
        </div>
      ) : (
        <Empty text={empty} />
      )}
    </Card>
  );
}

function StatusBadge({ status }: { status: string }) {
  const tone = status === 'APPROVED' ? 'bg-green-900 text-green-200' : status === 'REJECTED' ? 'bg-red-900 text-red-200' : 'bg-zinc-700 text-zinc-200';
  return <span className={`rounded-full px-2 py-1 text-xs ${tone}`}>{status}</span>;
}

function Empty({ text }: { text: string }) {
  return (
    <div className="rounded-md border border-dashed border-zinc-700 px-4 py-8 text-center">
      <FileAudio className="mx-auto mb-3 h-8 w-8 text-zinc-600" />
      <p className="text-sm text-zinc-400">{text}</p>
    </div>
  );
}
