import { useState } from 'react';
import { useApi } from '../../useApi';
import ChangePasswordForm from '../../components/ChangePasswordForm';

export default function StudentAccount() {
  const { data: me, error } = useApi('/student/me');
  const [message, setMessage] = useState('');

  return (
    <div>
      <h2>My account</h2>
      {error && <p className="error">{error}</p>}

      {me && (
        <div className="tile profile">
          <div><span>Name</span>{me.firstName} {me.lastName}</div>
          <div><span>Username</span><code>{me.username}</code></div>
          <div><span>Email</span>{me.email}</div>
          <div><span>Phone</span>{me.phone || '—'}</div>
        </div>
      )}

      <h3>Change password</h3>
      <div className="tile narrow">
        {message && <p className="success">{message}</p>}
        <ChangePasswordForm onDone={() => setMessage('Your password has been changed.')} />
      </div>
    </div>
  );
}
