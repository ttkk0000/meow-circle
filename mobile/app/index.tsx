// Root-level redirect: send the user to (tabs) when logged in, otherwise
// to (auth)/login. While the auth context is loading we show a tiny
// splash so we don't flash the wrong screen.

import { Redirect } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '@/auth';
import { colors } from '@/theme';

export default function Gate() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <View
        style={{
          flex: 1,
          backgroundColor: colors.surface100,
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <ActivityIndicator color={colors.ink} />
      </View>
    );
  }

  return <Redirect href={user ? '/(tabs)' : '/(auth)/login'} />;
}
