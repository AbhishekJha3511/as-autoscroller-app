import { memo, useEffect, useState } from 'react';
import { AppState, Linking, Pressable, ScrollView, Text, View } from 'react-native';
import Slider from '@react-native-community/slider';
import * as Scroller from './modules/auto-scroll-controller';
import type { Config, Custom, Direction, Side } from './modules/auto-scroll-controller';

const GITHUB = 'https://github.com/AbhishekJha3511';
const DIRS: Direction[] = ['DOWN', 'UP', 'LEFT', 'RIGHT'];
const SIDES: Side[] = ['RIGHT', 'LEFT'];
const DEFAULT_CUSTOM: Custom = { size: 48, alpha: 85, peek: 25, side: 'RIGHT' };

type RowProps = {
  label: string; min: number; max: number; value: number;
  format: (v: number) => string; onCommit: (v: number) => void;
};

// Local state while dragging => only this row re-renders; value is sent once on release.
const Row = memo(function Row({ label, min, max, value, format, onCommit }: RowProps) {
  const [v, setV] = useState(value);
  return (
    <View style={{ marginTop: 20 }}>
      <Text style={{ fontSize: 16 }}>{label}: {format(v)}</Text>
      <Slider
        minimumValue={min} maximumValue={max} step={1} value={v}
        onValueChange={setV}
        onSlidingComplete={(n) => { setV(n); onCommit(n); }}
      />
    </View>
  );
});

const Chip = ({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) => (
  <Pressable onPress={onPress}
    style={{ padding: 10, borderRadius: 8, backgroundColor: active ? '#222' : '#ddd' }}>
    <Text style={{ color: active ? '#fff' : '#000' }}>{label}</Text>
  </Pressable>
);

export default function App() {
  const [tab, setTab] = useState<'scroll' | 'custom'>('scroll');
  const [cfg, setCfg] = useState<Config>(() => Scroller.getConfig());
  const [custom, setCustom] = useState<Custom>(() => Scroller.getCustomize());
  const [enabled, setEnabled] = useState(false);
  const [resetKey, setResetKey] = useState(0);

  const updateCfg = (patch: Partial<Config>) => {
    const next = { ...cfg, ...patch };
    setCfg(next);
    Scroller.setConfig(next);
  };
  const updateCustom = (patch: Partial<Custom>) => {
    const next = { ...custom, ...patch };
    setCustom(next);
    Scroller.setCustomize(next);
  };

  useEffect(() => {
    const check = () => setEnabled(Scroller.isServiceEnabled());
    check();
    const sub = AppState.addEventListener('change', check);
    return () => sub.remove();
  }, []);

  const tabBtn = (id: 'scroll' | 'custom', label: string) => (
    <Pressable onPress={() => setTab(id)}
      style={{ flex: 1, padding: 14, alignItems: 'center', backgroundColor: tab === id ? '#222' : '#ddd' }}>
      <Text style={{ color: tab === id ? '#fff' : '#000', fontWeight: '600' }}>{label}</Text>
    </Pressable>
  );

  return (
    <View style={{ flex: 1, backgroundColor: '#fff', paddingTop: 44 }}>
      <View style={{ flexDirection: 'row' }}>
        {tabBtn('scroll', 'Scroll')}
        {tabBtn('custom', 'Customize')}
      </View>

      <ScrollView style={{ flex: 1 }} contentContainerStyle={{ padding: 20 }}>
        {tab === 'scroll' ? (
          <>
            <Text style={{ fontSize: 26 }}>Auto Scroller</Text>
            <Text style={{ color: enabled ? 'green' : 'crimson', marginVertical: 8 }}>
              {enabled ? '● Service ON, use the edge button' : '○ Service OFF, grant permission below'}
            </Text>
            <Pressable onPress={Scroller.openAccessibilitySettings}
              style={{ backgroundColor: '#ddd', padding: 14, borderRadius: 8 }}>
              <Text>Grant permission: Accessibility → Auto Scroller → On</Text>
            </Pressable>

            <Row label="Delay between swipes" min={0} max={180} value={cfg.delaySec}
              format={(v) => `${v} s`} onCommit={(v) => updateCfg({ delaySec: v })} />
            <Row label="Screen area per swipe" min={10} max={100} value={cfg.areaPct}
              format={(v) => `${v}%`} onCommit={(v) => updateCfg({ areaPct: v })} />
            <Row label="Swipe speed" min={5} max={30} value={cfg.speedX10}
              format={(v) => `${(v / 10).toFixed(1)}x`} onCommit={(v) => updateCfg({ speedX10: v })} />

            <Text style={{ fontSize: 16, marginTop: 20 }}>Direction</Text>
            <View style={{ flexDirection: 'row', gap: 8, marginTop: 8 }}>
              {DIRS.map((d) => (
                <Chip key={d} label={d} active={cfg.direction === d} onPress={() => updateCfg({ direction: d })} />
              ))}
            </View>

            <Text style={{ color: '#555', marginTop: 24, fontSize: 13 }}>
              Tap ▶ on the edge button to start, ❚❚ to pause. » hides the controller to a small edge
              handle, ✕ closes it (bring it back in the Customize tab). “Down” = next video, “Up” = previous.
            </Text>
          </>
        ) : (
          <>
            <Text style={{ fontSize: 26 }}>Customize controller</Text>

            <Row key={`s${resetKey}`} label="Edge button size" min={36} max={80} value={custom.size}
              format={(v) => `${v} dp`} onCommit={(v) => updateCustom({ size: v })} />
            <Row key={`a${resetKey}`} label="Transparency (opacity)" min={20} max={100} value={custom.alpha}
              format={(v) => `${v}%`} onCommit={(v) => updateCustom({ alpha: v })} />
            <Row key={`p${resetKey}`} label="Hidden off-screen" min={0} max={60} value={custom.peek}
              format={(v) => `${v}%`} onCommit={(v) => updateCustom({ peek: v })} />

            <Text style={{ fontSize: 16, marginTop: 20 }}>Screen edge</Text>
            <View style={{ flexDirection: 'row', gap: 8, marginTop: 8 }}>
              {SIDES.map((s) => (
                <Chip key={s} label={s === 'RIGHT' ? 'Right' : 'Left'} active={custom.side === s}
                  onPress={() => updateCustom({ side: s })} />
              ))}
            </View>

            <Pressable onPress={Scroller.showController}
              style={{ backgroundColor: '#ddd', padding: 14, borderRadius: 8, marginTop: 24 }}>
              <Text>Show controller</Text>
            </Pressable>
            <Pressable
              onPress={() => { setCustom(DEFAULT_CUSTOM); Scroller.setCustomize(DEFAULT_CUSTOM); setResetKey((k) => k + 1); }}
              style={{ backgroundColor: '#ddd', padding: 14, borderRadius: 8, marginTop: 10 }}>
              <Text>Reset to defaults</Text>
            </Pressable>
          </>
        )}
      </ScrollView>

      <Pressable onPress={() => Linking.openURL(GITHUB)} style={{ padding: 16, alignItems: 'center' }}>
        <Text style={{ color: '#1565C0', textDecorationLine: 'underline', fontSize: 13 }}>
          Made by Abhishek Jha  •  github.com/AbhishekJha3511
        </Text>
      </Pressable>
    </View>
  );
}
