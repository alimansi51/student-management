import { useParams } from 'react-router-dom';
import { useApi } from '../../useApi';

/** The Grades tab of a course. */
export default function StudentGrades() {
  const courseId = Number(useParams().courseId);
  const { data, error } = useApi('/student/assignments');
  const assignments = (data ?? []).filter((a) => a.courseId === courseId);

  const submitted = assignments.filter((a) => a.submissionId);
  const graded = submitted.filter((a) => a.grade != null);
  const average = graded.length
    ? (graded.reduce((sum, a) => sum + a.grade, 0) / graded.length).toFixed(1)
    : null;

  return (
    <div>
      {error && <p className="error">{error}</p>}

      <div className="cards">
        <div className="card">
          <span className="card-number">{average ?? '—'}</span>
          <span className="card-label">Average grade</span>
        </div>
        <div className="card">
          <span className="card-number">{graded.length}</span>
          <span className="card-label">Graded</span>
        </div>
        <div className="card">
          <span className="card-number">{submitted.length - graded.length}</span>
          <span className="card-label">Waiting for a grade</span>
        </div>
        <div className="card">
          <span className="card-number">{assignments.length - submitted.length}</span>
          <span className="card-label">Not submitted</span>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Assignment</th>
              <th>Submitted</th>
              <th>Grade</th>
            </tr>
          </thead>
          <tbody>
            {submitted.length === 0 && (
              <tr><td colSpan={3} className="empty">You haven't submitted anything in this course yet.</td></tr>
            )}
            {submitted.map((a) => (
              <tr key={a.id}>
                <td>{a.title}</td>
                <td>{a.submittedAt.replace('T', ' ').slice(0, 16)}</td>
                <td>{a.grade ?? <span className="muted">Not graded yet</span>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
