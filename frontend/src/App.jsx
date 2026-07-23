import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { ProfilePage } from './pages/ProfilePage';
import { CoachDashboardPage, CoachMemberDetailPage } from './pages/CoachDashboardPage';

function HomeRedirect() {
  const { user, isCoach } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={isCoach ? '/coach' : '/profile'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route element={<ProtectedRoute role="MEMBER" />}>
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      <Route element={<ProtectedRoute role="COACH" />}>
        <Route path="/coach" element={<CoachDashboardPage />} />
        <Route path="/coach/members/:userId" element={<CoachMemberDetailPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
