import { Outlet } from 'react-router';
import { Sidebar } from '../components/Sidebar';
import { MusicPlayer } from '../components/MusicPlayer';
import { ScrollArea } from '../components/ui/scroll-area';
import { useMusic } from '../context/MusicContext';

export function Root() {
  const { actionMessage, playbackError } = useMusic();

  return (
    <div className="h-screen flex flex-col bg-zinc-900">
      <div className="flex-1 flex overflow-hidden">
        {/* Sidebar */}
        <Sidebar />

        {/* Main Content */}
        <ScrollArea className="flex-1">
          <div className="p-8 pb-32">
            <Outlet />
          </div>
        </ScrollArea>
      </div>

      {(actionMessage || playbackError) && (
        <div className="fixed right-6 bottom-28 z-50 max-w-sm rounded-md border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white shadow-xl">
          {actionMessage ?? playbackError}
        </div>
      )}

      {/* Music Player */}
      <MusicPlayer />
    </div>
  );
}
