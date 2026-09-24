import { PageHeader } from '@/components/common/PageHeader';
import { Card, CardBody, CardHeader, CardTitle } from '@/components/ui/Card';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';

export default function AdminSettings() {
  const toast = useToast();

  return (
    <div className="max-w-3xl space-y-6">
      <PageHeader title="System settings" description="Configure workspace-wide preferences." />

      <Card>
        <CardHeader>
          <CardTitle>Workspace</CardTitle>
        </CardHeader>
        <CardBody className="space-y-4">
          <Input label="Workspace name" defaultValue="WorkForge" />
          <Input label="Support email" type="email" defaultValue="support@workforge.local" />
          <div className="flex justify-end">
            <Button onClick={() => toast.success('Settings saved', 'Workspace preferences updated.')}>
              Save changes
            </Button>
          </div>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Appearance</CardTitle>
        </CardHeader>
        <CardBody className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-content">Default theme</p>
            <p className="text-sm text-content-muted">Choose how WorkForge looks for you.</p>
          </div>
          <ThemeToggle />
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Notifications</CardTitle>
        </CardHeader>
        <CardBody className="space-y-3">
          {[
            'Email me when I am assigned an issue',
            'Email me when I am mentioned',
            'Send a daily digest of my open work',
          ].map((label, i) => (
            <label key={label} className="flex items-center gap-3 text-sm text-content">
              <input
                type="checkbox"
                defaultChecked={i < 2}
                className="h-4 w-4 rounded border-border-strong text-accent focus:ring-accent"
              />
              {label}
            </label>
          ))}
        </CardBody>
      </Card>
    </div>
  );
}
