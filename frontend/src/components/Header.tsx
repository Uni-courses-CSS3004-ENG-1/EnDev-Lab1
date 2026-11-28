import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { ChevronDown, LogOut, Moon, Sun, UserRound } from 'lucide-react';
import type { Session } from '../lib/api';
import { Avatar, Logo } from './ui';

export type Page = 'home' | 'lessons' | 'profile';

type Props = {
  session: Session;
  page: Page;
  onNavigate: (page: Page, anchor?: string) => void;
  onAuth: (mode: 'login' | 'signup', role?: 'STUDENT' | 'TUTOR') => void;
  onLogout: () => void;
  dark: boolean;
  onTheme: () => void;
};

export function Header({ session, page, onNavigate, onAuth, onLogout, dark, onTheme }: Props) {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const account = session.account;
  const isTutor = account?.role === 'TUTOR';
  const links: { label: string; page: Page; anchor?: string }[] = session.status === 'signedIn'
    ? isTutor
      ? [{ label: 'Dashboard', page: 'lessons' }, { label: 'Tutors', page: 'home', anchor: 'tutors' }]
      : [{ label: 'Find tutors', page: 'home', anchor: 'tutors' }, { label: 'My lessons', page: 'lessons' }]
    : [{ label: 'Find tutors', page: 'home', anchor: 'tutors' }, { label: 'How it works', page: 'home', anchor: 'how' }];

  return (
    <header className={'header' + (scrolled ? ' scrolled' : '')}>
      <div className="container header-inner">
        <button type="button" className="logo-btn" onClick={() => onNavigate('home')} aria-label="Home">
          <Logo />
        </button>
        <nav className="nav" aria-label="Main">
          {links.map((link) => {
            const active = link.page === page && (link.page !== 'home' || page === 'home');
            return (
              <button key={link.label} type="button" className={'nav-link' + (active && link.page !== 'home' ? ' active' : '')}
                      onClick={() => onNavigate(link.page, link.anchor)}>
                {link.label}
              </button>
            );
          })}
          {session.status !== 'signedIn' && (
            <button type="button" className="nav-link" onClick={() => onAuth('signup', 'TUTOR')}>Become a tutor</button>
          )}
        </nav>
        <div className="header-actions">
          {session.status === 'signedIn' && account ? (
            <AccountMenu name={account.name} email={account.email} role={account.role} dark={dark}
                         onTheme={onTheme} onProfile={() => onNavigate('profile')} onLogout={onLogout} />
          ) : session.status === 'signedOut' ? (
            <>
              <button type="button" className="icon-btn" onClick={onTheme} aria-label="Switch theme">
                {dark ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <button type="button" className="btn ghost" onClick={() => onAuth('login')}>Log in</button>
              <button type="button" className="btn primary" onClick={() => onAuth('signup')}>Sign up</button>
            </>
          ) : null}
        </div>
      </div>
    </header>
  );
}

function AccountMenu({ name, email, role, dark, onTheme, onProfile, onLogout }:
                       { name: string; email: string; role: string; dark: boolean;
                         onTheme: () => void; onProfile: () => void; onLogout: () => void }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const close = (e: MouseEvent | KeyboardEvent) => {
      if (e instanceof KeyboardEvent ? e.key === 'Escape' : !ref.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', close);
    return () => { document.removeEventListener('mousedown', close); document.removeEventListener('keydown', close); };
  }, [open]);

  const pick = (action: () => void) => () => { setOpen(false); action(); };
  return (
    <div className="menu" ref={ref}>
      <button type="button" className="menu-trigger" aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen(!open)}>
        <Avatar name={name} size={34} />
        <span className="menu-name">{name.split(' ')[0]}</span>
        <ChevronDown size={16} className={'chev' + (open ? ' up' : '')} aria-hidden="true" />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div className="menu-pop" role="menu"
                      initial={{ opacity: 0, y: -6, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -6, scale: 0.98 }} transition={{ duration: 0.16 }}>
            <div className="menu-head">
              <Avatar name={name} size={40} />
              <div>
                <b>{name}</b>
                <small>{email}</small>
                <span className="role-tag">{role === 'TUTOR' ? 'Tutor' : 'Student'}</span>
              </div>
            </div>
            <button type="button" role="menuitem" onClick={pick(onProfile)}><UserRound size={17} /> Profile</button>
            <button type="button" role="menuitem" onClick={pick(onTheme)}>
              {dark ? <Sun size={17} /> : <Moon size={17} />} {dark ? 'Light mode' : 'Dark mode'}
            </button>
            <hr />
            <button type="button" role="menuitem" className="danger-text" onClick={pick(onLogout)}><LogOut size={17} /> Log out</button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
