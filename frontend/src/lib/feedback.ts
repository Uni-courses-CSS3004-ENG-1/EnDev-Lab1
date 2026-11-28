// Toasts and plain-language errors. People read sentences, not status codes; the correlation id
// still goes to the server logs on every call.
import { useSyncExternalStore } from 'react';
import { isError } from './api';
import type { ApiResult } from './types';

export type Tone = 'success' | 'error' | 'info';
export type Toast = { id: number; tone: Tone; message: string };

let toasts: Toast[] = [];
let nextId = 1;
const listeners = new Set<() => void>();
const emit = () => listeners.forEach((listener) => listener());

export function toast(tone: Tone, message: string) {
  const item = { id: nextId++, tone, message };
  toasts = [...toasts, item].slice(-3);
  emit();
  window.setTimeout(() => dismiss(item.id), tone === 'error' ? 6000 : 4000);
}

export function dismiss(id: number) {
  toasts = toasts.filter((t) => t.id !== id);
  emit();
}

export const useToasts = () => useSyncExternalStore(
  (listener) => { listeners.add(listener); return () => listeners.delete(listener); },
  () => toasts);

const FIELD: Record<string, string> = {
  username: 'Username', password: 'Password', name: 'Full name', email: 'Email', subject: 'Subject',
  hourlyRate: 'Price per hour', bio: 'About you', role: 'Account type', startsAt: 'Time', tutorId: 'Tutor',
};

// The server's message turned into something a person can act on.
export function friendly(res: ApiResult<unknown>): string {
  if (res.status === 0) return 'We can’t reach the server. Check your connection and try again.';
  const message = isError(res.data) ? res.data.message : '';
  const code = isError(res.data) ? res.data.code : '';

  if (res.status === 401) {
    return message === 'bad username or password' ? 'That email and password don’t match.' : 'Please log in to continue.';
  }
  if (message.startsWith('username already taken')) return 'That username is taken. Try another one.';
  if (message.startsWith('email already exists')) return 'An account with this email already exists. Try logging in.';
  if (code === 'SLOT_TAKEN') return 'That time was just booked. Please pick another one.';
  if (message === 'not your lesson') return 'This lesson belongs to someone else.';
  if (res.status === 403) return 'Your account can’t do that.';
  if (res.status === 404) return 'We couldn’t find that. It may have been removed.';
  if (code === 'INVALID_TRANSITION') return 'This lesson has already moved on. Refresh to see its status.';
  if (res.status === 400 && message) {
    if (message === 'subject is required for a tutor') return 'Please tell students what you teach.';
    if (message === 'hourlyRate is required for a tutor') return 'Please set your price per hour.';
    if (message.startsWith('startsAt must be a future')) return 'Please pick a time in the future.';
    if (message.startsWith('username must be')) return 'Username: 3–32 lowercase letters, digits, dots, dashes or underscores.';
    if (message.startsWith('password size')) return 'Password must be 8 to 72 characters long.';
    if (message.includes('well-formed email')) return 'Please enter a valid email address.';
    const [field, ...rest] = message.split(' ');
    const label = FIELD[field ?? ''];
    if (label && rest.join(' ').includes('blank')) return `${label} is required.`;
    if (label) return `${label}: ${rest.join(' ')}.`;
  }
  return 'Something went wrong on our side. Please try again.';
}
