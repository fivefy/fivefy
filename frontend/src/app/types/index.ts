export interface User {
  id: string;
  name: string;
  email: string;
  profileImage?: string;
  subscriptionStatus: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'NONE';
  points: number;
}

export interface Artist {
  id: string;
  name: string;
  profileImage?: string;
  genre: string[];
  followers: number;
  bio?: string;
}

export interface Album {
  id: string;
  title: string;
  coverImage?: string;
  artistId: string;
  artistName: string;
  releaseDate: string;
  trackCount: number;
}

export interface Track {
  id: string;
  title: string;
  artistId: string;
  artistName: string;
  albumId: string;
  albumTitle: string;
  coverImage?: string;
  duration: number;
  playCount: number;
  lyrics?: string;
  audioUrl?: string;
}

export interface Playlist {
  id: string;
  title: string;
  description?: string;
  coverImage?: string;
  userId: string;
  trackIds: string[];
  isPublic: boolean;
  createdAt: string;
}

export interface Like {
  userId: string;
  trackId: string;
  createdAt: string;
}

export interface Playback {
  userId: string;
  trackId: string;
  playedAt: string;
}

export interface Comment {
  id: string;
  trackId: string;
  userId: string;
  userName: string;
  content: string;
  createdAt: string;
}

export interface Review {
  id: string;
  trackId: string;
  userId: string;
  userName: string;
  rating: number;
  content: string;
  createdAt: string;
}

export interface Subscription {
  id: string;
  userId: string;
  planName: string;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  startDate: string;
  endDate: string;
  price: number;
}

export interface Payment {
  id: string;
  userId: string;
  amount: number;
  status: 'APPROVED' | 'FAILED';
  createdAt: string;
  description: string;
}

export interface Point {
  id: string;
  userId: string;
  amount: number;
  type: 'CHARGE' | 'USE' | 'CHARGE_CANCEL' | 'EXPIRE' | 'EVENT' | 'REFUND';
  createdAt: string;
  description: string;
}

export interface PopularChart {
  rank: number;
  trackId: string;
  playCount: number;
  change: number; // 순위 변동
}

export interface Follow {
  userId: string;
  targetId: string;
  targetType: 'ARTIST' | 'USER';
  createdAt: string;
}

export interface Notification {
  id: string;
  userId: string;
  type: 'SUBSCRIPTION' | 'RECOMMENDATION' | 'ACTIVITY';
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface Recommendation {
  id: string;
  userId: string;
  trackId: string;
  reason: string;
  score: number;
  createdAt: string;
}
