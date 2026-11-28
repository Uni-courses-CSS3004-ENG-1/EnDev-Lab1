import { useMemo, useState, type FormEvent } from 'react';
import { motion } from 'motion/react';
import { ArrowRight, CalendarCheck2, CheckCircle2, CreditCard, Search, Sparkles, UserRoundSearch } from 'lucide-react';
import type { TutorCard } from '../lib/types';
import { money, subjectIcon } from '../lib/format';
import { Avatar, Logo, Reveal } from './ui';

const ease = [0.16, 1, 0.3, 1] as const;

export function Hero({ tutors, onSearch }: { tutors: TutorCard[] | null; onSearch: (subject: string) => void }) {
  const subjects = useMemo(() => [...new Set((tutors ?? []).map((t) => t.subject))].sort(), [tutors]);
  const [subject, setSubject] = useState('');
  const featured = useMemo(() => [...(tutors ?? [])].sort((a, b) => b.lessonsTaught - a.lessonsTaught)[0], [tutors]);

  function submit(event: FormEvent) {
    event.preventDefault();
    onSearch(subject);
  }

  return (
    <section className="hero">
      <div className="container hero-grid">
        <div>
          <motion.span className="eyebrow" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, ease }}>
            <Sparkles size={14} aria-hidden="true" /> One-to-one online lessons
          </motion.span>
          <motion.h1 className="display" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }}
                     transition={{ duration: 0.7, delay: 0.05, ease }}>
            Learn faster with <span className="accent-word">your perfect
              <svg viewBox="0 0 300 18" preserveAspectRatio="none" aria-hidden="true"><path d="M3 13 C 80 3, 200 3, 297 11" /></svg>
            </span> tutor
          </motion.h1>
          <motion.p className="lead" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.7, delay: 0.12, ease }}>
            Choose from experienced tutors, book an hour that fits your week, and pay only when your tutor confirms.
          </motion.p>
          <motion.form className="hero-search" onSubmit={submit} role="search"
                       initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.18, ease }}>
            <label className="hero-search-field">
              <Search size={20} aria-hidden="true" />
              <select value={subject} onChange={(e) => setSubject(e.target.value)} aria-label="What do you want to learn?">
                <option value="">What do you want to learn?</option>
                {subjects.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>
            <button type="submit" className="btn accent lg">Find a tutor <ArrowRight size={18} aria-hidden="true" /></button>
          </motion.form>
          <motion.ul className="hero-points" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.8, delay: 0.3 }}>
            <li><CheckCircle2 size={18} aria-hidden="true" /> Pay only after your tutor confirms</li>
            <li><CheckCircle2 size={18} aria-hidden="true" /> Cancel before the lesson, no questions asked</li>
          </motion.ul>
        </div>

        <motion.div className="hero-visual" aria-hidden="true" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }}
                    transition={{ duration: 0.9, delay: 0.1, ease }}>
          <div className="hero-panel">
            <div className="hero-orb a" />
            <div className="hero-orb b" />
            {featured && (
              <motion.div className="preview-card" animate={{ y: [0, -8, 0] }} transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}>
                <div className="row gap-12">
                  <Avatar name={featured.name} size={56} square />
                  <div>
                    <b className="preview-name">{featured.name}</b>
                    <span className="muted small">{featured.subject} tutor</span>
                  </div>
                </div>
                <p className="preview-bio">{featured.bio}</p>
                <div className="row between">
                  <span><b className="preview-price">{money(featured.hourlyRate)}</b> <span className="muted small">/ hour</span></span>
                  <span className="btn accent sm">Book lesson</span>
                </div>
              </motion.div>
            )}
            <motion.div className="float-chip one" initial={{ opacity: 0, x: 30 }} animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.7, duration: 0.6, ease }}>
              <span className="chip-icon green"><CalendarCheck2 size={18} /></span>
              <span><b>Lesson confirmed</b><small>Tomorrow · 16:00 – 17:00</small></span>
            </motion.div>
            <motion.div className="float-chip two" initial={{ opacity: 0, x: -30 }} animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.95, duration: 0.6, ease }}>
              <span className="chip-icon orange"><CreditCard size={18} /></span>
              <span><b>No charge yet</b><small>Paid only once confirmed</small></span>
            </motion.div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}

export function Subjects({ tutors, onPick }: { tutors: TutorCard[] | null; onPick: (subject: string) => void }) {
  const subjects = useMemo(() => {
    const counts = new Map<string, number>();
    (tutors ?? []).forEach((t) => counts.set(t.subject, (counts.get(t.subject) ?? 0) + 1));
    return [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).slice(0, 8);
  }, [tutors]);
  if (subjects.length === 0) return null;
  return (
    <section className="section">
      <div className="container">
        <Reveal>
          <h2 className="h2">Popular subjects</h2>
          <p className="muted mt-8">Start with what you need most. Every tutor teaches one hour at a time.</p>
        </Reveal>
        <div className="subject-grid">
          {subjects.map(([subject, count], i) => {
            const Icon = subjectIcon(subject);
            return (
              <Reveal key={subject} delay={i * 0.04}>
                <button type="button" className="subject-tile" onClick={() => onPick(subject)}>
                  <span className="subject-icon"><Icon size={22} aria-hidden="true" /></span>
                  <span>
                    <b>{subject}</b>
                    <small>{count} tutor{count > 1 ? 's' : ''}</small>
                  </span>
                  <ArrowRight size={18} className="subject-arrow" aria-hidden="true" />
                </button>
              </Reveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}

export function HowItWorks() {
  const steps = [
    { icon: UserRoundSearch, title: 'Find your tutor', text: 'Compare tutors by subject, price and experience, and read what they teach.' },
    { icon: CalendarCheck2, title: 'Book an hour', text: 'Pick a day and a time. Your tutor gets the request straight away.' },
    { icon: CreditCard, title: 'Pay when it’s confirmed', text: 'The payment is held only once your tutor confirms, and released after the lesson.' },
  ];
  return (
    <section className="section soft" id="how">
      <div className="container">
        <Reveal className="center-head">
          <h2 className="h2">How it works</h2>
          <p className="muted mt-8">Three steps from “I need help” to your first lesson.</p>
        </Reveal>
        <div className="steps">
          {steps.map((step, i) => (
            <Reveal key={step.title} delay={i * 0.08} className="step">
              <span className="step-num">{i + 1}</span>
              <span className="step-icon"><step.icon size={26} aria-hidden="true" /></span>
              <h3 className="h4">{step.title}</h3>
              <p className="muted">{step.text}</p>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

export function TeachBanner({ onTeach }: { onTeach: () => void }) {
  return (
    <section className="section">
      <div className="container">
        <Reveal className="teach">
          <div>
            <h2 className="h2">Become a tutor</h2>
            <p>Share what you know, set your own price and choose the requests you accept.
              You confirm every lesson before anyone pays.</p>
            <ul className="teach-points">
              <li><CheckCircle2 size={18} aria-hidden="true" /> Set your own hourly rate</li>
              <li><CheckCircle2 size={18} aria-hidden="true" /> Accept only the lessons you want</li>
              <li><CheckCircle2 size={18} aria-hidden="true" /> Get paid after every lesson</li>
            </ul>
            <button type="button" className="btn accent lg" onClick={onTeach}>Start teaching <ArrowRight size={18} /></button>
          </div>
          <div className="teach-art" aria-hidden="true">
            <div className="teach-stat"><b>Your price</b><span>You decide</span></div>
            <div className="teach-stat"><b>Your hours</b><span>Accept what fits</span></div>
            <div className="teach-stat"><b>Your students</b><span>One to one</span></div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

export function Footer({ onNavigate }: { onNavigate: (anchor: string) => void }) {
  return (
    <footer className="footer">
      <div className="container footer-grid">
        <div>
          <Logo />
          <p className="muted small mt-12">One-to-one lessons with tutors who love what they teach.</p>
        </div>
        <div>
          <b>Students</b>
          <button type="button" onClick={() => onNavigate('tutors')}>Find a tutor</button>
          <button type="button" onClick={() => onNavigate('how')}>How it works</button>
        </div>
        <div>
          <b>Developers</b>
          <a href="/swagger-ui.html">API documentation</a>
          <a href="/actuator/health">Service status</a>
        </div>
      </div>
      <div className="container footer-bottom muted small">© {new Date().getFullYear()} Tutoring marketplace</div>
    </footer>
  );
}
