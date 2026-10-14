import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../store/authStore';
import { notificationApi } from '../api/notificationApi';

/**
 * FR-15 — STOMP subscribe to /topic/users/{userId}/notifications.
 * Falls back to REST unread poll every 60s when disconnected.
 */
export function useNotificationWebSocket() {
  const userId = useAuthStore(s => s.userId);
  const isAuthenticated = useAuthStore(s => s.isAuthenticated);
  const clientRef = useRef<Client | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const accessToken = sessionStorage.getItem('accessToken');
    if (!isAuthenticated || !accessToken || !userId) {
      return;
    }

    const bumpUnread = () => {
      window.dispatchEvent(new Event('unreadCountChanged'));
    };

    const startPoll = () => {
      if (pollRef.current) return;
      pollRef.current = setInterval(() => {
        void notificationApi.getUnreadCount().then(bumpUnread).catch(() => undefined);
      }, 60_000);
    };

    const stopPoll = () => {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
    };

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws') as unknown as WebSocket,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        stopPoll();
        client.subscribe(`/topic/users/${userId}/notifications`, () => {
          bumpUnread();
        });
      },
      onDisconnect: () => {
        startPoll();
      },
      onStompError: () => {
        startPoll();
      },
    });

    clientRef.current = client;
    client.activate();
    startPoll();

    return () => {
      stopPoll();
      void client.deactivate();
      clientRef.current = null;
    };
  }, [isAuthenticated, userId]);
}
