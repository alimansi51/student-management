import { Link } from 'react-router-dom';
import { getSession } from '../../api';
import { useApi } from '../../useApi';

const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;

/** "My courses": one clickable card per enrolled course. */
export default function StudentHome() {
  const courses = useApi('/student/courses');
  const videos = useApi('/student/videos');
  const assignments = useApi('/student/assignments');

  const firstName = getSession()?.name.split(' ')[0];
  const forCourse = (list, id) => (list.data ?? []).filter((x) => x.courseId === id);

  return (
    <div>
      <h2>Welcome, {firstName}</h2>
      {courses.error && <p className="error">{courses.error}</p>}

      <h3>My courses</h3>
      {courses.data === null ? (
        <p className="muted">Loading…</p>
      ) : courses.data.length === 0 ? (
        <p className="muted">You are not enrolled in any course yet.</p>
      ) : (
        <div className="tiles">
          {courses.data.map((c) => {
            const courseAssignments = forCourse(assignments, c.id);
            const toSubmit = courseAssignments.filter((a) => !a.submissionId).length;
            return (
              <Link key={c.id} to={`/student/courses/${c.id}`} className="tile course-card">
                <div className="tile-head">
                  <h4>{c.title}</h4>
                  {toSubmit > 0 && <span className="badge badge-red">{toSubmit} to submit</span>}
                </div>
                <p className="muted">{c.description || 'No description'}</p>
                <p className="small course-stats">
                  {plural(forCourse(videos, c.id).length, 'video')} · {plural(courseAssignments.length, 'assignment')}
                </p>
                <span className="course-open">Open course →</span>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
