import {
  Atom, BarChart3, BookOpen, Calculator, Code2, FlaskConical, Globe2, Languages, Music, Palette,
  type LucideIcon,
} from 'lucide-react';
import type { Role, Status } from './types';

// What a person reads for each status, from their side of the lesson.
export function statusLabel(status: Status, role: Role) {
  if (status === 'REQUESTED') return role === 'TUTOR' ? 'New request' : 'Awaiting confirmation';
  if (status === 'CONFIRMED') return 'Confirmed';
  if (status === 'COMPLETED') return 'Completed';
  return 'Cancelled';
}

export function money(value: number | null | undefined) {
  if (value == null) return '—';
  const n = Number(value);
  return '$' + (Number.isInteger(n) ? n.toFixed(0) : n.toFixed(2));
}

export function initials(name: string) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]!.toUpperCase()).join('');
}

// the same name always gets the same gradient
const GRADIENTS = [
  ['#FF8A65', '#FF5252'], ['#7C8CFF', '#5B5BF0'], ['#34D399', '#0EA371'], ['#FBBF24', '#F97316'],
  ['#F472B6', '#DB2777'], ['#38BDF8', '#2563EB'], ['#A78BFA', '#7C3AED'], ['#2DD4BF', '#0D9488'],
];
export function gradient(seed: string) {
  let hash = 0;
  for (const ch of seed) hash = (hash * 31 + ch.charCodeAt(0)) | 0;
  const [a, b] = GRADIENTS[Math.abs(hash) % GRADIENTS.length]!;
  return `linear-gradient(135deg, ${a}, ${b})`;
}

const SUBJECT_ICONS: [RegExp, LucideIcon][] = [
  [/alg|math|calc|geom/i, Calculator],
  [/phys/i, Atom],
  [/chem|bio/i, FlaskConical],
  [/stat|data|econ/i, BarChart3],
  [/program|code|java|python|comput/i, Code2],
  [/engl|kazakh|russ|french|german|spanish|chinese|language/i, Languages],
  [/geo|hist/i, Globe2],
  [/music|piano|guitar/i, Music],
  [/art|draw|design/i, Palette],
];
export function subjectIcon(subject: string): LucideIcon {
  return SUBJECT_ICONS.find(([test]) => test.test(subject))?.[1] ?? BookOpen;
}

export function when(iso: string | null) {
  if (!iso) return null;
  const d = new Date(iso);
  const end = new Date(d.getTime() + 3600_000);
  const time = (x: Date) => x.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  return {
    weekday: d.toLocaleDateString(undefined, { weekday: 'short' }),
    day: d.getDate(),
    month: d.toLocaleDateString(undefined, { month: 'short' }),
    date: d.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' }),
    range: `${time(d)} – ${time(end)}`,
    past: end.getTime() < Date.now(),
  };
}

export function countdown(iso: string | null) {
  if (!iso) return '';
  const diff = new Date(iso).getTime() - Date.now();
  if (diff < 0) return diff > -3600_000 ? 'Happening now' : 'Past';
  const minutes = Math.round(diff / 60000);
  if (minutes < 60) return `Starts in ${minutes} min`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `Starts in ${hours} h`;
  const days = Math.round(hours / 24);
  return days === 1 ? 'Tomorrow' : `In ${days} days`;
}

export function greeting() {
  const h = new Date().getHours();
  return h < 5 ? 'Good evening' : h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
}
