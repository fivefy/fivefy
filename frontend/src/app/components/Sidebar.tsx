import { Link, useLocation } from 'react-router';
import { BarChart3, Bell, CreditCard, Home, Library, ListMusic, LogIn, Plus, Search, Shield, Sparkles, Upload, User } from 'lucide-react';
import { useState } from 'react';
import { useMusic } from '../context/MusicContext';
import { Button } from './ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from './ui/dialog';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';

const navGroups = [
  {
    title: '탐색',
    items: [
      { icon: Home, label: '홈', path: '/' },
      { icon: Search, label: '검색', path: '/search' },
      { icon: BarChart3, label: '차트', path: '/chart' },
      { icon: Sparkles, label: 'AI', path: '/ai' },
    ],
  },
  {
    title: '내 활동',
    items: [
      { icon: Library, label: '보관함', path: '/library' },
      { icon: Bell, label: '알림', path: '/notifications' },
      { icon: Upload, label: '스튜디오', path: '/studio' },
      { icon: CreditCard, label: '구독/결제', path: '/subscription' },
      { icon: User, label: '계정', path: '/account' },
    ],
  },
  {
    title: '운영',
    items: [
      { icon: Shield, label: '관리자 승인', path: '/admin' },
    ],
  },
];

export function Sidebar() {
  const location = useLocation();
  const { userPlaylists, createPlaylist, isAuthenticated } = useMusic();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newPlaylistTitle, setNewPlaylistTitle] = useState('');
  const [newPlaylistDesc, setNewPlaylistDesc] = useState('');

  const handleCreatePlaylist = () => {
    if (!newPlaylistTitle.trim()) return;
    createPlaylist(newPlaylistTitle, newPlaylistDesc);
    setNewPlaylistTitle('');
    setNewPlaylistDesc('');
    setIsCreateOpen(false);
  };

  return (
    <aside className="w-64 bg-black h-full flex flex-col">
      <div className="px-6 py-5">
        <h1 className="text-2xl font-bold text-white">Fivefy</h1>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 pb-6">
        <div className="space-y-6">
          {navGroups.map((group) => (
            <div key={group.title}>
              <div className="px-3 pb-2 text-xs font-semibold uppercase tracking-wide text-zinc-500">{group.title}</div>
              <div className="space-y-1">
                {group.items.map((item) => {
                  const Icon = item.icon;
                  const isActive = item.path === '/' ? location.pathname === '/' : location.pathname.startsWith(item.path);
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`flex items-center gap-3 px-3 py-2.5 rounded-md transition-colors ${
                        isActive ? 'bg-zinc-800 text-white' : 'text-zinc-400 hover:text-white hover:bg-zinc-900'
                      }`}
                    >
                      <Icon className="w-5 h-5" />
                      <span className="text-sm font-medium">{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}

          {!isAuthenticated && (
            <Link to="/login" className="flex items-center gap-3 px-3 py-2.5 rounded-md bg-zinc-900 text-zinc-200 hover:bg-zinc-800">
              <LogIn className="w-5 h-5" />
              <span className="text-sm font-medium">로그인</span>
            </Link>
          )}

          <div>
            <div className="flex items-center justify-between px-3 pb-2">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-zinc-500">플레이리스트</h3>
              <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
                <DialogTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-zinc-400 hover:text-white">
                    <Plus className="w-4 h-4" />
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>새 플레이리스트 만들기</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4 py-4">
                    <Input value={newPlaylistTitle} onChange={(event) => setNewPlaylistTitle(event.target.value)} placeholder="플레이리스트 제목" />
                    <Textarea value={newPlaylistDesc} onChange={(event) => setNewPlaylistDesc(event.target.value)} placeholder="설명" />
                    <Button onClick={handleCreatePlaylist} disabled={!newPlaylistTitle.trim()} className="w-full">
                      만들기
                    </Button>
                  </div>
                </DialogContent>
              </Dialog>
            </div>

            <div className="space-y-1">
              {userPlaylists.length > 0 ? (
                userPlaylists.map((playlist) => {
                  const isActive = location.pathname === `/playlist/${playlist.id}`;
                  return (
                    <Link
                      key={playlist.id}
                      to={`/playlist/${playlist.id}`}
                      className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                        isActive ? 'bg-zinc-800 text-white' : 'text-zinc-400 hover:text-white hover:bg-zinc-900'
                      }`}
                    >
                      <ListMusic className="w-4 h-4 flex-shrink-0" />
                      <span className="text-sm truncate">{playlist.title}</span>
                    </Link>
                  );
                })
              ) : (
                <div className="px-3 py-2 text-sm text-zinc-600">플레이리스트 없음</div>
              )}
            </div>
          </div>
        </div>
      </nav>
    </aside>
  );
}
