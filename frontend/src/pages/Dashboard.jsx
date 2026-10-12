import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { entities } from '../entities';

export default function Dashboard() {
  const [data, setData] = useState({});
  const [submissions, setSubmissions] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([...entities.map((e) => api(`/${e.key}`)), api('/submissions')])
      .then((lists) => {
        setData(Object.fromEntries(entities.map((e, i) => [e.key, lists[i]])));
        setSubmissions(lists[entities.length]);
      })
      .catch((err) => setError(err.message));
  }, []);

  const ungraded = submissions.filter((s) => s.grade == null);

  return (
    <div>
      <h2>Dashboard</h2>
      {error && <p className="error">{error}</p>}

      <div className="cards">
        {entities.map((e) => (
          <Link key={e.key} to={`/${e.key}`} className="card">
            <span className="card-number">{data[e.key]?.length ?? '…'}</span>
            <span className="card-label">{e.title}</span>
          </Link>
        ))}
      </div>

      <h3>Waiting for a grade ({ungraded.length})</h3>
      {ungraded.length === 0 ? (
        <p className="muted">All submissions are graded.</p>
      ) : (
        <ul className="list">
          {ungraded.map((s) => (
            <li key={s.id}>
              <strong>{s.student.fullName}</strong> — {s.assignment.title}{' '}
              <Link to={`/assignments?open=${s.assignment.id}`}>Grade →</Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
