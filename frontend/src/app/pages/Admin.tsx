import { useEffect, useMemo, useState } from 'react';
import { Check, RefreshCw, X } from 'lucide-react';
import { api } from '../lib/api';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Textarea } from '../components/ui/textarea';

type ApplicationGroup = 'artist' | 'album' | 'track';
type ApplicationRow = Record<string, any> & { group: ApplicationGroup };

const groupLabels: Record<ApplicationGroup, string> = {
  artist: '아티스트',
  album: '앨범',
  track: '트랙',
};

export function Admin() {
  const [rows, setRows] = useState<ApplicationRow[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [selected, setSelected] = useState<ApplicationRow | null>(null);
  const [rejectReasonById, setRejectReasonById] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const pendingRows = useMemo(() => rows.filter((row) => row.status === 'PENDING'), [rows]);
  const grouped = useMemo(() => ({
    artist: rows.filter((row) => row.group === 'artist'),
    album: rows.filter((row) => row.group === 'album'),
    track: rows.filter((row) => row.group === 'track'),
  }), [rows]);

  const load = async () => {
    setIsLoading(true);
    setMessage(null);
    const status = statusFilter || undefined;
    const results = await Promise.allSettled([
      api.adminArtistApplications(status),
      api.adminAlbumApplications(status),
      api.adminTrackApplications(status),
    ]);
    const nextRows: ApplicationRow[] = [];
    if (results[0].status === 'fulfilled') nextRows.push(...results[0].value.map((row: any) => ({ ...row, group: 'artist' as const })));
    if (results[1].status === 'fulfilled') nextRows.push(...results[1].value.map((row: any) => ({ ...row, group: 'album' as const })));
    if (results[2].status === 'fulfilled') nextRows.push(...results[2].value.map((row: any) => ({ ...row, group: 'track' as const })));
    setRows(nextRows.sort((a, b) => String(b.createdAt ?? '').localeCompare(String(a.createdAt ?? ''))));
    setIsLoading(false);

    const rejected = results.filter((result) => result.status === 'rejected');
    if (rejected.length > 0) {
      setMessage('일부 관리자 API 호출에 실패했습니다. 관리자 권한 또는 로그인 상태를 확인하세요.');
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const approve = async (row: ApplicationRow) => {
    setMessage(null);
    try {
      if (row.group === 'artist') await api.approveArtistApplication(String(row.applicationId));
      if (row.group === 'album') await api.approveAlbumApplication(String(row.applicationId));
      if (row.group === 'track') await api.approveTrackApplication(String(row.applicationId));
      setSelected(null);
      setMessage(`${groupLabels[row.group]} 신청을 승인했습니다.`);
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '승인 처리에 실패했습니다.');
    }
  };

  const reject = async (row: ApplicationRow) => {
    const reason = rejectReasonById[rowKey(row)]?.trim();
    if (!reason) {
      setMessage('거절 사유를 입력하세요.');
      return;
    }
    setMessage(null);
    try {
      if (row.group === 'artist') await api.rejectArtistApplication(String(row.applicationId), reason);
      if (row.group === 'album') await api.rejectAlbumApplication(String(row.applicationId), reason);
      if (row.group === 'track') await api.rejectTrackApplication(String(row.applicationId), reason);
      setSelected(null);
      setMessage(`${groupLabels[row.group]} 신청을 거절했습니다.`);
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '거절 처리에 실패했습니다.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">관리자 승인</h1>
          <p className="mt-2 text-zinc-400">등록 요청을 유형별로 확인하고 승인 또는 거절합니다.</p>
        </div>
        <Button variant="outline" className="gap-2" onClick={() => void load()} disabled={isLoading}>
          <RefreshCw className="h-4 w-4" />
          새로고침
        </Button>
      </div>

      {message && <div className="rounded-md bg-zinc-800 px-4 py-3 text-sm text-zinc-200">{message}</div>}

      <div className="grid gap-3 md:grid-cols-4">
        <Metric label="승인 대기" value={pendingRows.length} />
        <Metric label="아티스트" value={grouped.artist.length} />
        <Metric label="앨범" value={grouped.album.length} />
        <Metric label="트랙" value={grouped.track.length} />
      </div>

      <Card className="p-4">
        <div className="flex flex-wrap items-center gap-3">
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="rounded-md border border-zinc-700 bg-zinc-900 px-3 py-2 text-sm text-white">
            <option value="">전체 상태</option>
            <option value="PENDING">승인 대기</option>
            <option value="APPROVED">승인됨</option>
            <option value="REJECTED">거절됨</option>
          </select>
          <Button variant="outline" onClick={() => void load()}>필터 적용</Button>
        </div>
      </Card>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
        <Tabs defaultValue="pending">
          <TabsList className="bg-zinc-800">
            <TabsTrigger value="pending">승인 대기</TabsTrigger>
            <TabsTrigger value="artist">아티스트</TabsTrigger>
            <TabsTrigger value="album">앨범</TabsTrigger>
            <TabsTrigger value="track">트랙</TabsTrigger>
          </TabsList>
          <TabsContent value="pending" className="mt-6">
            <ApplicationTable rows={pendingRows} selected={selected} onSelect={setSelected} />
          </TabsContent>
          <TabsContent value="artist" className="mt-6">
            <ApplicationTable rows={grouped.artist} selected={selected} onSelect={setSelected} />
          </TabsContent>
          <TabsContent value="album" className="mt-6">
            <ApplicationTable rows={grouped.album} selected={selected} onSelect={setSelected} />
          </TabsContent>
          <TabsContent value="track" className="mt-6">
            <ApplicationTable rows={grouped.track} selected={selected} onSelect={setSelected} />
          </TabsContent>
        </Tabs>

        <ReviewPanel
          row={selected}
          reason={selected ? rejectReasonById[rowKey(selected)] ?? '' : ''}
          onReasonChange={(reason) => {
            if (!selected) return;
            setRejectReasonById((prev) => ({ ...prev, [rowKey(selected)]: reason }));
          }}
          onApprove={approve}
          onReject={reject}
        />
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <Card className="p-4">
      <div className="text-sm text-zinc-400">{label}</div>
      <div className="mt-1 text-2xl font-bold text-white">{value}</div>
    </Card>
  );
}

function ApplicationTable({ rows, selected, onSelect }: { rows: ApplicationRow[]; selected: ApplicationRow | null; onSelect: (row: ApplicationRow) => void }) {
  if (rows.length === 0) {
    return <div className="rounded-md border border-dashed border-zinc-700 py-16 text-center text-zinc-400">표시할 신청이 없습니다.</div>;
  }

  return (
    <div className="overflow-hidden rounded-md border border-zinc-800">
      <div className="grid grid-cols-[120px_1fr_120px_160px] bg-zinc-900 px-4 py-2 text-xs text-zinc-400">
        <div>유형</div>
        <div>제목</div>
        <div>상태</div>
        <div>생성일</div>
      </div>
      {rows.map((row) => (
        <button
          key={rowKey(row)}
          className={`grid w-full grid-cols-[120px_1fr_120px_160px] px-4 py-3 text-left text-sm hover:bg-zinc-800/60 ${selected && rowKey(selected) === rowKey(row) ? 'bg-zinc-800' : 'bg-zinc-950'}`}
          onClick={() => onSelect(row)}
        >
          <div className="text-zinc-300">{groupLabels[row.group]}</div>
          <div className="truncate text-white">{rowTitle(row)}</div>
          <div><StatusBadge status={row.status ?? '-'} /></div>
          <div className="truncate text-zinc-500">{row.createdAt ? new Date(row.createdAt).toLocaleDateString('ko-KR') : '-'}</div>
        </button>
      ))}
    </div>
  );
}

function ReviewPanel({ row, reason, onReasonChange, onApprove, onReject }: {
  row: ApplicationRow | null;
  reason: string;
  onReasonChange: (reason: string) => void;
  onApprove: (row: ApplicationRow) => void;
  onReject: (row: ApplicationRow) => void;
}) {
  if (!row) {
    return (
      <Card className="p-5">
        <h2 className="text-lg font-bold text-white">검토 상세</h2>
        <p className="mt-3 text-sm text-zinc-400">왼쪽 목록에서 신청을 선택하세요.</p>
      </Card>
    );
  }

  const canReview = row.status === 'PENDING';
  return (
    <Card className="p-5 space-y-4">
      <div>
        <div className="text-sm text-zinc-400">{groupLabels[row.group]} 신청 #{row.applicationId}</div>
        <h2 className="mt-1 text-xl font-bold text-white">{rowTitle(row)}</h2>
      </div>
      <dl className="space-y-2 text-sm">
        <Detail label="상태" value={row.status} />
        <Detail label="요청자" value={row.requesterUserId} />
        <Detail label="아티스트 ID" value={row.artistId} />
        <Detail label="유형" value={row.artistType ?? row.trackType} />
        <Detail label="생성일" value={row.createdAt ? new Date(row.createdAt).toLocaleString('ko-KR') : undefined} />
      </dl>
      {canReview && (
        <>
          <Textarea value={reason} onChange={(event) => onReasonChange(event.target.value)} placeholder="거절 사유" />
          <div className="flex gap-2">
            <Button className="gap-2" onClick={() => onApprove(row)}>
              <Check className="h-4 w-4" />
              승인
            </Button>
            <Button variant="outline" className="gap-2" onClick={() => onReject(row)}>
              <X className="h-4 w-4" />
              거절
            </Button>
          </div>
        </>
      )}
    </Card>
  );
}

function Detail({ label, value }: { label: string; value?: string | number }) {
  if (value == null || value === '') return null;
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-zinc-500">{label}</dt>
      <dd className="truncate text-zinc-200">{value}</dd>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const tone = status === 'APPROVED' ? 'bg-green-900 text-green-200' : status === 'REJECTED' ? 'bg-red-900 text-red-200' : 'bg-zinc-700 text-zinc-200';
  return <span className={`rounded-full px-2 py-1 text-xs ${tone}`}>{status}</span>;
}

function rowKey(row: ApplicationRow) {
  return `${row.group}-${row.applicationId}`;
}

function rowTitle(row: ApplicationRow) {
  return row.requestedName ?? row.title ?? `${groupLabels[row.group]} 신청`;
}
