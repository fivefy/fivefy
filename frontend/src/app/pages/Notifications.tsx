import { useEffect, useState } from 'react';
import { Bell, Check, Trash2 } from 'lucide-react';
import { Notification } from '../types';
import { api } from '../lib/api';
import { Button } from '../components/ui/button';

export function Notifications() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  const loadNotifications = () => {
    api.notifications().then(setNotifications).catch(() => setNotifications([]));
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const markAsRead = async (notificationId: string) => {
    try {
      await api.markNotificationRead(notificationId);
      setNotifications((prev) => prev.map((item) => item.id === notificationId ? { ...item, isRead: true } : item));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '알림 읽음 처리에 실패했습니다.');
    }
  };

  const markAllAsRead = async () => {
    try {
      await api.markAllNotificationsRead();
      setNotifications((prev) => prev.map((item) => ({ ...item, isRead: true })));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '전체 읽음 처리에 실패했습니다.');
    }
  };

  const deleteNotification = async (notificationId: string) => {
    try {
      await api.deleteNotification(notificationId);
      setNotifications((prev) => prev.filter((item) => item.id !== notificationId));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '알림 삭제에 실패했습니다.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-bold text-white">알림</h1>
        {notifications.some((notification) => !notification.isRead) && (
          <Button variant="outline" className="gap-2" onClick={markAllAsRead}>
            <Check className="w-4 h-4" />
            모두 읽음
          </Button>
        )}
      </div>
      {message && <p className="text-sm text-red-400">{message}</p>}
      {notifications.length > 0 ? (
        <div className="space-y-3">
          {notifications.map((notification) => (
            <div key={notification.id} className={`rounded-lg p-4 ${notification.isRead ? 'bg-zinc-800/40' : 'bg-zinc-800/80'}`}>
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="text-white font-medium">{notification.title}</div>
                  <div className="text-sm text-zinc-400 mt-1">{notification.message}</div>
                </div>
                <div className="flex items-center gap-1">
                  {!notification.isRead && (
                    <Button variant="ghost" size="icon" onClick={() => markAsRead(notification.id)}>
                      <Check className="w-4 h-4" />
                    </Button>
                  )}
                  <Button variant="ghost" size="icon" onClick={() => deleteNotification(notification.id)}>
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-20">
          <Bell className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
          <p className="text-zinc-400">표시할 알림이 없습니다.</p>
        </div>
      )}
    </div>
  );
}
