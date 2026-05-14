import { RouterProvider } from 'react-router';
import { MusicProvider } from './context/MusicContext';
import { router } from './routes';

export default function App() {
  return (
    <MusicProvider>
      <RouterProvider router={router} />
    </MusicProvider>
  );
}
