import {Redirect} from 'expo-router';
import {useAuth} from '@/auth';
import {MndLoader} from '@/components';

export default function Gate() {
  const { user, loading } = useAuth();

  if (loading) {
    return <MndLoader label="M&D 正在确认登录状态..." />;
  }

  return <Redirect href={user ? '/(tabs)' : '/(auth)/login'} />;
}
