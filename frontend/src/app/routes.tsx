import { createBrowserRouter } from 'react-router';
import { Root } from './pages/Root';
import { Home } from './pages/Home';
import { Search } from './pages/Search';
import { Library } from './pages/Library';
import { Subscription } from './pages/Subscription';
import { Artist } from './pages/Artist';
import { Album } from './pages/Album';
import { Playlist } from './pages/Playlist';
import { TrackDetail } from './pages/TrackDetail';
import { Chart } from './pages/Chart';
import { Auth } from './pages/Auth';
import { Notifications } from './pages/Notifications';
import { Studio } from './pages/Studio';
import { Account } from './pages/Account';
import { Ai } from './pages/Ai';
import { Admin } from './pages/Admin';

export const router = createBrowserRouter([
  {
    path: '/',
    Component: Root,
    children: [
      { index: true, Component: Home },
      { path: 'search', Component: Search },
      { path: 'library', Component: Library },
      { path: 'subscription', Component: Subscription },
      { path: 'chart', Component: Chart },
      { path: 'login', Component: Auth },
      { path: 'notifications', Component: Notifications },
      { path: 'studio', Component: Studio },
      { path: 'account', Component: Account },
      { path: 'ai', Component: Ai },
      { path: 'admin', Component: Admin },
      { path: 'track/:id', Component: TrackDetail },
      { path: 'artist/:id', Component: Artist },
      { path: 'album/:id', Component: Album },
      { path: 'playlist/:id', Component: Playlist },
    ],
  },
]);
