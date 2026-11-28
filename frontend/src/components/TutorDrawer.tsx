import { useEffect, useMemo, useRef, useState } from 'react';
import { motion } from 'motion/react';
import { CalendarDays, CheckCircle2, ChevronLeft, ChevronRight, Clock3, ShieldCheck } from 'lucide-react';
import { call } from '../lib/api';
import { friendly, toast } from '../lib/feedback';
import { money, subjectIcon } from '../lib/format';
import type { LessonDetails, Role, TutorCard } from '../lib/types';
import { Avatar, Modal, Spinner } from './ui';

const HOURS = Array.from({ length: 14 }, (_, i) => 8 + i); // 08:00 .. 21:00
const DAYS_SHOWN = 7;

function startOfDay(offset: number) {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + offset);
  return d;
}

type Props = {
  tutor: TutorCard | null;
  focusBooking: boolean;
  viewerRole: Role | null;
  myLessons: LessonDetails[];
  onClose: () => void;
  onNeedAccount: (tutor: TutorCard) => void;
  onBooked: (lesson: LessonDetails) => void;
};

// The tutor's profile and, below it, a week of hours to book: like a tutor page on a marketplace.
export function TutorDrawer({ tutor, focusBooking, viewerRole, myLessons, onClose, onNeedAccount, onBooked }: Props) {
  const [week, setWeek] = useState(0);
  const [day, setDay] = useState(1);
  const [hour, setHour] = useState<number | null>(null);
  const [topic, setTopic] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [taken, setTaken] = useState<Set<number>>(new Set());
  const bookingRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!tutor) return;
    setWeek(0); setDay(1); setHour(null); setError(null); setTaken(new Set()); setTopic(tutor.subject);
    if (focusBooking) window.setTimeout(() => bookingRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 250);
  }, [tutor, focusBooking]);

  // hours I already booked with this tutor are shown as taken; the server checks everyone's
  const mine = useMemo(() => new Set(myLessons
    .filter((l) => l.tutorId === tutor?.id && (l.status === 'REQUESTED' || l.status === 'CONFIRMED') && l.startsAt)
    .map((l) => new Date(l.startsAt!).getTime())), [myLessons, tutor]);

  if (!tutor) return <Modal open={false} onClose={onClose} labelledBy="tutor-name" side>{null}</Modal>;

  const Icon = subjectIcon(tutor.subject);
  const firstDay = week * DAYS_SHOWN;
  const start = hour === null ? null : new Date(startOfDay(day).getTime() + hour * 3600_000);
  const canBook = viewerRole !== 'TUTOR';

  async function book() {
    if (!tutor || !start) return;
    if (viewerRole === null) { onNeedAccount(tutor); return; }
    setBusy(true);
    setError(null);
    const res = await call<LessonDetails>('POST', '/api/v1/lessons/book', {
      tutorId: tutor.id, subject: topic.trim() || tutor.subject, startsAt: start.toISOString(),
    });
    setBusy(false);
    if (res.status === 201) {
      toast('success', `Request sent to ${tutor.name.split(' ')[0]}. You’ll pay only once it’s confirmed.`);
      onBooked(res.data as LessonDetails);
      return;
    }
    if (res.status === 409) { setTaken(new Set([...taken, start.getTime()])); setHour(null); }
    setError(friendly(res));
  }

  return (
    <Modal open onClose={onClose} labelledBy="tutor-name" side>
      <div className="drawer-hero">
        <Avatar name={tutor.name} size={88} square />
        <div>
          <h2 className="h3" id="tutor-name">{tutor.name}</h2>
          <p className="tutor-subject"><Icon size={16} aria-hidden="true" /> {tutor.subject}</p>
          <p className="muted small">{tutor.lessonsTaught > 0
            ? `${tutor.lessonsTaught} lesson${tutor.lessonsTaught === 1 ? '' : 's'} taught here` : 'New on the marketplace'}</p>
        </div>
      </div>

      <div className="drawer-facts">
        <div><b>{money(tutor.hourlyRate)}</b><span>per 60-min lesson</span></div>
        <div><b>{tutor.lessonsTaught}</b><span>lessons taught</span></div>
        <div><b>1:1</b><span>online</span></div>
      </div>

      <h3 className="h5 mt-24">About me</h3>
      <p className="drawer-bio">{tutor.bio || `${tutor.name.split(' ')[0]} hasn’t written an introduction yet.`}</p>

      {canBook && (
        <div className="booking" ref={bookingRef}>
          <div className="row between">
            <h3 className="h5"><CalendarDays size={18} aria-hidden="true" /> Book a lesson</h3>
            <div className="row gap-4">
              <button type="button" className="icon-btn sm" disabled={week === 0} aria-label="Previous week"
                      onClick={() => { setWeek(week - 1); setDay(Math.max(0, (week - 1) * DAYS_SHOWN)); setHour(null); }}>
                <ChevronLeft size={18} />
              </button>
              <button type="button" className="icon-btn sm" disabled={week === 3} aria-label="Next week"
                      onClick={() => { setWeek(week + 1); setDay((week + 1) * DAYS_SHOWN); setHour(null); }}>
                <ChevronRight size={18} />
              </button>
            </div>
          </div>

          <div className="days" role="listbox" aria-label="Day">
            {Array.from({ length: DAYS_SHOWN }, (_, i) => {
              const offset = firstDay + i;
              const d = startOfDay(offset);
              const active = offset === day;
              return (
                <button key={offset} type="button" role="option" aria-selected={active}
                        className={'day' + (active ? ' active' : '')} onClick={() => { setDay(offset); setHour(null); }}>
                  <small>{offset === 0 ? 'Today' : d.toLocaleDateString(undefined, { weekday: 'short' })}</small>
                  <b>{d.getDate()}</b>
                  <small>{d.toLocaleDateString(undefined, { month: 'short' })}</small>
                </button>
              );
            })}
          </div>

          <div className="slots" aria-label="Start time">
            {HOURS.map((h) => {
              const at = startOfDay(day).getTime() + h * 3600_000;
              const unavailable = at <= Date.now() + 30 * 60_000 || mine.has(at) || taken.has(at);
              return (
                <button key={h} type="button" disabled={unavailable} className={'slot' + (hour === h ? ' active' : '')}
                        onClick={() => setHour(h)}>
                  {String(h).padStart(2, '0')}:00
                </button>
              );
            })}
          </div>

          <label className="field mt-16">
            <span>What would you like to focus on? <span className="muted">(optional)</span></span>
            <input value={topic} onChange={(e) => setTopic(e.target.value)} maxLength={120} />
          </label>

          {error && <p className="form-error" role="alert">{error}</p>}

          <motion.div className="book-bar" layout>
            <div>
              {start ? (
                <>
                  <b>{start.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })}</b>
                  <span className="muted small"><Clock3 size={14} aria-hidden="true" /> {String(hour).padStart(2, '0')}:00 – {String(hour! + 1).padStart(2, '0')}:00 · {money(tutor.hourlyRate)}</span>
                </>
              ) : <span className="muted">Pick a time to continue</span>}
            </div>
            <button type="button" className="btn accent lg" disabled={!start || busy} onClick={book}>
              {busy ? <Spinner light /> : viewerRole === null ? 'Sign up to book' : 'Request lesson'}
            </button>
          </motion.div>
          <p className="assurance"><ShieldCheck size={16} aria-hidden="true" /> No charge until {tutor.name.split(' ')[0]} confirms. Cancel any time before.</p>
        </div>
      )}
      {!canBook && (
        <p className="note mt-24"><CheckCircle2 size={16} aria-hidden="true" /> This is how students see this tutor’s profile.</p>
      )}
    </Modal>
  );
}
