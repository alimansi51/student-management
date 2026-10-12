import { useState } from 'react';
import { api, getSession, login } from '../api';

/** Change-password form for students. Signs back in with the new password, then calls onDone. */
export default function ChangePasswordForm({ currentLabel = 'Current password', submitLabel = 'Change password', onDone }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');

  const set = (name) => (e) => setForm({ ...form, [name]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.newPassword !== form.confirm) {
      setError('The new passwords do not match');
      return;
    }
    try {
      await api('/student/password', {
        method: 'PUT',
        body: { currentPassword: form.currentPassword, newPassword: form.newPassword },
      });
      // the saved login still holds the old password, so sign in again with the new one
      await login(getSession().username, form.newPassword);
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
      onDone?.();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form onSubmit={submit}>
      {error && <p className="error">{error}</p>}
      <label className="field">
        <span>{currentLabel}</span>
        <input type="password" value={form.currentPassword} onChange={set('currentPassword')} required />
      </label>
      <label className="field">
        <span>New password (at least 6 characters)</span>
        <input type="password" value={form.newPassword} onChange={set('newPassword')} minLength={6} required />
      </label>
      <label className="field">
        <span>Repeat new password</span>
        <input type="password" value={form.confirm} onChange={set('confirm')} required />
      </label>
      <button className="btn btn-block" type="submit">{submitLabel}</button>
    </form>
  );
}
