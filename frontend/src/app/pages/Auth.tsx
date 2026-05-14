import { useState } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../lib/api';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { useMusic } from '../context/MusicContext';

export function Auth() {
  const navigate = useNavigate();
  const { refreshData } = useMusic();
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setError(null);
    try {
      if (mode === 'signup') {
        await api.signup(name, email, password);
      }
      await api.login(email, password);
      await refreshData();
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-md mx-auto space-y-6">
      <h1 className="text-3xl font-bold text-white">로그인</h1>
      <div className="grid grid-cols-2 rounded-lg bg-zinc-800 p-1">
        <Button variant={mode === 'login' ? 'default' : 'ghost'} onClick={() => setMode('login')}>로그인</Button>
        <Button variant={mode === 'signup' ? 'default' : 'ghost'} onClick={() => setMode('signup')}>회원가입</Button>
      </div>
      {mode === 'signup' && (
        <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="이름" />
      )}
      <Input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="email@example.com" />
      <Input value={password} onChange={(event) => setPassword(event.target.value)} type="password" placeholder="password" />
      {error && <p className="text-sm text-red-400">{error}</p>}
      <Button className="w-full" onClick={handleSubmit}>{mode === 'login' ? '로그인' : '회원가입 후 로그인'}</Button>
    </div>
  );
}
