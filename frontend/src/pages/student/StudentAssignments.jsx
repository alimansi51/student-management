import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { upload } from '../../api';
import { useApi } from '../../useApi';

function status(a) {
  if (a.grade != null) return <span className="badge badge-green">Graded: {a.grade}</span>;
  if (a.submissionId) return <span className="badge badge-blue">Submitted</span>;
  if (a.dueDate && a.dueDate < new Date().toISOString().slice(0, 10)) {
    return <span className="badge badge-red">Overdue</span>;
  }
  return <span className="badge">Not submitted</span>;
}

const isPdf = (file) => file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');

/** The Assignments tab of a course: see the assignments and hand in a PDF for each. */
export default function StudentAssignments() {
  const courseId = Number(useParams().courseId);
  const { data, error, reload } = useApi('/student/assignments');
  const assignments = data?.filter((a) => a.courseId === courseId);
  const [open, setOpen] = useState(null); // the assignment being submitted
  const [file, setFile] = useState(null);
  const [progress, setProgress] = useState(null); // upload %, null when not uploading
  const [submitError, setSubmitError] = useState('');

  const start = (a) => {
    setOpen(a);
    setFile(null);
    setSubmitError('');
  };

  const close = () => {
    if (progress === null) setOpen(null);
  };

  const chooseFile = (e) => {
    const chosen = e.target.files[0] ?? null;
    setSubmitError(chosen && !isPdf(chosen) ? 'Please choose a PDF file' : '');
    setFile(chosen && isPdf(chosen) ? chosen : null);
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!file) {
      setSubmitError('Please choose a PDF file');
      return;
    }
    const formData = new FormData();
    formData.append('assignmentId', open.id);
    formData.append('file', file);
    try {
      setProgress(0);
      await upload('/student/submissions', { formData, onProgress: setProgress });
      setOpen(null);
      reload();
    } catch (err) {
      setSubmitError(err.message);
    } finally {
      setProgress(null);
    }
  };

  return (
    <div>
      {error && <p className="error">{error}</p>}

      {assignments === undefined ? (
        <p className="muted">Loading…</p>
      ) : assignments.length === 0 ? (
        <p className="muted">No assignments in this course yet.</p>
      ) : (
        <div className="stack">
          {assignments.map((a) => (
            <div key={a.id} className="tile">
              <div className="tile-head">
                <h4>{a.title}</h4>
                {status(a)}
              </div>
              {a.description && <p>{a.description}</p>}
              <p className="muted small">
                Due: {a.dueDate ?? 'no due date'}
                {a.submittedAt && <> · Submitted {a.submittedAt.replace('T', ' ').slice(0, 16)}</>}
              </p>
              {a.fileUrl && (
                <a className="file-link" href={a.fileUrl} target="_blank" rel="noreferrer">
                  📄 {a.originalFileName} <span>Open ↗</span>
                </a>
              )}
              {a.grade == null && (
                <button className="btn btn-small" onClick={() => start(a)}>
                  {a.submissionId ? 'Replace my PDF' : 'Submit PDF'}
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {open && (
        <div className="modal-backdrop" onClick={close}>
          <form className="modal" onSubmit={submit} onClick={(e) => e.stopPropagation()}>
            <h3>{open.title}</h3>
            {open.description && <p className="muted">{open.description}</p>}
            {submitError && <p className="error">{submitError}</p>}
            <label className="field">
              <span>Your answer (PDF file)</span>
              <input type="file" accept="application/pdf,.pdf" onChange={chooseFile} />
              {file && <small className="muted">{file.name} ({(file.size / 1024 / 1024).toFixed(1)} MB)</small>}
              {open.submissionId && <small className="muted">This replaces the PDF you already handed in.</small>}
            </label>

            {progress !== null && (
              <div className="progress">
                <div className="progress-bar" style={{ width: `${progress}%` }} />
                <span>{progress < 100 ? `Uploading… ${progress}%` : 'Saving…'}</span>
              </div>
            )}

            <div className="modal-actions">
              <button type="button" className="btn btn-light" onClick={close} disabled={progress !== null}>Cancel</button>
              <button type="submit" className="btn" disabled={progress !== null}>
                {progress !== null ? 'Uploading…' : 'Submit'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
