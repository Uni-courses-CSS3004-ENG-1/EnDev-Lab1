import { useEffect, useState, type FormEvent, type InputHTMLAttributes, type ReactNode } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import {
  ArrowRight, BookOpenCheck, CalendarCheck2, CheckCircle2, DollarSign, Eye, EyeOff, GraduationCap, Lock, Mail,
  ShieldCheck, UserRound,
} from 'lucide-react';
import { login, register } from '../lib/api';
import { friendly } from '../lib/feedback';
import type { Role } from '../lib/types';
import { Logo, Modal, Spinner } from './ui';

export type AuthIntent = { mode: 'login' | 'signup'; role?: Role; reason?: string };

type Props = { intent: AuthIntent | null; subjects: string[]; onClose: () => void; onSignedIn: () => void };

const EMPTY = { name: '', email: '', password: '', subject: '', hourlyRate: '', bio: '' };
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function strength(password: string) {
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/\d/.test(password) || /[^a-zA-Z0-9]/.test(password)) score++;
  return score;
}

// an input with an icon on the left; `end` is an optional button on the right
function Input({ icon, label, end, ...props }:
                 { icon: ReactNode; label: string; end?: ReactNode } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="ifield">
      <span className="ifield-label">{label}</span>
      <span className="ifield-box">
        <span className="ifield-icon" aria-hidden="true">{icon}</span>
        <input {...props} />
        {end}
      </span>
    </label>
  );
}

export function AuthModal({ intent, subjects, onClose, onSignedIn }: Props) {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [role, setRole] = useState<Role>('STUDENT');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [form, setForm] = useState(EMPTY);
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!intent) return;
    setMode(intent.mode);
    setRole(intent.role ?? 'STUDENT');
    setError(null);
    setShow(false);
  }, [intent]);

  const set = (field: keyof typeof EMPTY) => (e: { target: { value: string } }) => setForm({ ...form, [field]: e.target.value });
  const score = strength(form.password);
  const eye = (
    <button type="button" className="ifield-end" onClick={() => setShow(!show)} aria-label={show ? 'Hide password' : 'Show password'}>
      {show ? <EyeOff size={18} /> : <Eye size={18} />}
    </button>
  );

  async function submitLogin(event: FormEvent) {
    event.preventDefault();
    if (!EMAIL.test(email.trim())) { setError('Please enter the email you signed up with.'); return; }
    if (!password) { setError('Please enter your password.'); return; }
    setBusy(true);
    setError(null);
    const res = await login(email.trim(), password);
    setBusy(false);
    setPassword('');
    if (res.status === 200) { onSignedIn(); return; }
    setError(res.status === 401 ? 'That email and password don’t match. Please try again.' : friendly(res));
  }

  async function submitSignup(event: FormEvent) {
    event.preventDefault();
    if (!form.name.trim()) { setError('Please enter your full name.'); return; }
    if (!EMAIL.test(form.email.trim())) { setError('Please enter a valid email address.'); return; }
    if (form.password.length < 8) { setError('Your password needs at least 8 characters.'); return; }
    setBusy(true);
    setError(null);
    const body: Record<string, unknown> = { password: form.password, role, name: form.name.trim(), email: form.email.trim() };
    if (role === 'TUTOR') {
      body.subject = form.subject.trim();
      body.hourlyRate = form.hourlyRate === '' ? null : Number(form.hourlyRate);
      body.bio = form.bio.trim();
    }
    const res = await register(body);
    setBusy(false);
    if (res.status === 201) { setForm(EMPTY); onSignedIn(); return; }
    setError(friendly(res));
  }

  const switchTo = (next: 'login' | 'signup') => { setMode(next); setError(null); setShow(false); };
  const tutorSide = mode === 'signup' && role === 'TUTOR';

  return (
    <Modal open={!!intent} onClose={onClose} labelledBy="auth-title" size="lg">
      <div className="auth">
        {/* left: why join */}
        <aside className={'auth-side' + (tutorSide ? ' tutor' : '')} aria-hidden="true">
          <Logo />
          <div>
            <h3>{tutorSide ? 'Teach what you love, on your terms.' : 'Your next lesson is a few clicks away.'}</h3>
            <ul>
              {(tutorSide
                ? [[DollarSign, 'Set your own hourly price'], [CalendarCheck2, 'Accept only the lessons you want'], [ShieldCheck, 'Paid after every lesson']]
                : [[GraduationCap, 'Hand-picked tutors in every subject'], [CalendarCheck2, 'Book any hour that suits you'], [ShieldCheck, 'Pay only once a tutor confirms']]
              ).map(([Icon, text]) => {
                const I = Icon as typeof DollarSign;
                return <li key={text as string}><span><I size={16} /></span>{text as string}</li>;
              })}
            </ul>
          </div>
          <div className="auth-quote">
            <CheckCircle2 size={18} />
            <span>No charge until your tutor confirms. Cancel before the lesson any time.</span>
          </div>
        </aside>

        {/* right: the form */}
        <div className="auth-main">
          <div className="auth-tabs" role="tablist" aria-label="Log in or sign up">
            {(['login', 'signup'] as const).map((m) => (
              <button key={m} type="button" role="tab" aria-selected={mode === m} className={mode === m ? 'on' : ''} onClick={() => switchTo(m)}>
                {mode === m && <motion.span layoutId="auth-tab" className="auth-tab-ink" transition={{ type: 'spring', stiffness: 500, damping: 38 }} />}
                <span>{m === 'login' ? 'Log in' : 'Sign up'}</span>
              </button>
            ))}
          </div>

          <h2 className="h3 mt-24" id="auth-title">
            {mode === 'login' ? 'Welcome back' : role === 'TUTOR' ? 'Create your tutor account' : 'Create your account'}
          </h2>
          <p className="muted mt-4">{intent?.reason ?? (mode === 'login'
            ? 'Log in with your email and password.'
            : 'It’s free and takes less than a minute.')}</p>

          <AnimatePresence>
            {error && (
              <motion.p className="form-error mt-16" role="alert" initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                {error}
              </motion.p>
            )}
          </AnimatePresence>

          <AnimatePresence mode="wait" initial={false}>
            {mode === 'login' ? (
              <motion.form key="login" className="form mt-20" onSubmit={submitLogin} noValidate
                           initial={{ opacity: 0, x: -12 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 12 }} transition={{ duration: 0.2 }}>
                <Input icon={<Mail size={18} />} label="Email" type="email" value={email} autoFocus
                       onChange={(e) => setEmail(e.target.value)} autoComplete="email" placeholder="you@example.com" />
                <Input icon={<Lock size={18} />} label="Password" type={show ? 'text' : 'password'} value={password}
                       onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" placeholder="Your password" end={eye} />
                <button type="submit" className="btn primary lg block mt-8" disabled={busy}>
                  {busy ? <Spinner light /> : <>Log in <ArrowRight size={18} aria-hidden="true" /></>}
                </button>
                <p className="center muted small">Don’t have an account? <button type="button" className="link" onClick={() => switchTo('signup')}>Sign up</button></p>
              </motion.form>
            ) : (
              <motion.form key="signup" className="form mt-20" onSubmit={submitSignup} noValidate
                           initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -12 }} transition={{ duration: 0.2 }}>
                <div className="role-cards" role="radiogroup" aria-label="I want to">
                  {([['STUDENT', 'Learn', 'Find a tutor', GraduationCap], ['TUTOR', 'Teach', 'Become a tutor', BookOpenCheck]] as const)
                    .map(([value, title, hint, Icon]) => (
                      <button key={value} type="button" role="radio" aria-checked={role === value}
                              className={'role-card' + (role === value ? ' on' : '')} onClick={() => setRole(value)}>
                        <span className="role-card-icon"><Icon size={20} aria-hidden="true" /></span>
                        <span><b>I want to {title.toLowerCase()}</b><small>{hint}</small></span>
                        <span className="role-card-check" aria-hidden="true" />
                      </button>
                    ))}
                </div>
                <Input icon={<UserRound size={18} />} label="Full name" value={form.name} onChange={set('name')}
                       autoComplete="name" maxLength={120} placeholder="Your name" />
                <Input icon={<Mail size={18} />} label="Email" type="email" value={form.email} onChange={set('email')}
                       autoComplete="email" maxLength={160} placeholder="you@example.com" />
                <div>
                  <Input icon={<Lock size={18} />} label="Password" type={show ? 'text' : 'password'} value={form.password}
                         onChange={set('password')} autoComplete="new-password" maxLength={72} placeholder="At least 8 characters" end={eye} />
                  {form.password && (
                    <div className="meter" data-score={score}>
                      {[0, 1, 2, 3].map((i) => <i key={i} className={i < score ? 'on' : ''} />)}
                      <small>{['Too short', 'Weak', 'Fair', 'Good', 'Strong'][score]}</small>
                    </div>
                  )}
                </div>
                <AnimatePresence initial={false}>
                  {role === 'TUTOR' && (
                    <motion.div className="tutor-extra" initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }}
                                exit={{ opacity: 0, height: 0 }} transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}>
                      <div className="grid-2">
                        <Input icon={<GraduationCap size={18} />} label="Subject you teach" value={form.subject} onChange={set('subject')}
                               maxLength={120} list="subjects" placeholder="e.g. Biology" />
                        <Input icon={<DollarSign size={18} />} label="Price per hour" type="number" min="1" step="0.5" inputMode="decimal"
                               value={form.hourlyRate} onChange={set('hourlyRate')} placeholder="25" />
                      </div>
                      <datalist id="subjects">{subjects.map((s) => <option key={s} value={s} />)}</datalist>
                      <label className="ifield">
                        <span className="ifield-label">About you <span className="muted">{form.bio.length}/300</span></span>
                        <textarea rows={3} maxLength={300} value={form.bio} onChange={set('bio')}
                                  placeholder="Your experience and how your lessons work" />
                      </label>
                    </motion.div>
                  )}
                </AnimatePresence>
                <button type="submit" className="btn accent lg block mt-8" disabled={busy}>
                  {busy ? <Spinner light /> : <>Create account <ArrowRight size={18} aria-hidden="true" /></>}
                </button>
                <p className="center muted small">Already have an account? <button type="button" className="link" onClick={() => switchTo('login')}>Log in</button></p>
              </motion.form>
            )}
          </AnimatePresence>
        </div>
      </div>
    </Modal>
  );
}
