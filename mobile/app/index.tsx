// Root-level redirect: send the user to (tabs) when logged in, otherwise
// to (auth)/login. While the auth context is loading we show a tiny
// splash so we don't flash the wrong screen.

import { Redirect } from 'expo-router';
import { useAuth } from '@/auth';
import { KittyLoader } from '@/components';

export default function Gate() {
  const { user, loading } = useAuth();

  if (loading) {
    return <KittyLoader label="正在打开喵友圈..." />;
  }

  return <Redirect href={user ? '/(tabs)' : '/(auth)/login'} />;
}
