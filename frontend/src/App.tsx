import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { call, getSession, logout, restoreSession, subscribeSession, takeEndedMessage } from './lib/api';
import { toast } from './lib/feedback';
import type { LessonDetails, Status, TutorCard } from './lib/types';
import { Header, type Page } from './components/Header';
import { Footer, Hero, HowItWorks, Subjects, TeachBanner } from './components/Landing';
import { Catalogue, NO_FILTERS, type Filters } from './components/Catalogue';
import { TutorDrawer } from './components/TutorDrawer';
import { AuthModal, type AuthIntent } from './components/AuthModal';
import { StudentLessons, TutorDashboard } from './components/Lessons';
import { ProfilePage } from './components/ProfilePage';
import { Logo, Toaster } from './components/ui';

export function App() {
  const session = useSyncExternalStore(subscribeSession, getSession);
  const account = session.account;
  const role = account?.role ?? null;

  const [dark, setDark] = useState(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  const [page, setPage] = useState<Page>('home');
  const [tutors, setTutors] = useState<TutorCard[] | null>(null);
  const [lessons, setLessons] = useState<LessonDetails[] | null>(null);
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [drawer, setDrawer] = useState<{ tutor: TutorCard; book: boolean } | null>(null);
  const [auth, setAuth] = useState<AuthIntent | null>(null);
  const pendingTutor = useRef<TutorCard | null>(null);

  useEffect(() => { document.documentElement.dataset.theme = dark ? 'dark' : 'light'; }, [dark]);

  const loadTutors = useCallback(async () => {
    const res = await call<TutorCard[]>('GET', '/api/v1/tutors');
    setTutors(res.status === 200 ? (res.data as TutorCard[]) : []);
  }, []);

  const loadLessons = useCallback(async () => {
    const res = await call<LessonDetails[]>('GET', '/api/v1/lessons/mine');
    if (res.status === 200) setLessons(res.data as LessonDetails[]);
  }, []);

  // first visit: the catalogue for everyone, and a silent sign-in from the refresh cookie
  useEffect(() => {
    void loadTutors();
    void restoreSession();
  }, [loadTutors]);

  useEffect(() => {
    if (session.status === 'signedIn') {
      void loadLessons();
    } else if (session.status === 'signedOut') {
      setLessons(null);
      setPage('home');
      const ended = takeEndedMessage();
      if (ended) toast('info', ended);
    }
  }, [session.status, loadLessons]);

  const subjects = useMemo(() => [...new Set((tutors ?? []).map((t) => t.subject))].sort(), [tutors]);

  function navigate(next: Page, anchor?: string) {
    setPage(next);
    requestAnimationFrame(() => {
      const target = anchor ? document.getElementById(anchor) : null;
      if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      else window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  function signedIn() {
    setAuth(null);
    const now = getSession();
    const myRole = now.account?.role;
    toast('success', `Welcome, ${now.account?.name.split(' ')[0] ?? now.me?.username}!`);
    void loadTutors();
    if (pendingTutor.current && myRole === 'STUDENT') {
      setDrawer({ tutor: pendingTutor.current, book: true });
      navigate('home', 'tutors');
    } else {
      navigate(myRole === 'TUTOR' ? 'lessons' : 'home');
    }
    pendingTutor.current = null;
  }

  function needAccount(tutor: TutorCard) {
    pendingTutor.current = tutor;
    setDrawer(null);
    setAuth({ mode: 'signup', role: 'STUDENT', reason: `Create a free account to book ${tutor.name.split(' ')[0]}. It takes a minute.` });
  }

  function booked(lesson: LessonDetails) {
    setDrawer(null);
    setLessons((list) => [lesson, ...(list ?? [])]);
    navigate('lessons');
  }

  function changed(id: string, status: Status) {
    setLessons((list) => list?.map((l) => (l.id === id ? { ...l, status } : l)) ?? list);
    if (status === 'COMPLETED') void loadTutors();
  }

  async function signOut() {
    await logout();
    toast('info', 'You’ve been logged out.');
  }

  if (session.status === 'loading') {
    return (
      <div className="splash">
        <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.4 }}>
          <Logo />
        </motion.div>
      </div>
    );
  }

  const signedIn_ = session.status === 'signedIn' && account;
  const pickSubject = (subject: string) => { setFilters({ ...NO_FILTERS, subject }); navigate('home', 'tutors'); };

  return (
    <>
      <a className="skip" href="#main">Skip to content</a>
      <Header session={session} page={page} onNavigate={navigate} dark={dark} onTheme={() => setDark(!dark)}
              onAuth={(mode, r) => setAuth({ mode, role: r })} onLogout={signOut} />

      <main id="main">
        <AnimatePresence mode="wait">
          <motion.div key={(signedIn_ ? 'in-' : 'out-') + page} initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }} transition={{ duration: 0.2 }}>
            {page === 'home' && !signedIn_ && (
              <>
                <Hero tutors={tutors} onSearch={pickSubject} />
                <Subjects tutors={tutors} onPick={pickSubject} />
                <Catalogue tutors={tutors} filters={filters} onFilters={setFilters} viewerRole={null}
                           onOpen={(tutor, book) => setDrawer({ tutor, book })} />
                <HowItWorks />
                <TeachBanner onTeach={() => setAuth({ mode: 'signup', role: 'TUTOR' })} />
              </>
            )}
            {page === 'home' && signedIn_ && (
              <div className="page-top">
                <Catalogue tutors={tutors} filters={filters} onFilters={setFilters} viewerRole={role}
                           title={role === 'TUTOR' ? 'Tutors on the marketplace' : 'Find your tutor'}
                           onOpen={(tutor, book) => setDrawer({ tutor, book })} />
              </div>
            )}
            {page === 'lessons' && signedIn_ && (role === 'TUTOR'
              ? <TutorDashboard role="TUTOR" name={account.name} lessons={lessons} onStatus={changed} onFind={() => navigate('home', 'tutors')} />
              : <StudentLessons role="STUDENT" name={account.name} lessons={lessons} onStatus={changed} onFind={() => navigate('home', 'tutors')} />)}
            {page === 'profile' && signedIn_ && <ProfilePage account={account} onLogout={signOut} />}
          </motion.div>
        </AnimatePresence>
      </main>

      <Footer onNavigate={(anchor) => navigate('home', anchor)} />

      <TutorDrawer tutor={drawer?.tutor ?? null} focusBooking={drawer?.book ?? false} viewerRole={role}
                   myLessons={lessons ?? []} onClose={() => setDrawer(null)} onNeedAccount={needAccount} onBooked={booked} />
      <AuthModal intent={auth} subjects={subjects} onSignedIn={signedIn}
                 onClose={() => { setAuth(null); pendingTutor.current = null; }} />
      <Toaster />
    </>
  );
}
