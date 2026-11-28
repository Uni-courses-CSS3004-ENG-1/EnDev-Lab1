import { useMemo } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { GraduationCap, Search, SlidersHorizontal, Sparkles, X } from 'lucide-react';
import type { Role, TutorCard } from '../lib/types';
import { money, subjectIcon } from '../lib/format';
import { Avatar } from './ui';

export type Filters = { query: string; subject: string; maxPrice: number; sort: 'popular' | 'price-asc' | 'price-desc' };
export const NO_FILTERS: Filters = { query: '', subject: '', maxPrice: 0, sort: 'popular' };

type Props = {
  tutors: TutorCard[] | null;
  filters: Filters;
  onFilters: (filters: Filters) => void;
  viewerRole: Role | null;
  onOpen: (tutor: TutorCard, book: boolean) => void;
  title?: string;
};

export function Catalogue({ tutors, filters, onFilters, viewerRole, onOpen, title = 'Find your tutor' }: Props) {
  const subjects = useMemo(() => [...new Set((tutors ?? []).map((t) => t.subject))].sort(), [tutors]);
  const set = (patch: Partial<Filters>) => onFilters({ ...filters, ...patch });

  const shown = useMemo(() => {
    const q = filters.query.trim().toLowerCase();
    const list = (tutors ?? []).filter((t) =>
      (!filters.subject || t.subject === filters.subject) &&
      (!filters.maxPrice || t.hourlyRate <= filters.maxPrice) &&
      (!q || `${t.name} ${t.subject} ${t.bio}`.toLowerCase().includes(q)));
    const sorts = {
      popular: (a: TutorCard, b: TutorCard) => b.lessonsTaught - a.lessonsTaught || a.name.localeCompare(b.name),
      'price-asc': (a: TutorCard, b: TutorCard) => a.hourlyRate - b.hourlyRate,
      'price-desc': (a: TutorCard, b: TutorCard) => b.hourlyRate - a.hourlyRate,
    };
    return [...list].sort(sorts[filters.sort]);
  }, [tutors, filters]);

  const filtered = filters.query || filters.subject || filters.maxPrice;

  return (
    <section className="section" id="tutors">
      <div className="container">
        <div className="catalogue-head">
          <div>
            <h2 className="h2">{title}</h2>
            <p className="muted mt-8">
              {tutors === null ? 'Loading tutors…' : `${shown.length} tutor${shown.length === 1 ? '' : 's'} available`}
            </p>
          </div>
        </div>

        <div className="filters" role="search">
          <label className="field-inline grow">
            <Search size={18} aria-hidden="true" />
            <input value={filters.query} onChange={(e) => set({ query: e.target.value })}
                   placeholder="Search by name or keyword" aria-label="Search tutors" />
          </label>
          <label className="field-inline">
            <GraduationCap size={18} aria-hidden="true" />
            <select value={filters.subject} onChange={(e) => set({ subject: e.target.value })} aria-label="Subject">
              <option value="">All subjects</option>
              {subjects.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>
          <label className="field-inline">
            <span className="muted small">$</span>
            <select value={filters.maxPrice} onChange={(e) => set({ maxPrice: Number(e.target.value) })} aria-label="Price per hour">
              <option value={0}>Any price</option>
              <option value={20}>Up to $20</option>
              <option value={30}>Up to $30</option>
              <option value={40}>Up to $40</option>
            </select>
          </label>
          <label className="field-inline">
            <SlidersHorizontal size={18} aria-hidden="true" />
            <select value={filters.sort} onChange={(e) => set({ sort: e.target.value as Filters['sort'] })} aria-label="Sort by">
              <option value="popular">Most lessons taught</option>
              <option value="price-asc">Lowest price</option>
              <option value="price-desc">Highest price</option>
            </select>
          </label>
          {filtered ? (
            <button type="button" className="btn ghost sm" onClick={() => onFilters({ ...NO_FILTERS, sort: filters.sort })}>
              <X size={16} aria-hidden="true" /> Clear
            </button>
          ) : null}
        </div>

        {tutors === null ? (
          <div className="tutor-list">{[0, 1, 2].map((i) => <div key={i} className="tutor-row skeleton" />)}</div>
        ) : shown.length === 0 ? (
          <div className="empty">
            <Search size={28} aria-hidden="true" />
            <b>No tutors match your filters</b>
            <p className="muted">Try another subject or a higher price.</p>
            <button type="button" className="btn secondary" onClick={() => onFilters(NO_FILTERS)}>Clear filters</button>
          </div>
        ) : (
          <motion.div layout className="tutor-list">
            <AnimatePresence mode="popLayout" initial={false}>
              {shown.map((tutor, i) => (
                <TutorRow key={tutor.id} tutor={tutor} index={i} viewerRole={viewerRole} onOpen={onOpen} />
              ))}
            </AnimatePresence>
          </motion.div>
        )}
      </div>
    </section>
  );
}

function TutorRow({ tutor, index, viewerRole, onOpen }:
                    { tutor: TutorCard; index: number; viewerRole: Role | null; onOpen: Props['onOpen'] }) {
  const Icon = subjectIcon(tutor.subject);
  return (
    <motion.article layout className="tutor-row"
                    initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0, transition: { delay: Math.min(index, 6) * 0.04 } }}
                    exit={{ opacity: 0, scale: 0.98, transition: { duration: 0.15 } }}
                    transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
      <button type="button" className="tutor-photo" onClick={() => onOpen(tutor, false)} aria-label={`View ${tutor.name}`}>
        <Avatar name={tutor.name} size={112} square />
      </button>
      <div className="tutor-main">
        <div className="row gap-8 wrap">
          <button type="button" className="tutor-name" onClick={() => onOpen(tutor, false)}>{tutor.name}</button>
          {tutor.lessonsTaught === 0 && <span className="tag new"><Sparkles size={12} aria-hidden="true" /> New tutor</span>}
        </div>
        <p className="tutor-subject"><Icon size={16} aria-hidden="true" /> {tutor.subject}</p>
        <p className="tutor-meta muted small">
          {tutor.lessonsTaught > 0 ? `${tutor.lessonsTaught} lesson${tutor.lessonsTaught === 1 ? '' : 's'} taught` : 'Ready for first students'}
        </p>
        <p className="tutor-bio">{tutor.bio || `${tutor.name.split(' ')[0]} teaches ${tutor.subject}.`}</p>
      </div>
      <div className="tutor-side">
        <div>
          <b className="tutor-price">{money(tutor.hourlyRate)}</b>
          <span className="muted small">60-min lesson</span>
        </div>
        <div className="tutor-actions">
          {viewerRole !== 'TUTOR' && (
            <button type="button" className="btn accent block" onClick={() => onOpen(tutor, true)}>Book lesson</button>
          )}
          <button type="button" className="btn secondary block" onClick={() => onOpen(tutor, false)}>View profile</button>
        </div>
      </div>
    </motion.article>
  );
}
