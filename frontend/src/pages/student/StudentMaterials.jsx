import { useParams } from 'react-router-dom';
import { useApi } from '../../useApi';

const formatSize = (bytes) =>
  bytes == null ? '' : bytes > 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(bytes / 1024)} KB`;

/** The Materials tab of a course: lectures, slides and other files from the teacher. */
export default function StudentMaterials() {
  const courseId = Number(useParams().courseId);
  const { data, error } = useApi('/student/materials');
  const materials = data?.filter((m) => m.courseId === courseId);

  return (
    <div>
      {error && <p className="error">{error}</p>}
      {materials === undefined ? (
        <p className="muted">Loading…</p>
      ) : materials.length === 0 ? (
        <p className="muted">No lectures or materials in this course yet.</p>
      ) : (
        <div className="stack">
          {materials.map((m) => (
            <div key={m.id} className="tile">
              <h4>{m.title}</h4>
              {m.description && <p>{m.description}</p>}
              {m.fileUrl && (
                // the server sends materials as downloads, so clicking saves the file straight away
                <a className="file-link" href={m.fileUrl} download={m.originalFileName}>
                  📄 {m.originalFileName} <span>{formatSize(m.fileSize)} · Download ↓</span>
                </a>
              )}
              {m.uploadedAt && (
                <p className="muted small">Added {m.uploadedAt.replace('T', ' ').slice(0, 16)}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
