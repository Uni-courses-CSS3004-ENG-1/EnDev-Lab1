import { useState, type ReactNode } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { BadgeCheck, CalendarClock, CalendarPlus, Check, Clock3, Inbox, Wallet, X } from 'lucide-react';
import { call } from '../lib/api';
import { friendly, toast } from '../lib/feedback';
import { countdown, greeting, money, when } from '../lib/format';
import type { Lesson, LessonDetails, Role, Status } from '../lib/types';
import { Avatar, ConfirmDialog, Spinner, StatusPill } from './ui';

type Verb = 'confirm' | 'complete' | 'cancel';
const ACTIVE: Status[] = ['REQUESTED', 'CONFIRMED'];

type Props = {
  role: Role;
  name: string;
  lessons: LessonDetails[] | null;
  onStatus: (id: string, status: Status) => void;
  onFind: () => void;
};

// One hook for both pages: run a status change, show a friendly result.
function useMoves(onStatus: Props['onStatus']) {
  const [busy, setBusy] = useState<string | null>(null);
  const [confirming, setConfirming] = useState<{ lesson: LessonDetails; verb: Verb } | null>(null);

  async function move(lesson: LessonDetails, verb: Verb) {
    setBusy(lesson.id + verb);
    const res = await call<Lesson>('POST', `/api/v1/lessons/${encodeURIComponent(lesson.id)}/${verb}`);
    setBusy(null);
    if (res.status === 200) {
      onStatus(lesson.id, (res.data as Lesson).status);
      const done: Record<Verb, string> = {
        confirm: 'Lesson confirmed. It’s on your schedule.',
        complete: 'Lesson marked as completed. The payment is on its way.',
        cancel: lesson.status === 'CONFIRMED' ? 'Lesson cancelled. The payment hold is refunded.' : 'Lesson cancelled.',
      };
      toast('success', done[verb]);
    } else {
      toast('error', friendly(res));
    }
  }
  return { busy, move, confirming, setConfirming };
}

function LessonRow({ lesson, role, actions }: { lesson: LessonDetails; role: Role; actions?: ReactNode }) {
  const w = when(lesson.startsAt);
  const other = role === 'TUTOR' ? lesson.studentName : lesson.tutorName;
  return (
    <motion.li layout className={'lesson-row' + (ACTIVE.includes(lesson.status) ? '' : ' faded')}
               initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
               exit={{ opacity: 0, x: -20, transition: { duration: 0.2 } }} transition={{ duration: 0.35 }}>
      <div className="date-block">
        {w ? <><small>{w.month}</small><b>{w.day}</b><small>{w.weekday}</small></> : <b>—</b>}
      </div>
      <div className="lesson-main">
        <h4>{lesson.subject}</h4>
        <p className="muted small">
          {w ? <><Clock3 size={14} aria-hidden="true" /> {w.range}</> : 'No time set'}
        </p>
      </div>
      {other && other !== 'UNASSIGNED' && (
        <div className="lesson-person">
          <Avatar name={other} size={32} />
          <span className="small">{other}</span>
        </div>
      )}
      <div className="lesson-status">
        <StatusPill status={lesson.status} role={role} />
        {lesson.hourlyRate != null && <span className="muted small">{money(lesson.hourlyRate)}</span>}
      </div>
      {actions && <div className="lesson-actions">{actions}</div>}
    </motion.li>
  );
}

// ---------------- student ----------------

export function StudentLessons({ name, lessons, onStatus, onFind }: Props) {
  const { busy, move, confirming, setConfirming } = useMoves(onStatus);
  const [tab, setTab] = useState<'upcoming' | 'past'>('upcoming');
  const all = lessons ?? [];
  const upcoming = all.filter((l) => ACTIVE.includes(l.status));
  const past = all.filter((l) => !ACTIVE.includes(l.status));
  const next = upcoming.find((l) => l.startsAt && !when(l.startsAt)!.past) ?? upcoming[0];
  const list = tab === 'upcoming' ? upcoming.filter((l) => l !== next) : past;

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <p className="muted">{greeting()}, {name.split(' ')[0]}</p>
          <h1 className="h1">My lessons</h1>
        </div>
        <button type="button" className="btn accent" onClick={onFind}><CalendarPlus size={18} aria-hidden="true" /> Book a lesson</button>
      </div>

      {lessons === null ? <div className="center pad"><Spinner /></div> : all.length === 0 ? (
        <div className="empty big">
          <span className="empty-icon"><CalendarPlus size={28} aria-hidden="true" /></span>
          <b>No lessons yet</b>
          <p className="muted">Find a tutor you like and book your first hour. You pay only once they confirm.</p>
          <button type="button" className="btn accent" onClick={onFind}>Find a tutor</button>
        </div>
      ) : (
        <>
          {next && tab === 'upcoming' && (
            <motion.section className="next-card" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
              <div className="next-left">
                <span className="next-label">{next.status === 'CONFIRMED' ? 'Your next lesson' : 'Waiting for your tutor'}</span>
                <h2 className="h2">{next.subject}</h2>
                <p className="next-when">{when(next.startsAt)?.date ?? 'Time to be agreed'} · {when(next.startsAt)?.range}</p>
                <div className="row gap-12 mt-16 wrap">
                  <span className="next-person"><Avatar name={next.tutorName ?? 'Tutor'} size={36} /> {next.tutorName}</span>
                  <StatusPill status={next.status} role="STUDENT" />
                </div>
              </div>
              <div className="next-right">
                <span className="next-countdown">{countdown(next.startsAt)}</span>
                <button type="button" className="btn secondary" onClick={() => setConfirming({ lesson: next, verb: 'cancel' })}>
                  Cancel lesson
                </button>
              </div>
            </motion.section>
          )}

          <div className="tabs" role="tablist">
            {(['upcoming', 'past'] as const).map((t) => (
              <button key={t} type="button" role="tab" aria-selected={tab === t} className={'tab' + (tab === t ? ' active' : '')}
                      onClick={() => setTab(t)}>
                {t === 'upcoming' ? 'Upcoming' : 'Past'} <span className="count">{t === 'upcoming' ? upcoming.length : past.length}</span>
                {tab === t && <motion.span layoutId="tab-line" className="tab-line" />}
              </button>
            ))}
          </div>

          {list.length === 0 ? (
            <p className="muted pad center">{tab === 'upcoming' ? 'Nothing else scheduled.' : 'No past lessons yet.'}</p>
          ) : (
            <ul className="lesson-list">
              <AnimatePresence initial={false}>
                {list.map((lesson) => (
                  <LessonRow key={lesson.id} lesson={lesson} role="STUDENT" actions={ACTIVE.includes(lesson.status) && (
                    <button type="button" className="btn ghost sm" disabled={busy === lesson.id + 'cancel'}
                            onClick={() => setConfirming({ lesson, verb: 'cancel' })}>Cancel</button>
                  )} />
                ))}
              </AnimatePresence>
            </ul>
          )}
        </>
      )}

      <ConfirmDialog open={!!confirming} title="Cancel this lesson?"
                     text={confirming?.lesson.status === 'CONFIRMED'
                       ? 'Your tutor will be told, and the payment hold will be refunded.'
                       : 'Your tutor will no longer see this request.'}
                     confirmLabel="Cancel lesson" onClose={() => setConfirming(null)}
                     onConfirm={async () => { await move(confirming!.lesson, 'cancel'); setConfirming(null); }} />
    </div>
  );
}

// ---------------- tutor ----------------

export function TutorDashboard({ name, lessons, onStatus }: Props) {
  const { busy, move, confirming, setConfirming } = useMoves(onStatus);
  const all = lessons ?? [];
  const requests = all.filter((l) => l.status === 'REQUESTED');
  const schedule = all.filter((l) => l.status === 'CONFIRMED');
  const history = all.filter((l) => !ACTIVE.includes(l.status));
  const sum = (list: LessonDetails[]) => list.reduce((total, l) => total + (l.hourlyRate ?? 0), 0);
  const completed = all.filter((l) => l.status === 'COMPLETED');

  const stats = [
    { icon: Inbox, label: 'New requests', value: String(requests.length), tone: 'orange' },
    { icon: CalendarClock, label: 'Upcoming lessons', value: String(schedule.length), tone: 'blue' },
    { icon: Wallet, label: 'Awaiting payout', value: money(sum(schedule)), tone: 'amber' },
    { icon: BadgeCheck, label: 'Total earned', value: money(sum(completed)), tone: 'green' },
  ];

  const spinner = (lesson: LessonDetails, verb: Verb, label: ReactNode) =>
    busy === lesson.id + verb ? <Spinner light={verb !== 'cancel'} /> : label;

  return (
    <div className="container page">
      <div className="page-head">
        <div>
          <p className="muted">{greeting()}, {name.split(' ')[0]}</p>
          <h1 className="h1">Dashboard</h1>
        </div>
      </div>

      <div className="stats">
        {stats.map((s, i) => (
          <motion.div key={s.label} className={'stat ' + s.tone} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.05 }}>
            <span className="stat-icon"><s.icon size={20} aria-hidden="true" /></span>
            <b>{s.value}</b>
            <span className="muted small">{s.label}</span>
          </motion.div>
        ))}
      </div>

      {lessons === null ? <div className="center pad"><Spinner /></div> : (
        <div className="dash-grid">
          <section>
            <h2 className="h4 section-title">Lesson requests {requests.length > 0 && <span className="badge-count">{requests.length}</span>}</h2>
            {requests.length === 0 ? (
              <div className="empty slim"><Inbox size={22} aria-hidden="true" /><p className="muted">No new requests. They’ll appear here as soon as a student books you.</p></div>
            ) : (
              <ul className="lesson-list">
                <AnimatePresence initial={false}>
                  {requests.map((lesson) => (
                    <LessonRow key={lesson.id} lesson={lesson} role="TUTOR" actions={<>
                      <button type="button" className="btn accent sm" disabled={!!busy} onClick={() => move(lesson, 'confirm')}>
                        {spinner(lesson, 'confirm', <><Check size={16} aria-hidden="true" /> Accept</>)}
                      </button>
                      <button type="button" className="btn ghost sm" disabled={!!busy} onClick={() => setConfirming({ lesson, verb: 'cancel' })}>
                        <X size={16} aria-hidden="true" /> Decline
                      </button>
                    </>} />
                  ))}
                </AnimatePresence>
              </ul>
            )}

            <h2 className="h4 section-title mt-32">Upcoming schedule</h2>
            {schedule.length === 0 ? (
              <div className="empty slim"><CalendarClock size={22} aria-hidden="true" /><p className="muted">Nothing scheduled yet. Accepted requests show up here.</p></div>
            ) : (
              <ul className="lesson-list">
                <AnimatePresence initial={false}>
                  {schedule.map((lesson) => (
                    <LessonRow key={lesson.id} lesson={lesson} role="TUTOR" actions={<>
                      <button type="button" className="btn primary sm" disabled={!!busy} onClick={() => move(lesson, 'complete')}>
                        {spinner(lesson, 'complete', 'Mark completed')}
                      </button>
                      <button type="button" className="btn ghost sm" disabled={!!busy} onClick={() => setConfirming({ lesson, verb: 'cancel' })}>Cancel</button>
                    </>} />
                  ))}
                </AnimatePresence>
              </ul>
            )}
          </section>

          <aside>
            <h2 className="h4 section-title">History</h2>
            {history.length === 0 ? <p className="muted small">Completed and cancelled lessons will be listed here.</p> : (
              <ul className="history">
                {history.map((lesson) => (
                  <li key={lesson.id}>
                    <div>
                      <b className="small">{lesson.subject}</b>
                      <span className="muted small">{lesson.studentName ?? '—'} · {when(lesson.startsAt)?.date ?? ''}</span>
                    </div>
                    <StatusPill status={lesson.status} role="TUTOR" />
                  </li>
                ))}
              </ul>
            )}
          </aside>
        </div>
      )}

      <ConfirmDialog open={!!confirming}
                     title={confirming?.lesson.status === 'REQUESTED' ? 'Decline this request?' : 'Cancel this lesson?'}
                     text={confirming?.lesson.status === 'REQUESTED'
                       ? 'The student will see that you can’t take this lesson.'
                       : 'The student will be told, and their payment hold will be refunded.'}
                     confirmLabel={confirming?.lesson.status === 'REQUESTED' ? 'Decline' : 'Cancel lesson'}
                     onClose={() => setConfirming(null)}
                     onConfirm={async () => { await move(confirming!.lesson, 'cancel'); setConfirming(null); }} />
    </div>
  );
}
