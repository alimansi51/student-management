import { useEffect, useState } from 'react';
import { api } from '../api';

/**
 * Opened from an assignment row: every student in the course, their submitted PDF (if any)
 * and a box to give or change the grade.
 */
export default function AssignmentSubmissions({ item: assignment, onClose }) {
  const [submissions, setSubmissions] = useState(null);
  const [grades, setGrades] = useState({}); // submission id -> grade typed in the box
  const [saved, setSaved] = useState({}); // submission id -> true right after saving
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const all = await api('/submissions');
      const mine = all.filter((s) => s.assignment.id === assignment.id);
      setSubmissions(mine);
      setGrades(Object.fromEntries(mine.map((s) => [s.id, s.grade ?? ''])));
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const saveGrade = async (s) => {
    setError('');
    const value = grades[s.id];
    try {
      await api(`/submissions/${s.id}/grade`, {
        method: 'PUT',
        body: { grade: value === '' ? null : Number(value) },
      });
      setSaved({ ...saved, [s.id]: true });
      setTimeout(() => setSaved((prev) => ({ ...prev, [s.id]: false })), 2000);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  // everyone enrolled in the course, plus anyone who submitted but has since left the course
  const enrolled = assignment.course?.students ?? [];
  const submittedBy = new Map((submissions ?? []).map((s) => [s.student.id, s]));
  const students = [
    ...enrolled,
    ...(submissions ?? []).map((s) => s.student).filter((st) => !enrolled.some((e) => e.id === st.id)),
  ];
  const gradedCount = (submissions ?? []).filter((s) => s.grade != null).length;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
        <h3>{assignment.title}</h3>
        <p className="muted small">
          {assignment.course?.title} · Due {assignment.dueDate ?? 'no due date'}
          {submissions && <> · {submissions.length} of {enrolled.length} submitted · {gradedCount} graded</>}
        </p>
        {error && <p className="error">{error}</p>}

        {submissions === null ? (
          <p className="muted">Loading…</p>
        ) : students.length === 0 ? (
          <p className="muted">No students are enrolled in this course yet.</p>
        ) : (
          <div className="submission-list">
            {students.map((st) => {
              const s = submittedBy.get(st.id);
              return (
                <div key={st.id} className="submission">
                  <div className="tile-head">
                    <strong>{st.fullName}</strong>
                    {!s ? (
                      <span className="badge">Not submitted</span>
                    ) : s.grade != null ? (
                      <span className="badge badge-green">Graded: {s.grade}</span>
                    ) : (
                      <span className="badge badge-blue">Waiting for a grade</span>
                    )}
                  </div>
                  {s && (
                    <>
                      <p className="muted small">Submitted {s.submittedAt?.replace('T', ' ').slice(0, 16)}</p>
                      {s.fileUrl ? (
                        <a className="file-link" href={s.fileUrl} target="_blank" rel="noreferrer">
                          📄 {s.originalFileName} <span>Open PDF ↗</span>
                        </a>
                      ) : (
                        // submissions made before answers became PDFs
                        <div className="answer">{s.content || <em>(no file)</em>}</div>
                      )}
                      <form className="grade-row" onSubmit={(e) => { e.preventDefault(); saveGrade(s); }}>
                        <label>
                          Grade
                          <input
                            type="number"
                            value={grades[s.id]}
                            onChange={(e) => setGrades({ ...grades, [s.id]: e.target.value })}
                          />
                        </label>
                        <button className="btn btn-small" type="submit">Save grade</button>
                        {saved[s.id] && <span className="saved">✓ Saved</span>}
                      </form>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        )}

        <div className="modal-actions">
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
