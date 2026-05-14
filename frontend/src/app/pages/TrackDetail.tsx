import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { Play, Trash2 } from 'lucide-react';
import { Track, Comment } from '../types';
import { api } from '../lib/api';
import { useMusic } from '../context/MusicContext';
import { Button } from '../components/ui/button';
import { Textarea } from '../components/ui/textarea';

export function TrackDetail() {
  const { id } = useParams<{ id: string }>();
  const { tracks, playTrack } = useMusic();
  const [track, setTrack] = useState<Track | null>(tracks.find((item) => item.id === id) ?? null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentContent, setCommentContent] = useState('');
  const [commentError, setCommentError] = useState<string | null>(null);
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editingContent, setEditingContent] = useState('');

  useEffect(() => {
    if (!id) return;
    api.track(id).then(setTrack).catch(() => undefined);
    api.comments(id).then(setComments).catch(() => setComments([]));
  }, [id]);

  const handleCreateComment = async () => {
    if (!id || !commentContent.trim()) return;
    setCommentError(null);
    try {
      const comment = await api.createComment(id, commentContent.trim());
      setComments((prev) => [comment, ...prev]);
      setCommentContent('');
    } catch (error) {
      setCommentError(error instanceof Error ? error.message : '댓글 작성에 실패했습니다.');
    }
  };

  const handleUpdateComment = async () => {
    if (!id || !editingCommentId || !editingContent.trim()) return;
    setCommentError(null);
    try {
      const comment = await api.updateComment(id, editingCommentId, editingContent.trim());
      setComments((prev) => prev.map((item) => item.id === editingCommentId ? comment : item));
      setEditingCommentId(null);
      setEditingContent('');
    } catch (error) {
      setCommentError(error instanceof Error ? error.message : '댓글 수정에 실패했습니다.');
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    if (!id) return;
    setCommentError(null);
    try {
      await api.deleteComment(id, commentId);
      setComments((prev) => prev.filter((item) => item.id !== commentId));
    } catch (error) {
      setCommentError(error instanceof Error ? error.message : '댓글 삭제에 실패했습니다.');
    }
  };

  if (!track) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-white mb-2">트랙을 찾을 수 없습니다</h2>
        <Link to="/" className="text-purple-500 hover:underline">홈으로 돌아가기</Link>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row gap-8 items-start">
        <div className="w-full md:w-64 aspect-square rounded-lg overflow-hidden bg-zinc-800">
          {track.coverImage && <img src={track.coverImage} alt={track.title} className="w-full h-full object-cover" />}
        </div>
        <div className="flex-1">
          <div className="text-sm text-zinc-400 mb-2">곡</div>
          <h1 className="text-5xl font-bold text-white mb-4">{track.title}</h1>
          <Link to={`/artist/${track.artistId}`} className="text-xl text-white hover:underline">
            {track.artistName}
          </Link>
          <div className="text-zinc-400 mt-2">{track.albumTitle}</div>
          <Button className="rounded-full gap-2 mt-6" onClick={() => void playTrack(track, [track])}>
            <Play className="w-5 h-5" fill="currentColor" />
            재생
          </Button>
        </div>
      </div>

      {track.lyrics && (
        <section>
          <h2 className="text-2xl font-bold text-white mb-4">가사</h2>
          <pre className="whitespace-pre-wrap text-zinc-300 font-sans">{track.lyrics}</pre>
        </section>
      )}

      <section>
        <h2 className="text-2xl font-bold text-white mb-4">댓글</h2>
        <div className="mb-5 space-y-3">
          <Textarea
            value={commentContent}
            onChange={(event) => setCommentContent(event.target.value)}
            placeholder="댓글을 입력하세요"
          />
          <div className="flex items-center gap-3">
            <Button onClick={handleCreateComment} disabled={!commentContent.trim()}>
              댓글 작성
            </Button>
            {commentError && <span className="text-sm text-red-400">{commentError}</span>}
          </div>
        </div>
        {comments.length > 0 ? (
          <div className="space-y-3">
            {comments.map((comment) => (
              <div key={comment.id} className="rounded-lg bg-zinc-800/60 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="text-sm text-zinc-400 mb-1">{comment.userName}</div>
                    {editingCommentId === comment.id ? (
                      <div className="space-y-2">
                        <Textarea value={editingContent} onChange={(event) => setEditingContent(event.target.value)} />
                        <div className="flex gap-2">
                          <Button size="sm" onClick={handleUpdateComment}>저장</Button>
                          <Button size="sm" variant="outline" onClick={() => setEditingCommentId(null)}>취소</Button>
                        </div>
                      </div>
                    ) : (
                      <div className="text-zinc-200">{comment.content}</div>
                    )}
                  </div>
                  {editingCommentId !== comment.id && (
                    <div className="flex gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => {
                          setEditingCommentId(comment.id);
                          setEditingContent(comment.content);
                        }}
                      >
                        수정
                      </Button>
                      <Button size="icon" variant="ghost" onClick={() => handleDeleteComment(comment.id)}>
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-zinc-400">아직 댓글이 없습니다.</p>
        )}
      </section>
    </div>
  );
}
