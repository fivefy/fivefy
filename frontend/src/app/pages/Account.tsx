import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api, authToken } from '../lib/api';
import { useMusic } from '../context/MusicContext';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { Input } from '../components/ui/input';

export function Account() {
  const navigate = useNavigate();
  const { user, refreshData } = useMusic();
  const [name, setName] = useState(user.name);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [deletePassword, setDeletePassword] = useState('');
  const [billingKeyId, setBillingKeyId] = useState('');
  const [cashProductType, setCashProductType] = useState('POINT_1000');
  const [refundOrderNumber, setRefundOrderNumber] = useState('');
  const [wallet, setWallet] = useState<any>(null);
  const [histories, setHistories] = useState<any[]>([]);
  const [payments, setPayments] = useState<any[]>([]);
  const [playbacks, setPlaybacks] = useState<any[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  const loadAccountData = async () => {
    const results = await Promise.allSettled([
      api.wallet(),
      api.walletHistories(),
      api.payments(),
      api.playbackHistory(),
    ]);
    if (results[0].status === 'fulfilled') setWallet(results[0].value);
    if (results[1].status === 'fulfilled') setHistories(results[1].value);
    if (results[2].status === 'fulfilled') setPayments(results[2].value);
    if (results[3].status === 'fulfilled') setPlaybacks(results[3].value);
  };

  useEffect(() => {
    setName(user.name);
  }, [user.name]);

  useEffect(() => {
    void loadAccountData();
  }, []);

  const updateProfile = async () => {
    setMessage(null);
    try {
      await api.updateMe({ name, currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      await refreshData();
      setMessage('프로필을 수정했습니다.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '프로필 수정에 실패했습니다.');
    }
  };

  const logout = async () => {
    try {
      await api.logout();
    } catch {
      authToken.clear();
    }
    await refreshData();
    navigate('/login');
  };

  const deleteAccount = async () => {
    if (!deletePassword) return;
    setMessage(null);
    try {
      await api.deleteMe(deletePassword);
      await refreshData();
      navigate('/login');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '회원 탈퇴에 실패했습니다.');
    }
  };

  const registerBillingKey = async () => {
    if (!billingKeyId.trim()) return;
    setMessage(null);
    try {
      await api.registerBillingKey(billingKeyId.trim());
      setBillingKeyId('');
      setMessage('빌링키를 등록했습니다.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '빌링키 등록에 실패했습니다.');
    }
  };

  const createCashOrder = async () => {
    setMessage(null);
    try {
      const result = await api.createCashOrder(cashProductType);
      setMessage(`포인트 주문이 생성되었습니다. 주문번호: ${result.orderNumber}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '포인트 주문 생성에 실패했습니다.');
    }
  };

  const refundCashOrder = async () => {
    if (!refundOrderNumber.trim()) return;
    setMessage(null);
    try {
      await api.refundCashOrder(refundOrderNumber.trim(), '사용자 요청');
      setRefundOrderNumber('');
      setMessage('환불 요청을 처리했습니다.');
      await loadAccountData();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '환불 요청에 실패했습니다.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">계정</h1>
          <p className="text-zinc-400">{user.email}</p>
        </div>
        <Button variant="outline" onClick={logout}>로그아웃</Button>
      </div>
      {message && <div className="rounded-md bg-zinc-800 px-4 py-3 text-sm text-zinc-200">{message}</div>}

      <div className="grid lg:grid-cols-2 gap-6">
        <Card className="p-5 space-y-4">
          <h2 className="text-xl font-bold text-white">프로필</h2>
          <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="이름" />
          <Input value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} type="password" placeholder="현재 비밀번호" />
          <Input value={newPassword} onChange={(event) => setNewPassword(event.target.value)} type="password" placeholder="새 비밀번호" />
          <Button onClick={updateProfile}>저장</Button>
        </Card>

        <Card className="p-5 space-y-4">
          <h2 className="text-xl font-bold text-white">지갑</h2>
          <div className="grid grid-cols-3 gap-3">
            <div className="rounded-md bg-zinc-800 p-3">
              <div className="text-xs text-zinc-400">총 포인트</div>
              <div className="text-xl text-white">{(wallet?.totalBalance ?? user.points).toLocaleString()}P</div>
            </div>
            <div className="rounded-md bg-zinc-800 p-3">
              <div className="text-xs text-zinc-400">유료</div>
              <div className="text-xl text-white">{(wallet?.balance ?? 0).toLocaleString()}P</div>
            </div>
            <div className="rounded-md bg-zinc-800 p-3">
              <div className="text-xs text-zinc-400">이벤트</div>
              <div className="text-xl text-white">{(wallet?.eventBalance ?? 0).toLocaleString()}P</div>
            </div>
          </div>
        </Card>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        <Card className="p-5">
          <h2 className="text-xl font-bold text-white mb-4">포인트 이력</h2>
          <List rows={histories.map((item) => ({
            title: item.logDescription ?? item.pointHistoryType,
            value: `${item.amount}P`,
            meta: item.createdAt,
          }))} empty="포인트 이력이 없습니다." />
        </Card>
        <Card className="p-5">
          <h2 className="text-xl font-bold text-white mb-4">결제 내역</h2>
          <List rows={payments.map((item) => ({
            title: item.orderNumber,
            value: `${item.amount?.toLocaleString?.() ?? item.amount}원`,
            meta: item.status,
          }))} empty="결제 내역이 없습니다." />
        </Card>
        <Card className="p-5">
          <h2 className="text-xl font-bold text-white mb-4">재생 기록</h2>
          <List rows={playbacks.map((item) => ({
            title: `Track #${item.trackId}`,
            value: item.status,
            meta: item.lastPlayedAt ?? item.startedAt,
          }))} empty="재생 기록이 없습니다." />
        </Card>
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        <Card className="p-5 space-y-4">
          <h2 className="text-xl font-bold text-white">빌링키</h2>
          <Input value={billingKeyId} onChange={(event) => setBillingKeyId(event.target.value)} placeholder="PortOne billingKeyId" />
          <Button onClick={registerBillingKey}>등록</Button>
        </Card>
        <Card className="p-5 space-y-4">
          <h2 className="text-xl font-bold text-white">포인트 주문/환불</h2>
          <Input value={cashProductType} onChange={(event) => setCashProductType(event.target.value)} placeholder="상품 타입" />
          <Button onClick={createCashOrder}>주문 생성</Button>
          <Input value={refundOrderNumber} onChange={(event) => setRefundOrderNumber(event.target.value)} placeholder="환불 주문번호" />
          <Button variant="outline" onClick={refundCashOrder}>환불 요청</Button>
        </Card>
      </div>

      <Card className="p-5 space-y-4 border-red-900/60">
        <h2 className="text-xl font-bold text-white">회원 탈퇴</h2>
        <Input value={deletePassword} onChange={(event) => setDeletePassword(event.target.value)} type="password" placeholder="비밀번호 확인" />
        <Button variant="destructive" onClick={deleteAccount}>탈퇴</Button>
      </Card>
    </div>
  );
}

function List({ rows, empty }: { rows: Array<{ title: string; value?: string; meta?: string }>; empty: string }) {
  if (rows.length === 0) return <p className="text-zinc-400">{empty}</p>;
  return (
    <div className="space-y-2">
      {rows.map((row, index) => (
        <div key={`${row.title}-${index}`} className="rounded-md bg-zinc-800/60 px-3 py-2">
          <div className="flex justify-between gap-3 text-sm">
            <span className="text-zinc-200 truncate">{row.title}</span>
            {row.value && <span className="text-zinc-400">{row.value}</span>}
          </div>
          {row.meta && <div className="mt-1 text-xs text-zinc-500 truncate">{row.meta}</div>}
        </div>
      ))}
    </div>
  );
}
