import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { MainLayout } from '@/layouts/MainLayout';
import { ProtectedRoute } from '@/routes/ProtectedRoute';
import { RequireRole } from '@/routes/RequireRole';
import { FullPageSpinner } from '@/components/ui/Spinner';
import { useAuthBootstrap } from '@/hooks/useAuth';
import { useThemeEffect } from '@/hooks/useTheme';

const Login = lazy(() => import('@/pages/Login'));
const Register = lazy(() => import('@/pages/Register'));
const Dashboard = lazy(() => import('@/pages/Dashboard'));
const Projects = lazy(() => import('@/pages/Projects'));
const ProjectLayout = lazy(() => import('@/pages/ProjectLayout'));
const ProjectDetail = lazy(() => import('@/pages/ProjectDetail'));
const Board = lazy(() => import('@/pages/Board'));
const Backlog = lazy(() => import('@/pages/Backlog'));
const Issues = lazy(() => import('@/pages/Issues'));
const ProjectSettings = lazy(() => import('@/pages/ProjectSettings'));
const IssueDetail = lazy(() => import('@/pages/IssueDetail'));
const MyWork = lazy(() => import('@/pages/MyWork'));
const Filters = lazy(() => import('@/pages/Filters'));
const Notifications = lazy(() => import('@/pages/Notifications'));
const ProjectPicker = lazy(() => import('@/pages/ProjectPicker'));
const AdminUsers = lazy(() => import('@/pages/admin/Users'));
const AdminRoles = lazy(() => import('@/pages/admin/Roles'));
const AdminSettings = lazy(() => import('@/pages/admin/Settings'));
const NotFound = lazy(() => import('@/pages/NotFound'));

export default function App() {
  // Restore session from persisted tokens and apply the theme.
  useAuthBootstrap();
  useThemeEffect();

  return (
    <Suspense fallback={<FullPageSpinner label="Loading…" />}>
      <Routes>
        {/* Public auth routes */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        </Route>

        {/* Protected application routes */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />

            <Route path="/projects" element={<Projects />} />
            <Route path="/projects/:projectKey" element={<ProjectLayout />}>
              <Route index element={<ProjectDetail />} />
              <Route path="board" element={<Board />} />
              <Route path="backlog" element={<Backlog />} />
              <Route path="issues" element={<Issues />} />
              <Route path="settings" element={<ProjectSettings />} />
            </Route>

            <Route path="/issues" element={<Issues />} />
            <Route path="/issues/:issueKey" element={<IssueDetail />} />

            <Route path="/my-work" element={<MyWork />} />
            <Route path="/filters" element={<Filters />} />
            <Route path="/notifications" element={<Notifications />} />

            {/* Top-level planning entries require picking a project first */}
            <Route
              path="/board"
              element={
                <ProjectPicker title="Board" description="View a project's Kanban board." destination="board" />
              }
            />
            <Route
              path="/backlog"
              element={
                <ProjectPicker title="Backlog" description="Plan sprints for a project." destination="backlog" />
              }
            />
            <Route
              path="/sprints"
              element={
                <ProjectPicker title="Sprints" description="Manage a project's sprints." destination="backlog" />
              }
            />
            <Route
              path="/reports"
              element={
                <ProjectPicker title="Reports" description="Review a project's progress." destination="" />
              }
            />

            {/* Administration */}
            <Route element={<RequireRole roles={['ADMIN']} />}>
              <Route path="/admin/users" element={<AdminUsers />} />
              <Route path="/admin/roles" element={<AdminRoles />} />
              <Route path="/admin/settings" element={<AdminSettings />} />
            </Route>

            <Route path="*" element={<NotFound />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  );
}
