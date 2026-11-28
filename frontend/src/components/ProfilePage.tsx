import { motion } from 'motion/react';
import { AtSign, GraduationCap, LogOut, Mail, Wallet } from 'lucide-react';
import type { Account } from '../lib/types';
import { money } from '../lib/format';
import { Avatar } from './ui';

export function ProfilePage({ account, onLogout }: { account: Account; onLogout: () => void }) {
  const isTutor = account.role === 'TUTOR';
  return (
    <div className="container page narrow">
      <div className="page-head"><h1 className="h1">Profile</h1></div>
      <motion.section className="profile" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
        <div className="profile-cover" aria-hidden="true" />
        <div className="profile-top">
          <Avatar name={account.name} size={96} />
          <div>
            <h2 className="h3">{account.name}</h2>
            <span className="role-tag">{isTutor ? 'Tutor' : 'Student'}</span>
          </div>
        </div>
        <dl className="details">
          {account.username !== account.email && <div><dt><AtSign size={16} aria-hidden="true" /> Username</dt><dd>{account.username}</dd></div>}
          <div><dt><Mail size={16} aria-hidden="true" /> Email</dt><dd>{account.email}</dd></div>
          {isTutor && <div><dt><GraduationCap size={16} aria-hidden="true" /> Subject</dt><dd>{account.subject}</dd></div>}
          {isTutor && <div><dt><Wallet size={16} aria-hidden="true" /> Price</dt><dd>{money(account.hourlyRate)} per hour</dd></div>}
        </dl>
        {isTutor && (
          <div className="about">
            <h3 className="h5">About you</h3>
            <p className="muted">{account.bio || 'You haven’t written an introduction yet.'}</p>
          </div>
        )}
        <div className="profile-foot">
          <button type="button" className="btn secondary" onClick={onLogout}><LogOut size={17} aria-hidden="true" /> Log out</button>
        </div>
      </motion.section>
    </div>
  );
}
