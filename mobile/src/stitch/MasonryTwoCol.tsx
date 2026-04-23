import { useMemo, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { spacing } from '@/theme';

type Props<T> = {
  data: T[];
  keyExtractor: (item: T) => string;
  renderItem: (item: T, meta: { column: 0 | 1; indexInColumn: number }) => ReactNode;
};

export function MasonryTwoCol<T>({ data, keyExtractor, renderItem }: Props<T>) {
  const [left, right] = useMemo(() => {
    const l: T[] = [];
    const r: T[] = [];
    data.forEach((item, i) => (i % 2 === 0 ? l : r).push(item));
    return [l, r];
  }, [data]);

  return (
    <View style={styles.row}>
      <View style={styles.col}>
        {left.map((item, indexInColumn) => (
          <View key={keyExtractor(item)} style={styles.cell}>
            {renderItem(item, { column: 0, indexInColumn })}
          </View>
        ))}
      </View>
      <View style={styles.col}>
        {right.map((item, indexInColumn) => (
          <View key={keyExtractor(item)} style={styles.cell}>
            {renderItem(item, { column: 1, indexInColumn })}
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.md,
  },
  col: {
    flex: 1,
    gap: spacing.md,
  },
  cell: {
    width: '100%',
  },
});
