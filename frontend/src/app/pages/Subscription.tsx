import { useEffect, useState } from 'react';
import { useMusic } from '../context/MusicContext';
import { Card } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Check, Sparkles, Crown, Zap, Bell } from 'lucide-react';
import { api } from '../lib/api';
import { Notification, Subscription as SubscriptionType } from '../types';

const plans = [
  {
    id: 'free',
    name: '무료',
    price: 0,
    features: [
      '광고 포함',
      '셔플 재생만 가능',
      '낮은 음질',
      '오프라인 재생 불가',
    ],
    icon: Zap,
  },
  {
    id: 'premium',
    name: '프리미엄',
    price: 10900,
    features: [
      '광고 없음',
      '무제한 건너뛰기',
      '최고 음질',
      '오프라인 재생',
      'AI 추천 우선 제공',
    ],
    icon: Crown,
    popular: true,
  },
  {
    id: 'family',
    name: '패밀리',
    price: 16900,
    features: [
      '프리미엄 모든 혜택',
      '최대 6명 사용',
      '개별 계정',
      '자녀 보호 기능',
    ],
    icon: Sparkles,
  },
];

export function Subscription() {
  const { user, refreshData } = useMusic();
  const [subscriptions, setSubscriptions] = useState<SubscriptionType[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const currentPlan = subscriptions.find((subscription) => subscription.status === 'ACTIVE') ?? {
    id: 'none',
    userId: user.id,
    planName: '무료',
    status: 'ACTIVE' as const,
    startDate: new Date().toISOString(),
    endDate: new Date().toISOString(),
    price: 0,
  };
  const unreadNotifications = notifications.filter(n => !n.isRead);

  useEffect(() => {
    api.subscriptions().then(setSubscriptions).catch(() => setSubscriptions([]));
    api.notifications().then(setNotifications).catch(() => setNotifications([]));
  }, []);

  const loadSubscriptions = async () => {
    setSubscriptions(await api.subscriptions());
    await refreshData();
  };

  const planTypeById = (planId: string): 'FREE' | 'RECURRING' | 'RECURRING_AUTO' => {
    if (planId === 'free') return 'FREE';
    if (planId === 'family') return 'RECURRING_AUTO';
    return 'RECURRING';
  };

  const planLabel = (planName: string) => {
    if (planName === 'FREE') return '무료 체험';
    if (planName === 'RECURRING') return '정기 구독';
    if (planName === 'RECURRING_AUTO') return '자동 구독';
    return planName;
  };

  const purchasePlan = async (planId: string) => {
    setMessage(null);
    try {
      const subscription = await api.purchaseSubscription(planTypeById(planId));
      setSubscriptions((prev) => [subscription, ...prev]);
      await loadSubscriptions();
      setMessage('구독을 활성화했습니다.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '구독 처리에 실패했습니다.');
    }
  };

  const cancelCurrentPlan = async () => {
    setMessage(null);
    try {
      await api.cancelSubscription();
      await loadSubscriptions();
      setMessage('구독을 취소했습니다.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '구독 취소에 실패했습니다.');
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">구독 관리</h1>
        <p className="text-zinc-400">
          현재 구독 플랜과 포인트를 확인하고 관리하세요
        </p>
      </div>

      {/* Current Subscription */}
      <Card className="p-6 bg-gradient-to-r from-purple-900/30 to-transparent border-purple-800/50">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <h3 className="text-xl font-bold text-white">
                {planLabel(currentPlan.planName)}
              </h3>
              <Badge className="bg-green-500">활성</Badge>
            </div>
            <p className="text-zinc-400 mb-4">
              {new Date(currentPlan.startDate).toLocaleDateString('ko-KR')} ~{' '}
              {new Date(currentPlan.endDate).toLocaleDateString('ko-KR')}
            </p>
            <div className="text-2xl font-bold text-white">
              ₩{currentPlan.price.toLocaleString()}/월
            </div>
          </div>
          <div className="text-right">
            <div className="text-sm text-zinc-400 mb-1">보유 포인트</div>
            <div className="text-2xl font-bold text-yellow-500">
              {user.points.toLocaleString()}P
            </div>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => setMessage('포인트 충전 API는 아직 백엔드 결제 승인 흐름 연결이 필요합니다.')}
            >
              충전하기
            </Button>
          </div>
        </div>
        <div className="mt-4">
          {currentPlan.id !== 'none' && currentPlan.planName !== '무료' && (
            <Button variant="outline" size="sm" onClick={cancelCurrentPlan}>
              구독 취소
            </Button>
          )}
          {message && <p className="mt-3 text-sm text-zinc-300">{message}</p>}
        </div>
      </Card>

      {/* Notifications */}
      {unreadNotifications.length > 0 && (
        <Card className="p-6">
          <div className="flex items-center gap-2 mb-4">
            <Bell className="w-5 h-5 text-blue-500" />
            <h3 className="text-lg font-bold text-white">알림</h3>
            <Badge>{unreadNotifications.length}</Badge>
          </div>
          <div className="space-y-3">
            {unreadNotifications.map((notification) => (
              <div
                key={notification.id}
                className="p-3 bg-zinc-800/50 rounded-lg"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="text-white font-medium mb-1">
                      {notification.title}
                    </div>
                    <div className="text-sm text-zinc-400">
                      {notification.message}
                    </div>
                    <div className="text-xs text-zinc-500 mt-1">
                      {new Date(notification.createdAt).toLocaleDateString('ko-KR')}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Available Plans */}
      <div>
        <h2 className="text-2xl font-bold text-white mb-6">이용 가능한 플랜</h2>
        <div className="grid md:grid-cols-3 gap-6">
          {plans.map((plan) => {
            const Icon = plan.icon;
            const isCurrent = plan.name === planLabel(currentPlan.planName) || planTypeById(plan.id) === currentPlan.planName;
            
            return (
              <Card
                key={plan.id}
                className={`p-6 relative ${
                  plan.popular
                    ? 'border-purple-500 bg-gradient-to-b from-purple-900/20 to-transparent'
                    : ''
                }`}
              >
                {plan.popular && (
                  <Badge className="absolute top-4 right-4 bg-purple-500">
                    인기
                  </Badge>
                )}
                
                <div className="mb-6">
                  <div className="flex items-center gap-2 mb-2">
                    <Icon className="w-6 h-6 text-purple-500" />
                    <h3 className="text-xl font-bold text-white">
                      {plan.name}
                    </h3>
                  </div>
                  <div className="text-3xl font-bold text-white mb-1">
                    {plan.price === 0 ? '무료' : `₩${plan.price.toLocaleString()}`}
                  </div>
                  {plan.price > 0 && (
                    <div className="text-sm text-zinc-400">/월</div>
                  )}
                </div>

                <ul className="space-y-3 mb-6">
                  {plan.features.map((feature, idx) => (
                    <li key={idx} className="flex items-start gap-2">
                      <Check className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" />
                      <span className="text-sm text-zinc-300">{feature}</span>
                    </li>
                  ))}
                </ul>

                <Button
                  className="w-full"
                  variant={isCurrent ? 'outline' : 'default'}
                  disabled={isCurrent}
                  onClick={() => purchasePlan(plan.id)}
                >
                  {isCurrent ? '현재 플랜' : '구독하기'}
                </Button>
              </Card>
            );
          })}
        </div>
      </div>

      {/* Payment History */}
      <Card className="p-6">
        <h3 className="text-xl font-bold text-white mb-4">결제 내역</h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between py-3 border-b border-zinc-800">
            <div>
              <div className="text-white font-medium">프리미엄 플랜</div>
              <div className="text-sm text-zinc-400">2026-01-01</div>
            </div>
            <div className="text-right">
              <div className="text-white font-medium">
                ₩{currentPlan.price.toLocaleString()}
              </div>
              <Badge className="bg-green-500">완료</Badge>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
