import { useEffect, useRef, useState, type ReactNode } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { CheckCircle2, Info, X, XCircle } from 'lucide-react';
import { gradient, initials, statusLabel } from '../lib/format';
import { dismiss, useToasts } from '../lib/feedback';
import type { Role, Status } from '../lib/types';

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <span className="logo">
      <svg viewBox="0 0 32 32" aria-hidden="true" className="logo-mark">
        <rect width="32" height="32" rx="9" className="logo-bg" />
        <path d="M9 11.5h14M16 11.5V23" className="logo-t" />
        <circle cx="23" cy="21.5" r="3" className="logo-dot" />
      </svg>
      {!compact && <span className="logo-word">Tutoring <span>marketplace</span></span>}
    </span>
  );
}

export function Avatar({ name, size = 44, square = false }: { name: string; size?: number; square?: boolean }) {
  return (
    <span className={'avatar' + (square ? ' square' : '')} aria-hidden="true"
          style={{ width: size, height: size, background: gradient(name), fontSize: Math.round(size * 0.38) }}>
      {initials(name)}
    </span>
  );
}

export function StatusPill({ status, role }: { status: Status; role: Role }) {
  return <span className={'pill status-' + status.toLowerCase()}><i />{statusLabel(status, role)}</span>;
}

export function Spinner({ light = false }: { light?: boolean }) {
  return <span className={'spinner' + (light ? ' light' : '')} aria-label="Loading" />;
}

// A native <dialog> opened with showModal(): focus stays inside, Esc closes it, the page behind is inert.
export function Modal({ open, onClose, labelledBy, children, size = 'md', side = false }:
                        { open: boolean; onClose: () => void; labelledBy: string; children: ReactNode;
                          size?: 'sm' | 'md' | 'lg'; side?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog ref={ref} className={`modal ${size}${side ? ' side' : ''}`} aria-labelledby={labelledBy}
            onCancel={(e) => { e.preventDefault(); onClose(); }}
            onClick={(e) => { if (e.target === ref.current) onClose(); }}>
      {open && (
        <div className="modal-body">
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close"><X size={18} /></button>
          {children}
        </div>
      )}
    </dialog>
  );
}

export function ConfirmDialog({ open, title, text, confirmLabel, danger = true, onConfirm, onClose }:
                                { open: boolean; title: string; text: string; confirmLabel: string; danger?: boolean;
                                  onConfirm: () => Promise<void> | void; onClose: () => void }) {
  const [busy, setBusy] = useState(false);
  return (
    <Modal open={open} onClose={onClose} labelledBy="confirm-title" size="sm">
      <h2 className="h3" id="confirm-title">{title}</h2>
      <p className="muted mt-8">{text}</p>
      <div className="row end gap-8 mt-24">
        <button type="button" className="btn secondary" onClick={onClose}>Keep it</button>
        <button type="button" className={'btn ' + (danger ? 'danger' : 'primary')} disabled={busy}
                onClick={async () => { setBusy(true); await onConfirm(); setBusy(false); }}>
          {busy ? <Spinner light /> : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}

const TOAST_ICON = { success: CheckCircle2, error: XCircle, info: Info };

export function Toaster() {
  const toasts = useToasts();
  return (
    <div className="toaster" aria-live="polite">
      <AnimatePresence initial={false}>
        {toasts.map((t) => {
          const Icon = TOAST_ICON[t.tone];
          return (
            <motion.div key={t.id} layout className={'toast ' + t.tone} role="status"
                        initial={{ opacity: 0, y: 16, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 8, scale: 0.97, transition: { duration: 0.15 } }}
                        transition={{ type: 'spring', stiffness: 500, damping: 34 }}>
              <Icon size={18} aria-hidden="true" />
              <span>{t.message}</span>
              <button type="button" onClick={() => dismiss(t.id)} aria-label="Dismiss"><X size={15} /></button>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}

// fades and lifts content in when it scrolls into view
export function Reveal({ children, delay = 0, className }: { children: ReactNode; delay?: number; className?: string }) {
  return (
    <motion.div className={className} initial={{ opacity: 0, y: 24 }} whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: '-40px' }}
                transition={{ duration: 0.6, delay, ease: [0.16, 1, 0.3, 1] }}>
      {children}
    </motion.div>
  );
}
