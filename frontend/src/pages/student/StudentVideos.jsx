import { useParams } from 'react-router-dom';
import { useApi } from '../../useApi';

/** The Videos tab of a course. */
export default function StudentVideos() {
  const courseId = Number(useParams().courseId);
  const { data, error } = useApi('/student/videos');
  const videos = data?.filter((v) => v.courseId === courseId);

  return (
    <div>
      {error && <p className="error">{error}</p>}
      {videos === undefined ? (
        <p className="muted">Loading…</p>
      ) : videos.length === 0 ? (
        <p className="muted">No videos in this course yet.</p>
      ) : (
        <div className="tiles videos">
          {videos.map((v) => (
            <div key={v.id} className="tile">
              {v.streamUrl ? (
                <video className="player" src={v.streamUrl} controls preload="metadata" />
              ) : (
                <div className="player player-missing">Video not available</div>
              )}
              <h4>{v.title}</h4>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
