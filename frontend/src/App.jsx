import { Navigate, Route, Routes } from 'react-router-dom';
import { getSession, landingPage } from './api';
import { entities } from './entities';
import Layout from './components/Layout';
import CrudPage from './components/CrudPage';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import StudentHome from './pages/student/StudentHome';
import CoursePage from './pages/student/CoursePage';
import StudentVideos from './pages/student/StudentVideos';
import StudentMaterials from './pages/student/StudentMaterials';
import StudentAssignments from './pages/student/StudentAssignments';
import StudentGrades from './pages/student/StudentGrades';
import StudentAccount from './pages/student/StudentAccount';
import FirstLogin from './pages/student/FirstLogin';

const adminLinks = [
  { to: '/', label: 'Dashboard', end: true },
  ...entities.map((e) => ({ to: `/${e.key}`, label: e.title })),
];

// Videos, assignments and grades are inside each course (see CoursePage)
const studentLinks = [
  { to: '/student/courses', label: 'My courses' },
  { to: '/student/account', label: 'My account' },
];

/**
 * Only lets the given role in; anyone else is sent to the login page or their own home page.
 * A student still on a one-time password is kept on the change-password page.
 */
function RequireRole({ role, passwordChange = false, children }) {
  const session = getSession();
  if (!session) return <Navigate to="/login" replace />;
  if (session.role !== role) return <Navigate to={landingPage(session)} replace />;
  if (!!session.mustChangePassword !== passwordChange) return <Navigate to={landingPage(session)} replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/change-password"
        element={
          <RequireRole role="STUDENT" passwordChange>
            <FirstLogin />
          </RequireRole>
        }
      />

      {/* Admin pages */}
      <Route
        element={
          <RequireRole role="ADMIN">
            <Layout title="SMS Admin" links={adminLinks} />
          </RequireRole>
        }
      >
        <Route path="/" element={<Dashboard />} />
        {entities.map((e) => (
          <Route key={e.key} path={`/${e.key}`} element={<CrudPage key={e.key} entity={e} />} />
        ))}
      </Route>

      {/* Student pages */}
      <Route
        path="/student"
        element={
          <RequireRole role="STUDENT">
            <Layout title="Student Portal" links={studentLinks} />
          </RequireRole>
        }
      >
        <Route index element={<Navigate to="courses" replace />} />
        <Route path="courses" element={<StudentHome />} />
        <Route path="courses/:courseId" element={<CoursePage />}>
          <Route index element={<Navigate to="videos" replace />} />
          <Route path="videos" element={<StudentVideos />} />
          <Route path="materials" element={<StudentMaterials />} />
          <Route path="assignments" element={<StudentAssignments />} />
          <Route path="grades" element={<StudentGrades />} />
        </Route>
        <Route path="account" element={<StudentAccount />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
