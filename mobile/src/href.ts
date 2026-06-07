import type { Href } from 'expo-router';

export function hrefPostDetail(id: number): Href {
  return `/(tabs)/post/${id}` as Href;
}

export function hrefListingDetail(id: number): Href {
  return `/(tabs)/listing/${id}` as Href;
}

export function hrefOrderDetail(id: number): Href {
  return `/(tabs)/order/${id}` as Href;
}

export function hrefCompose(): Href {
  return '/(tabs)/compose' as Href;
}
