import { useNavigate } from 'react-router-dom';
import { getSession, logout } from '../../api';
import ChangePasswordForm from '../../components/ChangePasswordForm';

/** Shown when a student signs in with the one-time password from the admin. */
export default function FirstLogin() {
  const navigate = useNavigate();
  const session = getSession();

  const cancel = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="login-page">
      <div className="login-box">
        <h1>Welcome, {session.name.split(' ')[0]}</h1>
        <p className="muted">
          You signed in with a one-time password. Please choose your own password to continue.
        </p>
        <ChangePasswordForm
          currentLabel="One-time password"
          submitLabel="Save and continue"
          onDone={() => navigate('/student', { replace: true })}
        />
        <button className="btn btn-light btn-block" onClick={cancel}>Log out</button>
      </div>
    </div>
  );
}
