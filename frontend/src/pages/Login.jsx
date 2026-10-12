import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { getSession, landingPage, login } from '../api';

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const session = getSession();
  if (session) return <Navigate to={landingPage(session)} replace />;

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const me = await login(username, password);
      navigate(landingPage(me));
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="login-page">
      <form className="login-box" onSubmit={submit}>
        <h1>Student Management</h1>
        <p className="muted">Sign in as the admin or as a student</p>
        {error && <p className="error">{error}</p>}
        <label className="field">
          <span>Username</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="e.g. john.smith"
            required
            autoFocus
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn btn-block" type="submit">Sign in</button>
      </form>
    </div>
  );
}
