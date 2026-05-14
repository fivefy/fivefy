import { Album, Artist, Comment, Notification, Playlist, PopularChart, Subscription, Track, User } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const ACCESS_TOKEN_KEY = 'fivefy.accessToken';
const REFRESH_TOKEN_KEY = 'fivefy.refreshToken';
const DEFAULT_COVER_IMAGE =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZyI+PHJlY3Qgd2lkdGg9IjIwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMyNzI3MmEiLz48Y2lyY2xlIGN4PSIxMDAiIGN5PSIxMDAiIHI9IjU2IiBmaWxsPSIjM2YzZjQ2Ii8+PHBhdGggZD0iTTg0IDcydjU2bDUyLTI4LTUyLTI4eiIgZmlsbD0iI2E3YTdjMiIvPjwvc3ZnPg==';

type BaseResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type PublicTrackResponse = {
  trackId: number;
  title: string;
  artistId?: number;
  artistName?: string;
  albumId?: number;
  albumTitle?: string;
  durationSec?: number;
  playCount?: number;
};

type TrackDetailResponse = PublicTrackResponse & {
  lyrics?: string;
  genre?: string;
  comments?: TrackCommentResponse[];
};

type AlbumDetailResponse = {
  albumId: number;
  artistId: number;
  artistName: string;
  title: string;
  description?: string;
  coverImageUrl?: string;
  trackCount?: number;
  publishedAt?: string;
  tracks?: Array<{ trackId: number; trackNumber?: number; title: string; durationSec?: number }>;
};

type ArtistResponse = {
  artistId: number;
  id?: number;
  name: string;
  artistType?: string;
  bio?: string;
  profileImageUrl?: string;
};

type PlaylistResponse = {
  id: number;
  userId: number;
  title: string;
  description?: string;
  createdAt?: string;
};

type PlaylistTrackResponse = {
  trackId: number;
  position: number;
};

type LikeResponse = {
  id: number;
  targetId: number;
  targetType: 'TRACK' | 'ALBUM' | 'PLAYLIST' | 'ARTIST';
};

type FollowResponse = {
  id: number;
  artistId: number;
};

type TrackCommentResponse = {
  commentId: number;
  userId: number;
  trackId: number;
  content: string;
  createdAt: string;
};

type PlayTrackResponse = {
  trackId: number;
  audioUrl: string;
  playCount: number;
};

type SubscriptionResponse = {
  id: number;
  planType: string;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  startDate: string;
  expiryDate: string;
};

type WalletResponse = {
  walletId?: number;
  eventBalance?: number;
  totalBalance?: number;
  balance?: number;
};

type PlaybackResponse = {
  id: number;
  playlistId?: number;
  trackId: number;
  status: string;
  playedDuration?: number;
  startedAt?: string;
  lastPlayedAt?: string;
  endedAt?: string;
  audioUrl?: string;
};

type AiTrackResponse = {
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

export const authToken = {
  get() {
    return localStorage.getItem(ACCESS_TOKEN_KEY) ?? import.meta.env.VITE_ACCESS_TOKEN ?? '';
  },
  set(accessToken: string, refreshToken?: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
  },
  clear() {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = authToken.get();
  const headers = new Headers(options.headers);

  if (!(options.body instanceof FormData) && options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(payload?.message ?? `HTTP ${response.status}`);
  }

  if (payload && 'success' in payload && payload.success === false) {
    throw new Error(payload.message ?? '요청에 실패했습니다.');
  }

  return (payload && 'data' in payload ? (payload as BaseResponse<T>).data : payload) as T;
}

const toId = (value?: string | number | null) => (value == null ? '' : String(value));

function mapTrack(track: PublicTrackResponse | TrackDetailResponse): Track {
  return {
    id: toId(track.trackId),
    title: track.title,
    artistId: toId(track.artistId),
    artistName: track.artistName ?? 'Unknown Artist',
    albumId: toId(track.albumId),
    albumTitle: track.albumTitle ?? 'Single',
    coverImage: DEFAULT_COVER_IMAGE,
    duration: track.durationSec ?? 0,
    playCount: track.playCount ?? 0,
    lyrics: 'lyrics' in track ? track.lyrics : undefined,
  };
}

function mapAlbum(album: AlbumDetailResponse): Album {
  return {
    id: toId(album.albumId),
    title: album.title,
    coverImage: album.coverImageUrl ?? DEFAULT_COVER_IMAGE,
    artistId: toId(album.artistId),
    artistName: album.artistName,
    releaseDate: album.publishedAt ?? '',
    trackCount: album.trackCount ?? album.tracks?.length ?? 0,
  };
}

function mapArtist(artist: ArtistResponse): Artist {
  return {
    id: toId(artist.artistId ?? artist.id),
    name: artist.name,
    profileImage: artist.profileImageUrl ?? DEFAULT_COVER_IMAGE,
    genre: artist.artistType ? [artist.artistType] : [],
    followers: 0,
    bio: artist.bio,
  };
}

function mapPlaylist(playlist: PlaylistResponse, trackIds: string[] = []): Playlist {
  return {
    id: toId(playlist.id),
    title: playlist.title,
    description: playlist.description,
    userId: toId(playlist.userId),
    trackIds,
    isPublic: true,
    createdAt: playlist.createdAt ?? '',
  };
}

export const api = {
  async login(email: string, password: string) {
    const data = await request<{ accessToken: string; refreshToken?: string }>('/api/users/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    authToken.set(data.accessToken, data.refreshToken);
    return data;
  },

  async signup(name: string, email: string, password: string) {
    return request('/api/users/signup', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    });
  },

  async me(): Promise<User> {
    const data = await request<{ email: string; name: string; status?: string }>('/api/users/me');
    let points = 0;
    try {
      const wallet = await request<WalletResponse>('/api/me/wallets');
      points = wallet.totalBalance ?? wallet.balance ?? 0;
    } catch {
      // 지갑이 아직 없거나 비로그인 상태면 0P로 표시한다.
    }

    return {
      id: data.email,
      email: data.email,
      name: data.name,
      subscriptionStatus: 'NONE',
      points,
    };
  },

  async updateMe(input: { name?: string; currentPassword?: string; newPassword?: string }) {
    const passwordChange = input.currentPassword && input.newPassword
      ? { currentPassword: input.currentPassword, newPassword: input.newPassword }
      : undefined;
    return request<{ name: string; updatedAt: string }>('/api/users/me', {
      method: 'PATCH',
      body: JSON.stringify({ name: input.name || undefined, passwordChange }),
    });
  },

  async logout() {
    await request('/api/users/logout', { method: 'POST' });
    authToken.clear();
  },

  async deleteMe(password: string) {
    await request('/api/users/me', {
      method: 'DELETE',
      body: JSON.stringify({ password }),
    });
    authToken.clear();
  },

  async tracks(page = 0, size = 50): Promise<Track[]> {
    const data = await request<PageResponse<PublicTrackResponse>>(`/api/tracks?page=${page}&size=${size}`);
    return data.content.map(mapTrack);
  },

  async track(trackId: string): Promise<Track> {
    return mapTrack(await request<TrackDetailResponse>(`/api/tracks/${trackId}`));
  },

  async playTrack(trackId: string): Promise<PlayTrackResponse> {
    return request<PlayTrackResponse>(`/api/tracks/${trackId}/play`, { method: 'POST' });
  },

  async search(query: string) {
    const data = await request<{
      artists: ArtistResponse[];
      tracks: PageResponse<{ id: number; title: string; genre?: string; durationSec?: number }>;
      albums: PageResponse<{ id: number; title: string; coverImageUrl?: string }>;
    }>(`/api/search?keyword=${encodeURIComponent(query)}&page=0&size=30`);

    return {
      tracks: data.tracks.content.map((track) => mapTrack({
        trackId: track.id,
        title: track.title,
        artistName: '',
        albumTitle: '',
        durationSec: track.durationSec,
      })),
      albums: data.albums.content.map((album) => ({
        id: toId(album.id),
        title: album.title,
        coverImage: album.coverImageUrl ?? DEFAULT_COVER_IMAGE,
        artistId: '',
        artistName: '',
        releaseDate: '',
        trackCount: 0,
      })),
      artists: data.artists.map(mapArtist),
    };
  },

  async searchHistories(): Promise<string[]> {
    return request<string[]>('/api/search-histories');
  },

  async deleteSearchHistory(keyword: string) {
    await request(`/api/search-histories/recent?keyword=${encodeURIComponent(keyword)}`, { method: 'DELETE' });
  },

  async deleteAllSearchHistories() {
    await request('/api/search-histories', { method: 'DELETE' });
  },

  async artist(artistId: string): Promise<Artist> {
    return mapArtist(await request<ArtistResponse>(`/api/artists/${artistId}`));
  },

  async artistAlbums(artistId: string): Promise<Album[]> {
    const data = await request<Array<{ albumId: number; title: string; coverImageUrl?: string; trackCount?: number }>>(
      `/api/artists/${artistId}/albums`,
    );
    return data.map((album) => ({
      id: toId(album.albumId),
      title: album.title,
      coverImage: album.coverImageUrl,
      artistId,
      artistName: '',
      releaseDate: '',
      trackCount: album.trackCount ?? 0,
    }));
  },

  async album(albumId: string) {
    const data = await request<AlbumDetailResponse>(`/api/albums/${albumId}`);
    return {
      album: mapAlbum(data),
      tracks: (data.tracks ?? []).map((track) =>
        mapTrack({
          trackId: track.trackId,
          title: track.title,
          artistId: data.artistId,
          artistName: data.artistName,
          albumId: data.albumId,
          albumTitle: data.title,
          durationSec: track.durationSec,
          playCount: 0,
        }),
      ),
    };
  },

  async playlists(): Promise<Playlist[]> {
    const playlists = await request<PlaylistResponse[]>('/api/playlists');
    return Promise.all(
      playlists.map(async (playlist) => {
        try {
          const tracks = await request<PlaylistTrackResponse[]>(`/api/playlists/${playlist.id}/tracks`);
          return mapPlaylist(playlist, tracks.sort((a, b) => a.position - b.position).map((track) => toId(track.trackId)));
        } catch {
          return mapPlaylist(playlist);
        }
      }),
    );
  },

  async createPlaylist(title: string, description?: string) {
    return mapPlaylist(await request<PlaylistResponse>('/api/playlists', {
      method: 'POST',
      body: JSON.stringify({ title, description }),
    }));
  },

  async createPlaylistWithTracks(title: string, description: string | undefined, trackIds: string[]) {
    const playlist = await this.createPlaylist(title, description);
    for (const trackId of trackIds) {
      await this.addTrackToPlaylist(playlist.id, trackId);
    }
    return { ...playlist, trackIds };
  },

  async updatePlaylist(playlistId: string, title: string, description?: string) {
    return mapPlaylist(await request<PlaylistResponse>(`/api/playlists/${playlistId}`, {
      method: 'PATCH',
      body: JSON.stringify({ title, description }),
    }));
  },

  async deletePlaylist(playlistId: string) {
    await request(`/api/playlists/${playlistId}`, { method: 'DELETE' });
  },

  async addTrackToPlaylist(playlistId: string, trackId: string) {
    await request(`/api/playlists/${playlistId}/tracks`, {
      method: 'POST',
      body: JSON.stringify({ trackId: Number(trackId) }),
    });
  },

  async removeTrackFromPlaylist(playlistId: string, trackId: string) {
    await request(`/api/playlists/${playlistId}/tracks/${trackId}`, { method: 'DELETE' });
  },

  async likes() {
    const data = await request<PageResponse<LikeResponse>>('/api/likes?page=0&size=100');
    return data.content;
  },

  async likeTrack(trackId: string) {
    return request<LikeResponse>('/api/likes', {
      method: 'POST',
      body: JSON.stringify({ targetId: Number(trackId), targetType: 'TRACK' }),
    });
  },

  async likeAlbum(albumId: string) {
    return request<LikeResponse>('/api/likes', {
      method: 'POST',
      body: JSON.stringify({ targetId: Number(albumId), targetType: 'ALBUM' }),
    });
  },

  async unlike(likeId: string) {
    await request(`/api/likes/${likeId}`, { method: 'DELETE' });
  },

  async follows() {
    const data = await request<PageResponse<FollowResponse>>('/api/follows?page=0&size=100');
    return data.content;
  },

  async followArtist(artistId: string) {
    return request<FollowResponse>('/api/follows', {
      method: 'POST',
      body: JSON.stringify({ artistId: Number(artistId) }),
    });
  },

  async unfollowArtist(artistId: string) {
    await request(`/api/follows/${artistId}`, { method: 'DELETE' });
  },

  async toggleFollowNotifications(artistId: string) {
    return request(`/api/follows/${artistId}/notifications`, { method: 'PATCH' });
  },

  async chart(): Promise<PopularChart[]> {
    const data = await request<Array<{ trackId: number; rank: number; playCount: number }>>('/api/popular-charts/top100');
    return data.map((item) => ({ trackId: toId(item.trackId), rank: item.rank, playCount: item.playCount, change: 0 }));
  },

  async comments(trackId: string): Promise<Comment[]> {
    const data = await request<PageResponse<TrackCommentResponse>>(`/api/tracks/${trackId}/comments?page=0&size=30`);
    return data.content.map((comment) => ({
      id: toId(comment.commentId),
      trackId: toId(comment.trackId),
      userId: toId(comment.userId),
      userName: `User ${comment.userId}`,
      content: comment.content,
      createdAt: comment.createdAt,
    }));
  },

  async createComment(trackId: string, content: string): Promise<Comment> {
    const comment = await request<TrackCommentResponse>(`/api/tracks/${trackId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    });
    return {
      id: toId(comment.commentId),
      trackId: toId(comment.trackId),
      userId: toId(comment.userId),
      userName: `User ${comment.userId}`,
      content: comment.content,
      createdAt: comment.createdAt,
    };
  },

  async updateComment(trackId: string, commentId: string, content: string): Promise<Comment> {
    const comment = await request<TrackCommentResponse>(`/api/tracks/${trackId}/comments/${commentId}`, {
      method: 'PATCH',
      body: JSON.stringify({ content }),
    });
    return {
      id: toId(comment.commentId),
      trackId: toId(comment.trackId),
      userId: toId(comment.userId),
      userName: `User ${comment.userId}`,
      content: comment.content,
      createdAt: comment.createdAt,
    };
  },

  async deleteComment(trackId: string, commentId: string) {
    await request(`/api/tracks/${trackId}/comments/${commentId}`, { method: 'DELETE' });
  },

  async notifications(): Promise<Notification[]> {
    const data = await request<PageResponse<any>>('/api/notifications?page=0&size=30');
    return data.content.map((item) => ({
      id: toId(item.id ?? item.notificationId),
      userId: '',
      type: item.type ?? 'ACTIVITY',
      title: item.title ?? String(item.type ?? '알림'),
      message: item.message ?? item.content ?? '',
      isRead: Boolean(item.read ?? item.isRead ?? item.status === 'READ'),
      createdAt: item.createdAt ?? '',
    }));
  },

  async markNotificationRead(notificationId: string) {
    await request(`/api/notifications/${notificationId}/read`, { method: 'PATCH' });
  },

  async markAllNotificationsRead() {
    await request('/api/notifications/read-all', { method: 'PATCH' });
  },

  async deleteNotification(notificationId: string) {
    await request(`/api/notifications/${notificationId}`, { method: 'DELETE' });
  },

  async deleteAllNotifications() {
    await request('/api/notifications', { method: 'DELETE' });
  },

  async subscriptions(): Promise<Subscription[]> {
    const data = await request<SubscriptionResponse[]>('/api/me/subscriptions');
    return data.map((subscription) => ({
      id: toId(subscription.id),
      userId: '',
      planName: subscription.planType,
      status: subscription.status,
      startDate: subscription.startDate,
      endDate: subscription.expiryDate,
      price: subscription.planType === 'RECURRING' ? 10900 : 0,
    }));
  },

  async purchaseSubscription(planType: 'FREE' | 'RECURRING' | 'RECURRING_AUTO'): Promise<Subscription> {
    const subscription = await request<SubscriptionResponse>('/api/me/point-orders/purchases', {
      method: 'POST',
      body: JSON.stringify({ planType }),
    });
    return {
      id: toId(subscription.id),
      userId: '',
      planName: subscription.planType,
      status: subscription.status,
      startDate: subscription.startDate,
      endDate: subscription.expiryDate,
      price: subscription.planType === 'FREE' ? 0 : 50,
    };
  },

  async cancelSubscription() {
    await request('/api/me/subscriptions', { method: 'DELETE' });
  },

  async wallet() {
    return request<WalletResponse>('/api/me/wallets');
  },

  async walletHistories() {
    return request<Array<{
      id: number;
      pointType: string;
      pointHistoryType: string;
      amount: number;
      balanceAfter: number;
      logDescription: string;
      createdAt: string;
    }>>('/api/me/wallets/histories');
  },

  async payments() {
    return request<Array<{
      id: number;
      amount: number;
      status: string;
      orderNumber: string;
      paidAt?: string;
      refundedAt?: string;
    }>>('/api/v1/payments');
  },

  async payment(paymentId: string) {
    return request(`/api/v1/payments/${paymentId}`);
  },

  async createCashOrder(productType: string) {
    return request<{ orderNumber: string }>('/api/v1/cash-orders/purchase', {
      method: 'POST',
      body: JSON.stringify({ productType }),
    });
  },

  async refundCashOrder(orderNumber: string, reason: string) {
    return request('/api/v1/cash-orders/refund', {
      method: 'POST',
      body: JSON.stringify({ orderNumber, reason }),
    });
  },

  async registerBillingKey(billingKeyId: string) {
    return request('/api/v1/billing-keys', {
      method: 'POST',
      body: JSON.stringify({ billingKeyId }),
    });
  },

  async deleteBillingKey(billingKeyId: string) {
    await request(`/api/v1/billing-keys/${billingKeyId}`, { method: 'DELETE' });
  },

  async playbackHistory(): Promise<PlaybackResponse[]> {
    return request<PlaybackResponse[]>('/api/me/playback-history');
  },

  async aiRecommendations(limit = 20) {
    return request<{ tracks: AiTrackResponse[]; reasoningHint?: string; basedOnCount?: number }>(
      `/api/ai/recommendations?limit=${limit}`,
    );
  },

  async moodSearch(input: { query: string; limit: number; mode: 'LYRICS' | 'BALANCED' | 'METADATA' }) {
    return request<{ searchTextUsed: string; modeUsed: string; tracks: AiTrackResponse[] }>('/api/ai/search/mood', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  async generatePlaylist(input: { prompt?: string; seedTrackIds?: number[]; size: number; diversityLambda?: number }) {
    return request<{ searchTextUsed: string; tracks: AiTrackResponse[] }>('/api/ai/playlists/generate', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  async myArtists() {
    return request<Array<{
      artistId: number;
      name: string;
      artistType: string;
      bio?: string;
      profileImageUrl?: string;
      createdAt?: string;
      updatedAt?: string;
    }>>('/api/my/artists');
  },

  async updateArtist(artistId: string, input: { name?: string; bio?: string; profileImageUrl?: string }) {
    return request(`/api/artists/${artistId}`, {
      method: 'PATCH',
      body: JSON.stringify(input),
    });
  },

  async activateArtist(artistId: string) {
    return request(`/api/artists/${artistId}/activate`, { method: 'PATCH' });
  },

  async deactivateArtist(artistId: string) {
    return request(`/api/artists/${artistId}/deactivate`, { method: 'PATCH' });
  },

  async createArtistApplication(input: { requestedName: string; artistType: string; bio?: string; profileImageUrl?: string }) {
    return request('/api/artist-applications', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  async createAlbumApplication(input: {
    artistId: number;
    title: string;
    description?: string;
    coverImageUrl?: string;
    publishDelayDays: number;
  }) {
    return request('/api/album-applications', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  async createFreeTrackApplication(input: { title: string; lyrics?: string; genre: string; durationSec: number; audioFile: File }) {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify({
      title: input.title,
      lyrics: input.lyrics,
      genre: input.genre,
      durationSec: input.durationSec,
    })], { type: 'application/json' }));
    formData.append('audioFile', input.audioFile);

    return request('/api/track-applications/free-creations', {
      method: 'POST',
      body: formData,
    });
  },

  async createOfficialTrackApplication(input: {
    artistId: number;
    albumId: number;
    trackNumber: number;
    title: string;
    lyrics?: string;
    genre: string;
    durationSec: number;
    featuredArtistText?: string;
    publishDelayDays: number;
    audioFile: File;
  }) {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify({
      artistId: input.artistId,
      albumId: input.albumId,
      trackNumber: input.trackNumber,
      title: input.title,
      lyrics: input.lyrics,
      genre: input.genre,
      durationSec: input.durationSec,
      featuredArtistText: input.featuredArtistText,
      publishDelayDays: input.publishDelayDays,
    })], { type: 'application/json' }));
    formData.append('audioFile', input.audioFile);

    return request('/api/track-applications/official-releases', {
      method: 'POST',
      body: formData,
    });
  },

  async myArtistApplications() {
    return request<any[]>('/api/artist-applications/me');
  },

  async myAlbumApplications() {
    return request<any[]>('/api/album-applications/me');
  },

  async myTrackApplications() {
    return request<any[]>('/api/track-applications/me');
  },

  async adminArtistApplications(status?: string) {
    const query = status ? `?status=${status}&page=0&size=50` : '?page=0&size=50';
    const data = await request<PageResponse<any>>(`/api/admin/artist-applications${query}`);
    return data.content;
  },

  async adminAlbumApplications(status?: string) {
    const query = status ? `?status=${status}&page=0&size=50` : '?page=0&size=50';
    const data = await request<PageResponse<any>>(`/api/admin/album-applications${query}`);
    return data.content;
  },

  async adminTrackApplications(status?: string) {
    const query = status ? `?status=${status}&page=0&size=50` : '?page=0&size=50';
    const data = await request<PageResponse<any>>(`/api/admin/track-applications${query}`);
    return data.content;
  },

  async approveArtistApplication(applicationId: string) {
    return request(`/api/admin/artist-applications/${applicationId}/approve`, { method: 'POST' });
  },

  async rejectArtistApplication(applicationId: string, rejectionReason: string) {
    return request(`/api/admin/artist-applications/${applicationId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ rejectionReason }),
    });
  },

  async approveAlbumApplication(applicationId: string) {
    return request(`/api/admin/album-applications/${applicationId}/approve`, { method: 'POST' });
  },

  async rejectAlbumApplication(applicationId: string, rejectionReason: string) {
    return request(`/api/admin/album-applications/${applicationId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ rejectionReason }),
    });
  },

  async approveTrackApplication(applicationId: string) {
    return request(`/api/admin/track-applications/${applicationId}/approve`, { method: 'POST' });
  },

  async rejectTrackApplication(applicationId: string, rejectionReason: string) {
    return request(`/api/admin/track-applications/${applicationId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ rejectionReason }),
    });
  },
};

export type { PlayTrackResponse };
