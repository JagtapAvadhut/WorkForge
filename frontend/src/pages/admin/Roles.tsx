import { PageHeader } from '@/components/common/PageHeader';
import { Card, CardBody } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Check, ShieldCheck } from 'lucide-react';
import type { Role } from '@/types';

interface RoleDef {
  role: Role;
  label: string;
  description: string;
  permissions: string[];
}

const ROLES: RoleDef[] = [
  {
    role: 'ADMIN',
    label: 'Administrator',
    description: 'Full control over the workspace, users, and system settings.',
    permissions: ['Manage users & roles', 'Manage all projects', 'Configure system settings', 'Delete any content'],
  },
  {
    role: 'PROJECT_LEAD',
    label: 'Project Lead',
    description: 'Manages projects they lead, including sprints and members.',
    permissions: ['Create & edit projects', 'Manage sprints', 'Manage project members', 'Edit all project issues'],
  },
  {
    role: 'MEMBER',
    label: 'Member',
    description: 'Contributes to projects by creating and working on issues.',
    permissions: ['Create issues', 'Edit assigned issues', 'Comment on issues', 'Transition issues'],
  },
  {
    role: 'VIEWER',
    label: 'Viewer',
    description: 'Read-only access to projects and issues.',
    permissions: ['View projects', 'View issues', 'View boards & backlogs'],
  },
];

export default function AdminRoles() {
  return (
    <div>
      <PageHeader
        title="Roles & permissions"
        description="Understand what each role can do across WorkForge."
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {ROLES.map((r) => (
          <Card key={r.role}>
            <CardBody>
              <div className="mb-2 flex items-center gap-2">
                <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent/10 text-accent">
                  <ShieldCheck size={18} />
                </span>
                <div>
                  <h3 className="font-semibold text-content">{r.label}</h3>
                  <Badge tone="neutral">{r.role}</Badge>
                </div>
              </div>
              <p className="mb-3 text-sm text-content-muted">{r.description}</p>
              <ul className="space-y-1.5">
                {r.permissions.map((p) => (
                  <li key={p} className="flex items-center gap-2 text-sm text-content">
                    <Check size={15} className="shrink-0 text-success" />
                    {p}
                  </li>
                ))}
              </ul>
            </CardBody>
          </Card>
        ))}
      </div>
    </div>
  );
}
