import { Link, NavLink, Outlet, useParams } from 'react-router-dom';
import { useApi } from '../../useApi';

/** One course, with tabs for its videos, materials, assignments and grades. */
export default function CoursePage() {
  const { courseId } = useParams();
  const { data: courses, error } = useApi('/student/courses');

  if (error) return <p className="error">{error}</p>;
  if (courses === null) return <p className="muted">Loading…</p>;

  const course = courses.find((c) => c.id === Number(courseId));
  if (!course) {
    return (
      <div>
        <Link to="/student/courses">← My courses</Link>
        <p className="muted">This course doesn't exist or you are not enrolled in it.</p>
      </div>
    );
  }

  return (
    <div>
      <Link to="/student/courses" className="back-link">← My courses</Link>
      <h2>{course.title}</h2>
      {course.description && <p className="muted">{course.description}</p>}

      <nav className="tabs">
        <NavLink to="videos">Videos</NavLink>
        <NavLink to="materials">Materials</NavLink>
        <NavLink to="assignments">Assignments</NavLink>
        <NavLink to="grades">Grades</NavLink>
      </nav>

      <Outlet />
    </div>
  );
}
