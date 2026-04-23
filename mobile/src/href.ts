import type { Href } from 'expo-router';

export function hrefPostDetail(id: number): Href {
  return `/(tabs)/post/${id}` as Href;
}

export function hrefCompose(): Href {
  return '/(tabs)/compose' as Href;
}
